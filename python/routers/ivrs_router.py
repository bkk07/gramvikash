"""
IVRS voice-processing router –
  POST /api/v1/ivrs/process
"""

import logging

from fastapi import APIRouter, HTTPException

from models.schemas import IVRSProcessRequest, IVRSProcessResponse

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/ivrs", tags=["IVRS"])

rag_service = None


def set_rag_service(service):
    global rag_service
    rag_service = service


@router.post("/process", response_model=IVRSProcessResponse)
async def process_ivrs_query(request: IVRSProcessRequest):
    """
    Receives transcribed speech from the Java IVRS controller,
    translates → RAG → generates diagnosis → translates back.
    """
    if rag_service is None:
        raise HTTPException(status_code=503, detail="Service not initialised")

    try:
        result = rag_service.process_ivrs_query(
            speech_text=request.speech_text,
            language=request.language.value,
            region=request.region,
        )
        return IVRSProcessResponse(
            original_text=result["original_text"],
            translated_query=result["translated_query"],
            diagnosis=result["diagnosis"],
            translated_response=result["translated_response"],
            language=result["language"],
        )
    except Exception as exc:
        logger.error("IVRS processing error: %s", exc)
        raise HTTPException(
            status_code=500, detail=f"IVRS processing failed: {exc}"
        )
