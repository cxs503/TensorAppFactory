#!/usr/bin/env python3
"""Gate release readiness on a verified APK and complete product acceptance evidence."""

from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path

from jsonschema import Draft202012Validator

try:
    from .validate_manifest import validate_manifest
except ImportError:  # Direct script execution
    from validate_manifest import validate_manifest


ROOT = Path(__file__).resolve().parents[1]
BUILD_SCHEMA = ROOT / "specs" / "build-result.schema.json"
ACCEPTANCE_SCHEMA = ROOT / "specs" / "acceptance-result.schema.json"
GATE_SCHEMA = ROOT / "specs" / "release-gate-result.schema.json"


def read_json(path: Path) -> tuple[dict | None, str | None]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        return None, str(exc)
    if not isinstance(value, dict):
        return None, "top-level JSON value must be an object"
    return value, None


def schema_errors(payload: dict, schema_path: Path, label: str) -> list[str]:
    schema = json.loads(schema_path.read_text(encoding="utf-8"))
    validator = Draft202012Validator(schema)
    return [f"{label}: {error.message}" for error in validator.iter_errors(payload)]


def verify_release(
    manifest_path: Path,
    build_result_path: Path,
    acceptance_result_path: Path,
    result_file: Path,
) -> tuple[int, dict]:
    manifest, manifest_error = read_json(manifest_path)
    build, build_error = read_json(build_result_path)
    acceptance, acceptance_error = read_json(acceptance_result_path)

    product = manifest.get("product", {}) if manifest else {}
    product_id = product.get("id")
    required = [
        item.get("id")
        for item in manifest.get("delivery", {}).get("acceptance_criteria", [])
        if isinstance(item, dict) and isinstance(item.get("id"), str)
    ] if manifest else []

    build_status = build.get("status", "failed") if build else "failed"
    acceptance_status = acceptance.get("status") if acceptance else None
    failures: list[str] = []

    if manifest_error:
        failures.append(f"manifest cannot be read: {manifest_error}")
    else:
        failures.extend(validate_manifest(manifest_path))
    if build_error:
        failures.append(f"build result cannot be read: {build_error}")
    elif build:
        failures.extend(schema_errors(build, BUILD_SCHEMA, "build result"))
    if acceptance_error:
        failures.append(f"acceptance result cannot be read: {acceptance_error}")
    elif acceptance:
        failures.extend(schema_errors(acceptance, ACCEPTANCE_SCHEMA, "acceptance result"))

    if build and product_id and build.get("product_id") != product_id:
        failures.append("build result product_id does not match product manifest")
    if acceptance and product_id and acceptance.get("product_id") != product_id:
        failures.append("acceptance result product_id does not match product manifest")
    if build_status != "passed":
        failures.append("build status is not passed")
    if acceptance_status != "passed":
        failures.append("product acceptance status is not passed")

    artifact_verified = False
    artifact_sha256 = build.get("sha256") if build else None
    if build and build_status == "passed" and build.get("project_dir") and build.get("artifact"):
        project_dir = Path(build["project_dir"]).resolve()
        artifact_path = (project_dir / build["artifact"]).resolve()
        try:
            artifact_path.relative_to(project_dir)
        except ValueError:
            failures.append("artifact path escapes the generated project directory")
        else:
            if not artifact_path.is_file():
                failures.append("built APK artifact is missing")
            else:
                actual_hash = hashlib.sha256(artifact_path.read_bytes()).hexdigest()
                if actual_hash != build.get("sha256"):
                    failures.append("built APK SHA-256 does not match build result")
                else:
                    artifact_verified = True
    else:
        failures.append("build result does not identify a passed APK artifact")

    accepted = acceptance.get("criteria", []) if acceptance else []
    accepted_by_id: dict[str, dict] = {}
    for item in accepted:
        criterion_id = item.get("criterion_id")
        if criterion_id in accepted_by_id:
            failures.append(f"duplicate acceptance evidence for criterion {criterion_id}")
        accepted_by_id[criterion_id] = item

    if len(required) != len(set(required)):
        failures.append("product manifest contains duplicate acceptance criterion IDs")
    missing = sorted(set(required) - set(accepted_by_id))
    extra = sorted(set(accepted_by_id) - set(required))
    if missing:
        failures.append("missing acceptance evidence for: " + ", ".join(missing))
    if extra:
        failures.append("acceptance evidence contains undeclared criteria: " + ", ".join(extra))

    passed_criteria = sorted(
        criterion_id
        for criterion_id, item in accepted_by_id.items()
        if criterion_id in required and item.get("status") == "passed"
    )
    failed_criteria = sorted(
        criterion_id
        for criterion_id in required
        if criterion_id in accepted_by_id and accepted_by_id[criterion_id].get("status") != "passed"
    )
    if failed_criteria:
        failures.append("acceptance criteria failed: " + ", ".join(failed_criteria))

    result = {
        "schema_version": "1.0",
        "status": "release_ready" if not failures and artifact_verified else "blocked",
        "product_id": product_id,
        "build_status": build_status if build_status in {"passed", "failed"} else "failed",
        "acceptance_status": acceptance_status if acceptance_status in {"passed", "failed"} else None,
        "artifact_sha256": artifact_sha256 if isinstance(artifact_sha256, str) else None,
        "artifact_verified": artifact_verified,
        "required_criteria": sorted(required),
        "passed_criteria": passed_criteria,
        "failures": failures,
    }

    gate_schema = json.loads(GATE_SCHEMA.read_text(encoding="utf-8"))
    Draft202012Validator(gate_schema).validate(result)
    result_file.parent.mkdir(parents=True, exist_ok=True)
    result_file.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return (0 if result["status"] == "release_ready" else 1), result


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("manifest", type=Path)
    parser.add_argument("build_result", type=Path)
    parser.add_argument("acceptance_result", type=Path)
    parser.add_argument("--result-file", required=True, type=Path)
    args = parser.parse_args()

    code, result = verify_release(
        args.manifest,
        args.build_result,
        args.acceptance_result,
        args.result_file,
    )
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return code


if __name__ == "__main__":
    sys.exit(main())
