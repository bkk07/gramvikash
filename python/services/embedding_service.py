"""
Embedding Service – loads RAG documents, computes embeddings using
sentence-transformers/all-MiniLM-L6-v2, and provides cosine-similarity search.
"""

import json
import logging
from typing import Dict, List, Tuple, Optional

import numpy as np
from sentence_transformers import SentenceTransformer

from config import (
    EMBEDDING_MODEL_NAME,
    RAG_DOCUMENTS_PATH,
    RAG_TOP_K,
    RAG_SIMILARITY_THRESHOLD,
)

logger = logging.getLogger(__name__)


class EmbeddingService:
    """Pre-computes embeddings for the RAG document corpus and supports
    cosine-similarity search at query time."""

    def __init__(self) -> None:
        logger.info("Loading embedding model: %s", EMBEDDING_MODEL_NAME)
        self.model = SentenceTransformer(EMBEDDING_MODEL_NAME)

        self.documents: List[Dict] = []
        self.document_texts: List[str] = []
        self.embeddings: Optional[np.ndarray] = None

        self._load_documents()
        self._compute_embeddings()

    # ── internal helpers ─────────────────────────────────────────────────

    def _load_documents(self) -> None:
        """Load RAG documents from the JSON file."""
        with open(RAG_DOCUMENTS_PATH, "r", encoding="utf-8") as fh:
            self.documents = json.load(fh)

        for doc in self.documents:
            parts: List[str] = [
                f"Crop: {doc.get('crop', '')}",
                f"Disease: {doc.get('disease', '')}",
                f"Pathogen: {doc.get('pathogen', '')}",
                f"Symptoms: {', '.join(doc.get('symptoms', []))}",
            ]

            # management
            for key, values in doc.get("management", {}).items():
                if isinstance(values, list):
                    parts.append(f"{key}: {', '.join(values)}")

            # conditions
            for key, value in doc.get("conditions", {}).items():
                parts.append(f"{key}: {value}")

            # region
            regions = doc.get("region", [])
            if regions:
                parts.append(f"Region: {', '.join(regions)}")

            self.document_texts.append(" | ".join(parts))

        logger.info("Loaded %d RAG documents", len(self.documents))

    def _compute_embeddings(self) -> None:
        """Pre-compute normalised embeddings for every document."""
        if self.document_texts:
            self.embeddings = self.model.encode(
                self.document_texts,
                convert_to_numpy=True,
                normalize_embeddings=True,
            )
            logger.info("Computed embeddings: shape %s", self.embeddings.shape)

    # ── public API ───────────────────────────────────────────────────────

    def search(
        self,
        query: str,
        top_k: Optional[int] = None,
        threshold: Optional[float] = None,
    ) -> List[Tuple[Dict, float]]:
        """Return the *top_k* documents whose cosine similarity to *query*
        exceeds *threshold*."""

        top_k = top_k or RAG_TOP_K
        threshold = threshold if threshold is not None else RAG_SIMILARITY_THRESHOLD

        query_emb = self.model.encode(
            [query], convert_to_numpy=True, normalize_embeddings=True
        )

        # dot product on unit vectors == cosine similarity
        similarities = np.dot(self.embeddings, query_emb.T).flatten()
        top_indices = np.argsort(similarities)[::-1][:top_k]

        return [
            (self.documents[idx], float(similarities[idx]))
            for idx in top_indices
            if similarities[idx] >= threshold
        ]

    def search_by_crop_and_disease(
        self, crop: str, disease: str
    ) -> List[Tuple[Dict, float]]:
        """Targeted search combining crop name + disease name."""
        return self.search(f"Crop: {crop} Disease: {disease}", top_k=3, threshold=0.2)

    @property
    def document_count(self) -> int:
        return len(self.documents)
