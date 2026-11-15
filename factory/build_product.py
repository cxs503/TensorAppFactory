#!/usr/bin/env python3
"""Run one manifest-driven Android product build and emit machine-readable evidence."""

from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
import sys
from pathlib import Path

from jsonschema import Draft202012Validator

try:
    from .create_android_app import create_android_app
except ImportError:  # Direct script execution
    from create_android_app import create_android_app


ROOT = Path(__file__).resolve().parents[1]
RESULT_SCHEMA = ROOT / "specs" / "build-result.schema.json"


def write_result(path: Path, payload: dict) -> None:
    schema = json.loads(RESULT_SCHEMA.read_text(encoding="utf-8"))
    Draft202012Validator(schema).validate(payload)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def build_product(
    manifest_path: Path,
    output_dir: Path,
    result_file: Path | None = None,
    gradle_command: str = "gradle",
) -> tuple[int, dict]:
    result_path = result_file or output_dir.with_name(f"{output_dir.name}-build-result.json")
    base = {
        "result_schema_version": "1.0",
        "status": "failed",
        "product_id": None,
        "project_dir": str(output_dir.resolve()),
        "manifest_sha256": None,
        "artifact": None,
        "sha256": None,
        "build_log": None,
        "command": None,
        "gradle_exit_code": None,
        "failure_reason": None,
    }

    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        base["product_id"] = manifest.get("product", {}).get("id")
    except (OSError, json.JSONDecodeError) as exc:
        base["failure_reason"] = f"cannot read product manifest: {exc}"
        write_result(result_path, base)
        return 1, base

    base["manifest_sha256"] = hashlib.sha256(manifest_path.read_bytes()).hexdigest()
    errors = create_android_app(manifest_path, output_dir)
    if errors:
        base["failure_reason"] = "; ".join(errors)
        write_result(result_path, base)
        return 1, base

    command = [gradle_command, "--no-daemon", "testDebugUnitTest", "assembleDebug"]
    base["command"] = command
    try:
        completed = subprocess.run(
            command,
            cwd=output_dir,
            check=False,
            capture_output=True,
            text=True,
        )
    except OSError as exc:
        base["failure_reason"] = f"could not execute Gradle: {exc}"
        write_result(result_path, base)
        return 1, base

    log_path = output_dir / "build.log"
    log_path.write_text(
        "COMMAND: " + " ".join(command) + "\n\nSTDOUT:\n" + completed.stdout
        + "\nSTDERR:\n" + completed.stderr,
        encoding="utf-8",
    )
    base["build_log"] = "build.log"
    base["gradle_exit_code"] = completed.returncode
    if completed.returncode != 0:
        base["failure_reason"] = "Gradle test/build command failed; see build.log"
        write_result(result_path, base)
        return 1, base

    apk_path = output_dir / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
    if not apk_path.is_file() or apk_path.stat().st_size == 0:
        base["failure_reason"] = "Gradle succeeded but the expected debug APK is missing or empty"
        write_result(result_path, base)
        return 1, base

    digest = hashlib.sha256(apk_path.read_bytes()).hexdigest()
    base.update({
        "status": "passed",
        "artifact": str(apk_path.relative_to(output_dir)),
        "sha256": digest,
    })
    write_result(result_path, base)
    return 0, base


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("manifest", type=Path)
    parser.add_argument("--output-dir", required=True, type=Path)
    parser.add_argument("--result-file", type=Path)
    parser.add_argument("--gradle-command", default="gradle")
    args = parser.parse_args()

    code, result = build_product(
        args.manifest,
        args.output_dir,
        args.result_file,
        args.gradle_command,
    )
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return code


if __name__ == "__main__":
    sys.exit(main())
