"""
GramVikash AI Service – FastAPI entry-point.

Starts the embedding / RAG / image-classification services at startup
and exposes diagnosis + IVRS endpoints.
"""

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from config import HOST, PORT, EMBEDDING_MODEL_NAME
from models.schemas import HealthResponse
from routers import diagnosis_router, ivrs_router
from services.rag_service import RAGService

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)

rag_service: RAGService = None                    # module-level singleton


@asynccontextmanager
async def lifespan(app: FastAPI):
    global rag_service
    logger.info("Initialising GramVikash AI services …")
    rag_service = RAGService()
    diagnosis_router.set_rag_service(rag_service)
    ivrs_router.set_rag_service(rag_service)
    logger.info("All services initialised ✓")
    yield
    logger.info("Shutting down GramVikash AI services …")


app = FastAPI(
    title="GramVikash AI Service",
    description=(
        "RAG-based crop disease diagnosis and IVRS voice processing "
        "for Indian farmers."
    ),
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(diagnosis_router.router)
app.include_router(ivrs_router.router)


@app.get("/health", response_model=HealthResponse)
async def health_check():
    clf = rag_service.image_classifier if rag_service else None
    return HealthResponse(
        status="healthy",
        rag_documents_loaded=(
            rag_service.embedding_service.document_count if rag_service else 0
        ),
        embedding_model=EMBEDDING_MODEL_NAME,
        image_classifier_model=clf.model_name if clf else "not loaded",
        image_classifier_classes=clf.num_classes if clf else 0,
        image_classifier_ready=clf._model_loaded if clf else False,
    )


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("main:app", host=HOST, port=int(PORT), reload=True)
