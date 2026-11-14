import json
import tempfile
import unittest
from pathlib import Path

from factory.validate_manifest import DEFAULT_SCHEMA, validate_manifest


class ManifestValidatorTests(unittest.TestCase):
    def test_reference_manifest_is_valid(self):
        manifest = Path("specs/examples/hello-product.json")
        self.assertEqual(validate_manifest(manifest), [])

    def test_missing_required_product_field_is_rejected(self):
        source = json.loads(Path("specs/examples/hello-product.json").read_text())
        del source["product"]["name"]
        with tempfile.TemporaryDirectory() as directory:
            candidate = Path(directory) / "invalid.json"
            candidate.write_text(json.dumps(source), encoding="utf-8")
            errors = validate_manifest(candidate, DEFAULT_SCHEMA)
        self.assertTrue(any("'name' is a required property" in error for error in errors))

    def test_unsupported_platform_is_rejected(self):
        source = json.loads(Path("specs/examples/hello-product.json").read_text())
        source["product"]["platforms"] = ["android", "smart-tv"]
        with tempfile.TemporaryDirectory() as directory:
            candidate = Path(directory) / "invalid-platform.json"
            candidate.write_text(json.dumps(source), encoding="utf-8")
            errors = validate_manifest(candidate, DEFAULT_SCHEMA)
        self.assertTrue(any("smart-tv" in error for error in errors))


if __name__ == "__main__":
    unittest.main()
