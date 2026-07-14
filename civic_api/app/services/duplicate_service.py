"""Duplicate complaint detection add-on.

Uses location radius + category similarity + description text similarity.
Does not replace existing complaint creation or routing logic.
"""

from __future__ import annotations

import math
import re
from difflib import SequenceMatcher

from sqlalchemy.orm import Session

from app.config import settings
from app.models import Complaint

# Semantic category buckets (frontend values + legacy labels)
CATEGORY_GROUPS: dict[str, set[str]] = {
    "road": {"road", "pothole", "street", "bridge", "footpath", "public works"},
    "waste": {"waste", "garbage", "sanitation", "cleanliness", "trash", "dirty"},
    "electricity": {"electricity", "streetlight", "power", "light", "electric", "transformer"},
    "water": {"water", "drainage", "sewage", "leakage", "pipe", "tap", "flood"},
    "other": {"other", "general", "municipal"},
}

STOPWORDS = {
    "a", "an", "the", "and", "or", "of", "to", "in", "on", "at", "for", "is", "are",
    "was", "were", "be", "been", "being", "with", "by", "from", "this", "that",
    "it", "its", "as", "near", "please", "very", "there", "here", "my", "our",
}


def _normalize_category(category: str | None) -> str:
    if not category:
        return "other"
    text = category.lower().strip().replace("_", " ").replace("-", " ")
    for group, keywords in CATEGORY_GROUPS.items():
        if group in text:
            return group
        if any(k in text for k in keywords):
            return group
    return "other"


def categories_similar(a: str | None, b: str | None) -> bool:
    return _normalize_category(a) == _normalize_category(b)


def haversine_meters(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """Great-circle distance between two points in meters."""
    r = 6371000.0
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    d_phi = math.radians(lat2 - lat1)
    d_lambda = math.radians(lon2 - lon1)
    a = math.sin(d_phi / 2) ** 2 + math.cos(phi1) * math.cos(phi2) * math.sin(d_lambda / 2) ** 2
    return 2 * r * math.asin(math.sqrt(a))


def _tokenize(text: str | None) -> set[str]:
    if not text:
        return set()
    tokens = re.findall(r"[a-z0-9]+", text.lower())
    return {t for t in tokens if len(t) > 2 and t not in STOPWORDS}


def description_similarity(a: str | None, b: str | None) -> float:
    """Hybrid similarity: Jaccard on tokens + SequenceMatcher ratio."""
    tokens_a, tokens_b = _tokenize(a), _tokenize(b)
    if tokens_a and tokens_b:
        jaccard = len(tokens_a & tokens_b) / len(tokens_a | tokens_b)
    else:
        jaccard = 0.0

    seq = SequenceMatcher(None, (a or "").lower().strip(), (b or "").lower().strip()).ratio()
    return max(jaccard, seq * 0.85 + jaccard * 0.15)


def find_duplicate_complaint(
    db: Session,
    *,
    description: str,
    category: str,
    latitude: float,
    longitude: float,
    radius_meters: float | None = None,
    text_threshold: float | None = None,
) -> Complaint | None:
    """Return the best matching open complaint, or None."""
    if not settings.duplicate_check_enabled:
        return None

    radius = radius_meters if radius_meters is not None else settings.duplicate_radius_meters
    threshold = (
        text_threshold
        if text_threshold is not None
        else settings.duplicate_text_similarity_threshold
    )

    # Only match open issues (resolved ones are treated as closed / new)
    candidates = (
        db.query(Complaint)
        .filter(Complaint.status.in_(["NEW", "IN_PROGRESS"]))
        .filter(Complaint.latitude.isnot(None), Complaint.longitude.isnot(None))
        .all()
    )

    best: Complaint | None = None
    best_score = -1.0

    for existing in candidates:
        if not categories_similar(category, existing.category):
            continue

        distance = haversine_meters(latitude, longitude, existing.latitude, existing.longitude)
        if distance > radius:
            continue

        text_score = description_similarity(description, existing.description)
        if text_score < threshold:
            continue

        # Prefer closer + more similar matches
        distance_factor = 1.0 - (distance / radius)
        combined = (text_score * 0.7) + (distance_factor * 0.3)
        if combined > best_score:
            best_score = combined
            best = existing

    return best
