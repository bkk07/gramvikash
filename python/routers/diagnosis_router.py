"""
Web diagnosis router –
  POST /api/v1/diagnose           (text + image)
  POST /api/v1/diagnose/text-only (text only)
  POST /api/v1/rag/search         (raw RAG search)
"""

import logging
from typing import Optional

from fastapi import APIRouter, File, Form, HTTPException, UploadFile

from models.schemas import (
    DiagnosisResponse,
    RAGSearchRequest,
    RAGSearchResponse,
    RAGSearchResult,
)

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1", tags=["Diagnosis"])

# Will be wired from main.py at startup
rag_service = None


def set_rag_service(service):
    global rag_service
    rag_service = service


# ── endpoints ────────────────────────────────────────────────────────────────


@router.post("/diagnose", response_model=DiagnosisResponse)
async def diagnose_crop(
    user_query: str = Form(..., description="Description of the crop problem"),
    language: str = Form("en", description="Language code: en, hi, te"),
    region: Optional[str] = Form(None, description="Region / state of the farmer"),
    farmer_id: Optional[int] = Form(None, description="Farmer ID"),
    image: Optional[UploadFile] = File(None, description="Image of the affected crop"),
):
    """
    Web diagnosis pipeline.
    Accepts user text + optional image → classifies disease → RAG → LLM → response.
    """
    if rag_service is None:
        raise HTTPException(status_code=503, detail="Service not initialised")

    image_bytes = None
    if image:
        image_bytes = await image.read()
        logger.info("Received image: %s (%d bytes)", image.filename, len(image_bytes))

    try:
        result = rag_service.diagnose_with_image(
            user_query=user_query,
            image_bytes=image_bytes,
            language=language,
            region=region,
        )
        return DiagnosisResponse(**result)
    except Exception as exc:
        logger.error("Diagnosis error: %s", exc)
        raise HTTPException(status_code=500, detail=f"Diagnosis failed: {exc}")


@router.post("/diagnose/text-only", response_model=DiagnosisResponse)
async def diagnose_text_only(
    user_query: str = Form(...),
    language: str = Form("en"),
    region: Optional[str] = Form(None),
):
    """Text-only diagnosis (no image)."""
    if rag_service is None:
        raise HTTPException(status_code=503, detail="Service not initialised")

    try:
        result = rag_service.diagnose_text_only(
            query=user_query, language=language, region=region
        )
        return DiagnosisResponse(**result)
    except Exception as exc:
        logger.error("Text diagnosis error: %s", exc)
        raise HTTPException(status_code=500, detail=f"Diagnosis failed: {exc}")


@router.post("/rag/search", response_model=RAGSearchResponse)
async def search_rag(request: RAGSearchRequest):
    """Raw RAG similarity search."""
    if rag_service is None:
        raise HTTPException(status_code=503, detail="Service not initialised")

    results = rag_service.search_rag(request.query, request.top_k)
    return RAGSearchResponse(
        query=request.query,
        results=[RAGSearchResult(**r) for r in results],
    )
