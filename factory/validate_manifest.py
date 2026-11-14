#!/usr/bin/env python3
"""Validate a TensorAppFactory product manifest against the repository contract."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

from jsonschema import Draft202012Validator


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SCHEMA = ROOT / "specs" / "product.schema.json"


def validate_manifest(manifest_path: Path, schema_path: Path = DEFAULT_SCHEMA) -> list[str]:
    try:
        schema = json.loads(schema_path.read_text(encoding="utf-8"))
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    except OSError as exc:
        return [f"file error: {exc}"]
    except json.JSONDecodeError as exc:
        return [f"invalid JSON: {exc}"]

    Draft202012Validator.check_schema(schema)
    validator = Draft202012Validator(schema)
    errors = sorted(validator.iter_errors(manifest), key=lambda error: list(map(str, error.path)))
    return [
        f"{'.'.join(map(str, error.absolute_path)) or '<root>'}: {error.message}"
        for error in errors
    ]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("manifest", type=Path, help="Path to product JSON manifest")
    parser.add_argument("--schema", type=Path, default=DEFAULT_SCHEMA)
    args = parser.parse_args()

    errors = validate_manifest(args.manifest, args.schema)
    if errors:
        print("Manifest validation failed:")
        for error in errors:
            print(f"  - {error}")
        return 1

    print(f"Manifest valid: {args.manifest}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
