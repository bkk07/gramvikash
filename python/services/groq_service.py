"""
Groq LLM Service – generates crop-disease diagnoses and performs
language translation via the Groq API (Llama 3.1).
"""

import logging
from typing import Dict, List, Optional

from groq import Groq

from config import GROQ_API_KEY, GROQ_MODEL

logger = logging.getLogger(__name__)

LANG_NAMES = {"en": "English", "hi": "Hindi", "te": "Telugu"}


class GroqService:
    """Thin wrapper around the Groq chat-completions API."""

    def __init__(self) -> None:
        if GROQ_API_KEY:
            self.client = Groq(api_key=GROQ_API_KEY)
        else:
            self.client = None
            logger.warning("GROQ_API_KEY not set – LLM features will be limited.")

    # ── diagnosis generation ─────────────────────────────────────────────

    def generate_diagnosis(
        self,
        user_query: str,
        classified_crop: Optional[str],
        classified_disease: Optional[str],
        rag_results: List[Dict],
        region: Optional[str] = None,
        language: str = "en",
    ) -> str:
        """Produce a structured diagnosis combining RAG context + LLM."""

        if not self.client:
            return self._fallback_response(
                rag_results, classified_crop, classified_disease
            )

        rag_context = self._format_rag_context(rag_results)

        system_prompt = (
            "You are an expert agricultural advisor for Indian farmers "
            "(GramVikash platform). You help diagnose crop diseases and "
            "provide actionable advice.\n\n"
            "Guidelines:\n"
            "- Be specific and practical in your recommendations\n"
            "- Include both cultural and chemical management options\n"
            "- Mention resistant varieties when available\n"
            "- If region is provided, give region-specific advice\n"
            "- Be empathetic – farmers depend on their crops for livelihood\n"
            "- Structure your response clearly: Diagnosis, Symptoms, "
            "Management, Prevention\n"
            "- Keep the response concise but informative (200-300 words)"
        )

        classification_line = ""
        if classified_crop and classified_disease:
            classification_line = (
                f"\nImage classification result: {classified_crop} – "
                f"{classified_disease}"
            )

        region_line = f"\nRegion: {region}" if region else ""

        user_prompt = (
            f"Farmer's description: {user_query}"
            f"{classification_line}{region_line}\n\n"
            f"RAG Knowledge-Base Results:\n"
            f"{rag_context or 'No matching documents found in knowledge base.'}"
            "\n\nBased on the above information, provide a comprehensive "
            "diagnosis and management advice for the farmer. If RAG results "
            "are available prioritise that information; otherwise provide "
            "general expert advice. Respond in English."
        )

        try:
            resp = self.client.chat.completions.create(
                model=GROQ_MODEL,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt},
                ],
                temperature=0.3,
                max_tokens=1024,
            )
            return resp.choices[0].message.content
        except Exception as exc:
            logger.error("Groq API error: %s", exc)
            return self._fallback_response(
                rag_results, classified_crop, classified_disease
            )

    # ── translation ──────────────────────────────────────────────────────

    def translate(self, text: str, source_lang: str, target_lang: str) -> str:
        """Translate *text* between Hindi / Telugu / English using the LLM."""

        if not self.client or source_lang == target_lang:
            return text

        source = LANG_NAMES.get(source_lang, source_lang)
        target = LANG_NAMES.get(target_lang, target_lang)

        try:
            resp = self.client.chat.completions.create(
                model=GROQ_MODEL,
                messages=[
                    {
                        "role": "system",
                        "content": (
                            f"You are a translator. Translate the following "
                            f"text from {source} to {target}. Only output "
                            f"the translated text – nothing else. Use simple, "
                            f"farmer-friendly language."
                        ),
                    },
                    {"role": "user", "content": text},
                ],
                temperature=0.1,
                max_tokens=2048,
            )
            return resp.choices[0].message.content
        except Exception as exc:
            logger.error("Translation error: %s", exc)
            return text

    # ── helpers ──────────────────────────────────────────────────────────

    def _format_rag_context(self, rag_results: List[Dict]) -> str:
        if not rag_results:
            return ""

        parts: List[str] = []
        for i, result in enumerate(rag_results, 1):
            doc = result.get("document", result)
            block = (
                f"--- Document {i} ---\n"
                f"Crop: {doc.get('crop', 'Unknown')}\n"
                f"Disease: {doc.get('disease', 'Unknown')}\n"
                f"Pathogen: {doc.get('pathogen', 'Unknown')}\n"
                f"Symptoms: {', '.join(doc.get('symptoms', []))}\n"
                f"Season: {doc.get('season', 'Unknown')}\n"
                f"Region: {', '.join(doc.get('region', []))}\n"
            )
            for key, values in doc.get("management", {}).items():
                if isinstance(values, list):
                    block += f"{key.capitalize()}: {'; '.join(values)}\n"
            parts.append(block)

        return "\n".join(parts)

    def _fallback_response(
        self,
        rag_results: List[Dict],
        classified_crop: Optional[str],
        classified_disease: Optional[str],
    ) -> str:
        """Basic response when the LLM is unavailable."""

        if not rag_results:
            img_hint = ""
            if classified_crop and classified_disease:
                img_hint = (
                    f"The image suggests it could be {classified_crop} "
                    f"with {classified_disease}. "
                )
            return (
                "Based on the analysis, your crop may be affected by a disease. "
                f"{img_hint}"
                "Please consult your local agricultural extension officer for "
                "a detailed diagnosis. In the meantime, ensure proper drainage, "
                "avoid excess nitrogen, and remove visibly infected plant parts."
            )

        doc = (
            rag_results[0]
            if isinstance(rag_results[0], dict) and "crop" in rag_results[0]
            else rag_results[0].get("document", {})
        )
        symptoms = ", ".join(doc.get("symptoms", [])[:3])
        mgmt = doc.get("management", {})
        cultural = mgmt.get("cultural", [])[:2]
        chemical = mgmt.get("chemical", [])[:2]

        resp = (
            f"Your {doc.get('crop', 'crop')} is most likely affected by "
            f"{doc.get('disease', 'a disease')}. "
        )
        if symptoms:
            resp += f"Common symptoms include: {symptoms}. "
        if cultural:
            resp += f"Management: {'; '.join(cultural)}. "
        if chemical:
            resp += f"Chemical control: {'; '.join(chemical)}."
        return resp
