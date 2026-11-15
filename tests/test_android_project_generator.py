import json
import tempfile
import unittest
import xml.etree.ElementTree as ET
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
            strings = (output / "app/src/main/res/values/strings.xml").read_text(encoding="utf-8")
            self.assertIn('applicationId = "dev.tensor.products.hello_tensor"', gradle)
            self.assertIn("package dev.tensor.products.hello_tensor", activity)
            self.assertIn('android:label="@string/app_name"', manifest)
            self.assertIn("<string name=" + '"app_name">Hello Tensor</string>', strings)
            self.assertIn("reference app that verifies", strings)
            self.assertTrue((output / "product.json").is_file())
            self.assertTrue((output / "settings.gradle.kts").is_file())

    def test_escapes_product_identity_in_android_resources(self):
        source = json.loads(REFERENCE.read_text(encoding="utf-8"))
        source["product"]["name"] = "Reader & Writer <Coach>"
        source["product"]["summary"] = "A reading & writing coach that supports learners with practical exercises."
        with tempfile.TemporaryDirectory() as directory:
            manifest_path = Path(directory) / "special-product.json"
            manifest_path.write_text(json.dumps(source), encoding="utf-8")
            output = Path(directory) / "generated"
            self.assertEqual(create_android_app(manifest_path, output), [])
            strings = ET.parse(output / "app/src/main/res/values/strings.xml")
            values = {node.get("name"): node.text for node in strings.getroot().findall("string")}
            self.assertEqual(values["app_name"], "Reader & Writer <Coach>")
            self.assertEqual(values["product_summary"], source["product"]["summary"])

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


    def test_rejects_manifest_claiming_unimplemented_android_feature(self):
        source = json.loads(REFERENCE.read_text(encoding="utf-8"))
        source["product"]["features"] = ["basic-navigation"]
        with tempfile.TemporaryDirectory() as directory:
            manifest_path = Path(directory) / "planned-feature.json"
            manifest_path.write_text(json.dumps(source), encoding="utf-8")
            output = Path(directory) / "generated"
            errors = create_android_app(manifest_path, output)
            self.assertTrue(any("not implemented" in error for error in errors))
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
