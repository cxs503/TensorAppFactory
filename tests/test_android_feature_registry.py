import json
import tempfile
import unittest
from pathlib import Path

from factory.validate_android_features import validate_android_features


class AndroidFeatureRegistryTests(unittest.TestCase):
    def test_accepts_implemented_feature(self):
        self.assertEqual(validate_android_features(["product-identity-home"]), [])

    def test_rejects_planned_feature(self):
        errors = validate_android_features(["basic-navigation"])
        self.assertTrue(any("not implemented" in error for error in errors))

    def test_rejects_unknown_feature(self):
        errors = validate_android_features(["magic-ai-feature"])
        self.assertTrue(any("unknown Android feature" in error for error in errors))

    def test_rejects_duplicate_feature(self):
        errors = validate_android_features(["product-identity-home", "product-identity-home"])
        self.assertTrue(any("duplicate Android feature" in error for error in errors))


if __name__ == "__main__":
    unittest.main()
