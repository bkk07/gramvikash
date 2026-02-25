from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any
from enum import Enum


class Language(str, Enum):
    ENGLISH = "en"
    HINDI = "hi"
    TELUGU = "te"


# ── Web Diagnosis ────────────────────────────────────────────────────────────

class DiagnosisRequest(BaseModel):
    user_query: str = Field(..., description="User's description of the crop problem")
    language: Language = Language.ENGLISH
    region: Optional[str] = None
    farmer_id: Optional[int] = None


class DiagnosisResponse(BaseModel):
    classified_disease: Optional[str] = None
    classified_crop: Optional[str] = None
    confidence: Optional[float] = None
    diagnosis: str
    symptoms_matched: List[str] = []
    management_advice: Dict[str, Any] = {}
    region_specific: bool = False
    source: str = "rag"  # "rag" or "llm"
    language: str = "en"


# ── IVRS ─────────────────────────────────────────────────────────────────────

class IVRSProcessRequest(BaseModel):
    speech_text: str = Field(..., description="Transcribed speech from the farmer")
    language: Language = Language.ENGLISH
    region: Optional[str] = None
    farmer_id: Optional[int] = None


class IVRSProcessResponse(BaseModel):
    original_text: str
    translated_query: str
    diagnosis: str
    translated_response: str
    language: str


# ── RAG Search ───────────────────────────────────────────────────────────────

class RAGSearchRequest(BaseModel):
    query: str
    top_k: int = 3


class RAGSearchResult(BaseModel):
    chunk_id: str
    crop: str
    disease: str
    similarity_score: float
    document: Dict[str, Any]


class RAGSearchResponse(BaseModel):
    query: str
    results: List[RAGSearchResult]


# ── Health ───────────────────────────────────────────────────────────────────

class HealthResponse(BaseModel):
    status: str
    rag_documents_loaded: int
    embedding_model: str
    image_classifier_model: str
    image_classifier_classes: int = 0
    image_classifier_ready: bool = False
