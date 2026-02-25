# GramVikash – Python AI Service

Provides **RAG-based crop-disease diagnosis** and **IVRS voice processing** via FastAPI.

## Quick Start

```bash
cd python
pip install -r requirements.txt

# Copy and fill in your API keys
cp .env.example .env        # edit GROQ_API_KEY at minimum

# Run
python main.py              # starts on http://localhost:8000
```

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/diagnose` | Web diagnosis (text + optional image) |
| `POST` | `/api/v1/diagnose/text-only` | Text-only diagnosis |
| `POST` | `/api/v1/rag/search` | Raw RAG similarity search |
| `POST` | `/api/v1/ivrs/process` | IVRS pipeline (translate → RAG → respond) |
| `GET`  | `/health` | Health check |

## Environment Variables

| Variable | Default | Required |
|----------|---------|----------|
| `GROQ_API_KEY` | – | **Yes** |
| `EMBEDDING_MODEL` | `sentence-transformers/all-MiniLM-L6-v2` | No |
| `IMAGE_CLASSIFIER_MODEL` | `linkanjarad/mobilenet_v2_1.0_224-plant-disease-identification` | No |
| `GROQ_MODEL` | `llama-3.1-70b-versatile` | No |
| `RAG_TOP_K` | `3` | No |
| `RAG_SIMILARITY_THRESHOLD` | `0.3` | No |

## Architecture

```
User Query + Image
       │
       ▼
┌─────────────────────────────┐
│  Image Classifier (HF)     │  ← classifies crop + disease
│  sentence-transformers      │  ← embeds query for RAG
│  FAISS / numpy search       │  ← cosine similarity against 16 docs
│  Groq LLM (Llama 3.1)      │  ← generates structured diagnosis
└─────────────────────────────┘
       │
       ▼
Structured Response:
  - Classified disease
  - Matched symptoms
  - Management advice (cultural + chemical)
  - Region-specific tips
```
