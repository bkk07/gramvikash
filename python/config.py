import os
from pathlib import Path
from dotenv import load_dotenv

load_dotenv()

# ── Paths ────────────────────────────────────────────────────────────────────
BASE_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = BASE_DIR.parent
RAG_DOCUMENTS_PATH = PROJECT_ROOT / "gramvikash_rag_documents.json"

# ── Model configs ───────────────────────────────────────────────────────────
EMBEDDING_MODEL_NAME = os.getenv(
    "EMBEDDING_MODEL", "sentence-transformers/all-MiniLM-L6-v2"
)

# ── Custom image classifier (EfficientNet-B0 trained on Colab) ───────────────
# Place model.pth and class_names.json inside python/model/
MODEL_DIR = BASE_DIR / "model"
MODEL_DIR.mkdir(exist_ok=True)
MODEL_WEIGHTS_PATH = os.getenv("MODEL_WEIGHTS_PATH", str(MODEL_DIR / "model.pth"))
CLASS_NAMES_PATH = os.getenv("CLASS_NAMES_PATH", str(MODEL_DIR / "class_names.json"))

# ── Groq LLM ────────────────────────────────────────────────────────────────
GROQ_API_KEY = os.getenv("GROQ_API_KEY", "")
GROQ_MODEL = os.getenv("GROQ_MODEL", "llama-3.1-70b-versatile")

# ── RAG settings ─────────────────────────────────────────────────────────────
RAG_TOP_K = int(os.getenv("RAG_TOP_K", "3"))
RAG_SIMILARITY_THRESHOLD = float(os.getenv("RAG_SIMILARITY_THRESHOLD", "0.3"))

# ── TTS ──────────────────────────────────────────────────────────────────────
TTS_OUTPUT_DIR = BASE_DIR / "tts_output"
TTS_OUTPUT_DIR.mkdir(exist_ok=True)

# ── Server ───────────────────────────────────────────────────────────────────
HOST = os.getenv("FASTAPI_HOST", "0.0.0.0")
PORT = int(os.getenv("FASTAPI_PORT", "8000"))
