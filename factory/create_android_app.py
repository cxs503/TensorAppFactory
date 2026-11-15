#!/usr/bin/env python3
"""Instantiate a validated Android project from the approved native template."""

from __future__ import annotations

import argparse
import json
import re
import shutil
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

try:
    from .validate_manifest import validate_manifest
except ImportError:  # Direct script execution
    from validate_manifest import validate_manifest


ROOT = Path(__file__).resolve().parents[1]
TEMPLATE = ROOT / "templates" / "android-native"
PACKAGE_PREFIX = "dev.tensor.products"


def package_for(product_id: str) -> str:
    safe_id = product_id.replace("-", "_")
    return f"{PACKAGE_PREFIX}.{safe_id}"


def project_name_for(product_id: str) -> str:
    return "".join(part.capitalize() for part in re.split(r"[-_]+", product_id))


def create_android_app(manifest_path: Path, output_dir: Path) -> list[str]:
    errors = validate_manifest(manifest_path)
    if errors:
        return [f"manifest: {error}" for error in errors]

    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    product = manifest["product"]
    if "android" not in product["platforms"]:
        return ["product.platforms must include 'android'"]

    destination = output_dir.expanduser().resolve()
    template = TEMPLATE.resolve()
    if destination == template or template in destination.parents:
        return ["output directory must not be inside the approved template"]
    if destination.exists() and any(destination.iterdir()):
        return [f"output directory is not empty: {destination}"]

    package_name = package_for(product["id"])
    project_name = project_name_for(product["id"])
    display_name = product["name"]

    try:
        destination.mkdir(parents=True, exist_ok=True)
        shutil.copytree(TEMPLATE, destination, dirs_exist_ok=True)
        replacements = {
            "dev.tensorappfactory.reference": package_name,
            "TensorAndroidReference": project_name,
        }
        for path in destination.rglob("*"):
            if not path.is_file() or path.suffix not in {".kt", ".kts", ".xml", ".md"}:
                continue
            original = path.read_text(encoding="utf-8")
            updated = original
            for old, new in replacements.items():
                updated = updated.replace(old, new)
            if updated != original:
                path.write_text(updated, encoding="utf-8")
        strings_path = destination / "app" / "src" / "main" / "res" / "values" / "strings.xml"
        strings_tree = ET.parse(strings_path)
        strings_root = strings_tree.getroot()
        resource_values = {
            "app_name": display_name,
            "product_summary": product["summary"],
        }
        for resource in strings_root.findall("string"):
            if resource.get("name") in resource_values:
                resource.text = resource_values[resource.get("name")]
        strings_tree.write(strings_path, encoding="utf-8", xml_declaration=True)
        old_package_dir = destination / "app" / "src" / "main" / "java" / "dev" / "tensorappfactory" / "reference"
        new_package_dir = destination / "app" / "src" / "main" / "java" / Path(*package_name.split("."))
        if old_package_dir.exists():
            new_package_dir.parent.mkdir(parents=True, exist_ok=True)
            shutil.move(str(old_package_dir), str(new_package_dir))
            shutil.rmtree(destination / "app" / "src" / "main" / "java" / "dev" / "tensorappfactory", ignore_errors=True)
        (destination / "product.json").write_text(
            json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
    except (OSError, UnicodeError) as exc:
        return [f"generation failed: {exc}"]

    return []


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("manifest", type=Path, help="Validated product JSON manifest")
    parser.add_argument("--output-dir", required=True, type=Path)
    args = parser.parse_args()

    errors = create_android_app(args.manifest, args.output_dir)
    if errors:
        print("Android project generation failed:")
        for error in errors:
            print(f"  - {error}")
        return 1

    print(f"Android project generated: {args.output_dir.resolve()}")
    print(f"Build with: cd {args.output_dir.resolve()} && gradle assembleDebug")
    return 0


if __name__ == "__main__":
    sys.exit(main())
