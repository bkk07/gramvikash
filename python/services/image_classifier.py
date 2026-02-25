"""
Image Classifier – loads a custom EfficientNet-B0 model trained on
PlantVillage-style data (model.pth + class_names.json) and classifies
crop disease images.

The model is trained externally (Colab) and the artefacts are placed in
the ``python/model/`` directory:
    python/model/model.pth          – state_dict
    python/model/class_names.json   – ordered list of class folder names

Labels follow the ``Crop___Disease`` convention and are mapped to
(crop, disease) pairs that match the RAG corpus.
"""

import io
import json
import logging
from pathlib import Path
from typing import Dict, List, Optional, Tuple

import torch
import torch.nn as nn
from PIL import Image
from torchvision import models, transforms

from config import MODEL_WEIGHTS_PATH, CLASS_NAMES_PATH

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Label → (crop, disease) mapping
# Maps the folder-name labels produced by ImageFolder ("Crop___Disease")
# to the human-readable names used in the RAG documents.
# ---------------------------------------------------------------------------
LABEL_MAPPING: Dict[str, Tuple[str, str]] = {
    # Potato
    "potato___early_blight":            ("Potato", "Early Blight"),
    "potato___late_blight":             ("Potato", "Late Blight"),
    "potato___healthy":                 ("Potato", "Healthy"),
    # Rice
    "rice___blast":                     ("Rice / Paddy", "Blast"),
    "rice___brown_spot":                ("Rice / Paddy", "Brown Spot"),
    "rice___bacterial_leaf_blight":     ("Rice / Paddy", "Bacterial Leaf Blight"),
    "rice___tungro":                    ("Rice / Paddy", "Tungro Disease"),
    # Wheat
    "wheat___leaf_rust":                ("Wheat", "Leaf Rust / Brown Rust"),
    "wheat___brown_rust":               ("Wheat", "Leaf Rust / Brown Rust"),
    "wheat___stem_rust":                ("Wheat", "Stem Rust / Black Rust"),
    "wheat___black_rust":               ("Wheat", "Stem Rust / Black Rust"),
    "wheat___stripe_rust":              ("Wheat", "Stripe Rust / Yellow Rust"),
    "wheat___yellow_rust":              ("Wheat", "Stripe Rust / Yellow Rust"),
    "wheat___loose_smut":               ("Wheat", "Loose Smut"),
    # Sugarcane
    "sugarcane___red_rot":              ("Sugarcane", "Red Rot"),
    "sugarcane___smut":                 ("Sugarcane", "Smut"),
    "sugarcane___wilt":                 ("Sugarcane", "Wilt"),
    "sugarcane___grassy_shoot":         ("Sugarcane", "Grassy Shoot Disease"),
    "sugarcane___ratoon_stunting":      ("Sugarcane", "Ratoon Stunting Disease"),
}

# ── image transform (must match training) ────────────────────────────────────
_INFERENCE_TRANSFORM = transforms.Compose([
    transforms.Resize((224, 224)),
    transforms.ToTensor(),
])


def _build_model(num_classes: int) -> nn.Module:
    """Reconstruct the EfficientNet-B0 architecture used during training."""
    model = models.efficientnet_b0(weights=None)
    model.classifier[1] = nn.Linear(model.classifier[1].in_features, num_classes)
    return model


class ImageClassifier:
    """Lazy-loaded custom EfficientNet-B0 classifier."""

    def __init__(self) -> None:
        self.model: Optional[nn.Module] = None
        self.class_names: List[str] = []
        self._model_loaded = False

    # ── lazy load ────────────────────────────────────────────────────────

    def _load_model(self) -> None:
        if self._model_loaded:
            return

        weights_path = Path(MODEL_WEIGHTS_PATH)
        classes_path = Path(CLASS_NAMES_PATH)

        if not weights_path.exists():
            logger.warning(
                "Model weights not found at %s – image classification disabled. "
                "Train the model on Colab and place model.pth + class_names.json "
                "in python/model/",
                weights_path,
            )
            return

        if not classes_path.exists():
            logger.warning(
                "class_names.json not found at %s – image classification disabled.",
                classes_path,
            )
            return

        try:
            # load class names
            with open(classes_path, "r", encoding="utf-8") as fh:
                self.class_names = json.load(fh)
            logger.info(
                "Loaded %d class names from %s", len(self.class_names), classes_path
            )

            # build & load model
            self.model = _build_model(len(self.class_names))
            state_dict = torch.load(
                weights_path, map_location="cpu", weights_only=True
            )
            self.model.load_state_dict(state_dict)
            self.model.eval()

            self._model_loaded = True
            logger.info(
                "Image classifier loaded: EfficientNet-B0 (%d classes) from %s",
                len(self.class_names),
                weights_path,
            )
        except Exception as exc:
            logger.error("Failed to load image classifier: %s", exc)
            self._model_loaded = False

    # ── public API ───────────────────────────────────────────────────────

    def classify(
        self, image_bytes: bytes
    ) -> Tuple[Optional[str], Optional[str], float]:
        """
        Classify a crop-disease image.

        Returns
        -------
        (crop_name, disease_name, confidence)
        """
        self._load_model()
        if not self._model_loaded or self.model is None:
            logger.warning("Image classifier not available – skipping")
            return None, None, 0.0

        try:
            image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
            tensor = _INFERENCE_TRANSFORM(image).unsqueeze(0)  # [1, 3, 224, 224]

            with torch.no_grad():
                outputs = self.model(tensor)
                probs = torch.softmax(outputs, dim=1)
                confidence, predicted = torch.max(probs, 1)

            raw_label = self.class_names[predicted.item()]
            conf = float(confidence.item())
            logger.info("Raw classification: %s (%.3f)", raw_label, conf)

            crop, disease = self._map_label(raw_label)
            if crop and disease:
                logger.info("Mapped to: %s – %s", crop, disease)
                return crop, disease, conf

            # unmapped label – split on "___" as fallback
            crop_fb, disease_fb = self._split_label(raw_label)
            return crop_fb, disease_fb, conf

        except Exception as exc:
            logger.error("Image classification failed: %s", exc)
            return None, None, 0.0

    # ── mapping helpers ──────────────────────────────────────────────────

    def _map_label(
        self, label: str
    ) -> Tuple[Optional[str], Optional[str]]:
        """Look up the label in LABEL_MAPPING (case-insensitive)."""
        normalised = label.lower().strip().replace(" ", "_").replace("-", "_")

        if normalised in LABEL_MAPPING:
            return LABEL_MAPPING[normalised]

        # partial / fuzzy match
        for key, (crop, disease) in LABEL_MAPPING.items():
            if key in normalised or normalised in key:
                return crop, disease

        return None, None

    @staticmethod
    def _split_label(label: str) -> Tuple[str, str]:
        """Fallback: split a 'Crop___Disease' label into (crop, disease)."""
        if "___" in label:
            parts = label.split("___", 1)
            crop = parts[0].replace("_", " ").strip().title()
            disease = parts[1].replace("_", " ").strip().title()
            return crop, disease
        return label, "Unknown"

    @property
    def model_name(self) -> str:
        return "EfficientNet-B0 (custom)"

    @property
    def num_classes(self) -> int:
        return len(self.class_names)
