#!/usr/bin/env python3
"""Validate Android product feature claims against the implemented feature catalog."""

from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG_PATH = ROOT / "specs" / "android-feature-catalog.json"


def validate_android_features(features: list[str], catalog_path: Path = CATALOG_PATH) -> list[str]:
    catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
    entries = {item["id"]: item for item in catalog["features"]}
    errors: list[str] = []
    seen: set[str] = set()
    for feature_id in features:
        if feature_id in seen:
            errors.append(f"duplicate Android feature: {feature_id}")
            continue
        seen.add(feature_id)
        entry = entries.get(feature_id)
        if entry is None:
            errors.append(f"unknown Android feature: {feature_id}")
        elif entry["status"] != "implemented":
            errors.append(f"Android feature is not implemented: {feature_id} (status={entry['status']})")
    return errors
