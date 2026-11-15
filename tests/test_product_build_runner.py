import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
from types import SimpleNamespace

from factory.build_product import build_product


ROOT = Path(__file__).resolve().parents[1]
REFERENCE = ROOT / "specs" / "examples" / "hello-product.json"


class ProductBuildRunnerTests(unittest.TestCase):
    def test_success_emits_artifact_hash_and_machine_readable_result(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            output = root / "generated"
            result_file = root / "evidence" / "build-result.json"

            def fake_gradle(command, cwd, **kwargs):
                apk = cwd / "app/build/outputs/apk/debug/app-debug.apk"
                apk.parent.mkdir(parents=True, exist_ok=True)
                apk.write_bytes(b"test-apk-bytes")
                return SimpleNamespace(returncode=0, stdout="BUILD SUCCESSFUL", stderr="")

            with patch("factory.build_product.subprocess.run", side_effect=fake_gradle):
                code, result = build_product(REFERENCE, output, result_file)

            self.assertEqual(code, 0)
            self.assertEqual(result["status"], "passed")
            self.assertEqual(result["product_id"], "hello-tensor")
            self.assertEqual(len(result["sha256"]), 64)
            self.assertTrue(Path(result["artifact"]).is_file())
            self.assertEqual(json.loads(result_file.read_text())["sha256"], result["sha256"])
            self.assertTrue((output / "build.log").is_file())

    def test_failed_gradle_emits_failure_evidence(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            output = root / "generated"
            result_file = root / "build-result.json"
            failed = SimpleNamespace(returncode=1, stdout="", stderr="compile error")
            with patch("factory.build_product.subprocess.run", return_value=failed):
                code, result = build_product(REFERENCE, output, result_file)

            self.assertEqual(code, 1)
            self.assertEqual(result["status"], "failed")
            self.assertEqual(result["gradle_exit_code"], 1)
            self.assertIn("build.log", result["failure_reason"])
            self.assertEqual(json.loads(result_file.read_text())["status"], "failed")

    def test_missing_manifest_emits_failure_without_running_gradle(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            result_file = root / "result.json"
            with patch("factory.build_product.subprocess.run") as run:
                code, result = build_product(root / "missing.json", root / "out", result_file)
            self.assertEqual(code, 1)
            self.assertIn("cannot read product manifest", result["failure_reason"])
            run.assert_not_called()


if __name__ == "__main__":
    unittest.main()
