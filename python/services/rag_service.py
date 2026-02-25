"""
RAG Service – orchestrates the full diagnosis pipeline:

  Image classification  ➜  Embedding search  ➜  Groq LLM  ➜  Translation

Exposes three high-level methods:
  • diagnose_with_image   – web pipeline (text + optional image)
  • diagnose_text_only    – text-only shortcut
  • process_ivrs_query    – IVRS voice pipeline
  • search_rag            – raw similarity search
"""

import logging
from typing import Dict, List, Optional, Tuple

from services.embedding_service import EmbeddingService
from services.groq_service import GroqService
from services.image_classifier import ImageClassifier

logger = logging.getLogger(__name__)


class RAGService:
    def __init__(self) -> None:
        self.embedding_service = EmbeddingService()
        self.groq_service = GroqService()
        self.image_classifier = ImageClassifier()

    # ── web pipeline ─────────────────────────────────────────────────────

    def diagnose_with_image(
        self,
        user_query: str,
        image_bytes: Optional[bytes] = None,
        language: str = "en",
        region: Optional[str] = None,
    ) -> Dict:
        """Full diagnosis: image classification ➜ RAG ➜ LLM ➜ translate."""

        classified_crop: Optional[str] = None
        classified_disease: Optional[str] = None
        confidence: float = 0.0

        # 1. classify image (if provided) ─────────────────────────────────
        if image_bytes:
            classified_crop, classified_disease, confidence = (
                self.image_classifier.classify(image_bytes)
            )
            logger.info(
                "Classification: %s – %s (%.3f)",
                classified_crop,
                classified_disease,
                confidence,
            )

        # 2. build search query ───────────────────────────────────────────
        search_query = user_query
        if classified_crop and classified_disease:
            search_query = f"{classified_crop} {classified_disease} {user_query}"

        # 3. RAG search ───────────────────────────────────────────────────
        rag_results = self.embedding_service.search(search_query)

        # also do a targeted crop+disease search and merge
        if classified_crop and classified_disease:
            specific = self.embedding_service.search_by_crop_and_disease(
                classified_crop, classified_disease
            )
            seen_ids: set = set()
            merged: List[Tuple[Dict, float]] = []
            for doc, score in specific:
                cid = doc.get("chunk_id", "")
                if cid not in seen_ids:
                    seen_ids.add(cid)
                    merged.append((doc, score))
            for doc, score in rag_results:
                cid = doc.get("chunk_id", "")
                if cid not in seen_ids:
                    seen_ids.add(cid)
                    merged.append((doc, score))
            rag_results = merged[:5]

        # 4. determine source ─────────────────────────────────────────────
        rag_docs = [{"document": doc, "score": score} for doc, score in rag_results]
        source = "rag" if rag_results else "llm"

        # 5. generate diagnosis via Groq ──────────────────────────────────
        diagnosis = self.groq_service.generate_diagnosis(
            user_query=user_query,
            classified_crop=classified_crop,
            classified_disease=classified_disease,
            rag_results=rag_docs,
            region=region,
            language=language,
        )

        # 6. extract structured data from top RAG hit ─────────────────────
        symptoms_matched: List[str] = []
        management_advice: Dict = {}
        region_specific = False

        if rag_results:
            top_doc = rag_results[0][0]
            symptoms_matched = top_doc.get("symptoms", [])
            management_advice = top_doc.get("management", {})
            if region:
                doc_regions = [r.lower() for r in top_doc.get("region", [])]
                region_specific = any(region.lower() in r for r in doc_regions)

        # 7. translate if needed ──────────────────────────────────────────
        if language != "en":
            diagnosis = self.groq_service.translate(diagnosis, "en", language)

        return {
            "classified_disease": classified_disease,
            "classified_crop": classified_crop,
            "confidence": confidence,
            "diagnosis": diagnosis,
            "symptoms_matched": symptoms_matched,
            "management_advice": management_advice,
            "region_specific": region_specific,
            "source": source,
            "language": language,
        }

    # ── text-only shortcut ───────────────────────────────────────────────

    def diagnose_text_only(
        self, query: str, language: str = "en", region: Optional[str] = None
    ) -> Dict:
        return self.diagnose_with_image(
            user_query=query, image_bytes=None, language=language, region=region
        )

    # ── IVRS voice pipeline ──────────────────────────────────────────────

    def process_ivrs_query(
        self, speech_text: str, language: str = "en", region: Optional[str] = None
    ) -> Dict:
        """translate ➜ RAG ➜ diagnose ➜ translate back."""

        original_text = speech_text

        # translate to English if needed
        if language != "en":
            translated_query = self.groq_service.translate(
                speech_text, language, "en"
            )
        else:
            translated_query = speech_text

        logger.info("IVRS query (translated): %s", translated_query)

        # diagnose in English
        result = self.diagnose_text_only(
            query=translated_query, language="en", region=region
        )
        english_response = result["diagnosis"]

        # translate response back
        if language != "en":
            translated_response = self.groq_service.translate(
                english_response, "en", language
            )
        else:
            translated_response = english_response

        return {
            "original_text": original_text,
            "translated_query": translated_query,
            "diagnosis": english_response,
            "translated_response": translated_response,
            "language": language,
            "classified_crop": result.get("classified_crop"),
            "classified_disease": result.get("classified_disease"),
            "source": result.get("source", "llm"),
        }

    # ── raw search ───────────────────────────────────────────────────────

    def search_rag(self, query: str, top_k: int = 3) -> List[Dict]:
        results = self.embedding_service.search(query, top_k=top_k)
        return [
            {
                "chunk_id": doc.get("chunk_id", ""),
                "crop": doc.get("crop", ""),
                "disease": doc.get("disease", ""),
                "similarity_score": score,
                "document": doc,
            }
            for doc, score in results
        ]
