import json
import tempfile
import unittest
from pathlib import Path

from factory.create_android_app import create_android_app, package_for


ROOT = Path(__file__).resolve().parents[1]
REFERENCE = ROOT / "specs" / "examples" / "hello-product.json"


class AndroidProjectGeneratorTests(unittest.TestCase):
    def test_package_name_is_deterministic_and_java_safe(self):
        self.assertEqual(package_for("hello-tensor"), "dev.tensor.products.hello_tensor")

    def test_generates_buildable_project_with_product_identity(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "generated-app"
            self.assertEqual(create_android_app(REFERENCE, output), [])
            gradle = (output / "app/build.gradle.kts").read_text(encoding="utf-8")
            activity = next(output.glob("app/src/main/java/**/MainActivity.kt")).read_text(encoding="utf-8")
            manifest = (output / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
            self.assertIn('applicationId = "dev.tensor.products.hello_tensor"', gradle)
            self.assertIn("package dev.tensor.products.hello_tensor", activity)
            self.assertIn('android:label="Hello Tensor"', manifest)
            self.assertTrue((output / "product.json").is_file())
            self.assertTrue((output / "settings.gradle.kts").is_file())

    def test_rejects_non_android_product_without_creating_output(self):
        source = json.loads(REFERENCE.read_text(encoding="utf-8"))
        source["product"]["platforms"] = ["web"]
        with tempfile.TemporaryDirectory() as directory:
            manifest = Path(directory) / "web.json"
            manifest.write_text(json.dumps(source), encoding="utf-8")
            output = Path(directory) / "output"
            errors = create_android_app(manifest, output)
            self.assertTrue(any("include 'android'" in error for error in errors))
            self.assertFalse(output.exists())

    def test_rejects_nonempty_output_directory(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "occupied"
            output.mkdir()
            (output / "keep.txt").write_text("do not overwrite", encoding="utf-8")
            errors = create_android_app(REFERENCE, output)
            self.assertTrue(any("not empty" in error for error in errors))
            self.assertEqual((output / "keep.txt").read_text(encoding="utf-8"), "do not overwrite")


if __name__ == "__main__":
    unittest.main()
