import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from factory.verify_release import verify_release


ROOT = Path(__file__).resolve().parents[1]
REFERENCE = ROOT / "specs" / "examples" / "hello-product.json"


class ReleaseGateTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.project = self.root / "project"
        self.project.mkdir()
        self.apk = self.project / "app/build/outputs/apk/debug/app-debug.apk"
        self.apk.parent.mkdir(parents=True)
        self.apk.write_bytes(b"release-gate-test-apk")
        self.manifest = json.loads(REFERENCE.read_text(encoding="utf-8"))
        self.manifest_path = self.root / "product.json"
        self.manifest_path.write_text(json.dumps(self.manifest), encoding="utf-8")
        self.build_path = self.root / "build-result.json"
        self.acceptance_path = self.root / "acceptance-result.json"
        self.result_path = self.root / "release-gate.json"
        self.build = {
            "result_schema_version": "1.0",
            "status": "passed",
            "product_id": "hello-tensor",
            "project_dir": str(self.project),
            "manifest_sha256": hashlib.sha256(self.manifest_path.read_bytes()).hexdigest(),
            "artifact": "app/build/outputs/apk/debug/app-debug.apk",
            "sha256": hashlib.sha256(self.apk.read_bytes()).hexdigest(),
            "build_log": "build.log",
            "command": ["gradle", "--no-daemon", "testDebugUnitTest", "assembleDebug"],
            "gradle_exit_code": 0,
            "failure_reason": None,
        }
        self.acceptance = {
            "schema_version": "1.0",
            "product_id": "hello-tensor",
            "status": "passed",
            "criteria": [{
                "criterion_id": "APP-001",
                "status": "passed",
                "test_ref": "tests/test_main_activity.py::test_home_screen",
                "evidence": ["Automated UI assertion verified product name and summary on launch."],
            }],
        }
        self._write_inputs()

    def tearDown(self):
        self.temp.cleanup()

    def _write_inputs(self):
        self.build_path.write_text(json.dumps(self.build), encoding="utf-8")
        self.acceptance_path.write_text(json.dumps(self.acceptance), encoding="utf-8")

    def test_release_ready_requires_valid_build_and_all_acceptance_evidence(self):
        code, result = verify_release(
            self.manifest_path, self.build_path, self.acceptance_path, self.result_path
        )
        self.assertEqual(code, 0)
        self.assertEqual(result["status"], "release_ready")
        self.assertTrue(result["artifact_verified"])
        self.assertEqual(result["required_criteria"], ["APP-001"])
        self.assertEqual(json.loads(self.result_path.read_text())["status"], "release_ready")

    def test_missing_acceptance_criterion_blocks_release(self):
        self.acceptance["criteria"] = []
        self._write_inputs()
        code, result = verify_release(
            self.manifest_path, self.build_path, self.acceptance_path, self.result_path
        )
        self.assertEqual(code, 1)
        self.assertEqual(result["status"], "blocked")
        self.assertTrue(any("missing acceptance evidence" in item for item in result["failures"]))

    def test_failed_acceptance_criterion_blocks_release(self):
        self.acceptance["status"] = "failed"
        self.acceptance["criteria"][0]["status"] = "failed"
        self._write_inputs()
        code, result = verify_release(
            self.manifest_path, self.build_path, self.acceptance_path, self.result_path
        )
        self.assertEqual(code, 1)
        self.assertEqual(result["status"], "blocked")
        self.assertTrue(any("acceptance criteria failed" in item for item in result["failures"]))

    def test_modified_apk_blocks_release_even_when_build_json_claims_pass(self):
        self.apk.write_bytes(b"tampered-after-build")
        code, result = verify_release(
            self.manifest_path, self.build_path, self.acceptance_path, self.result_path
        )
        self.assertEqual(code, 1)
        self.assertFalse(result["artifact_verified"])
        self.assertTrue(any("SHA-256 does not match" in item for item in result["failures"]))

    def test_manifest_change_after_build_blocks_release(self):
        self.manifest["product"]["name"] = "Changed after build"
        self.manifest_path.write_text(json.dumps(self.manifest), encoding="utf-8")
        code, result = verify_release(
            self.manifest_path, self.build_path, self.acceptance_path, self.result_path
        )
        self.assertEqual(code, 1)
        self.assertTrue(any("manifest SHA-256" in item for item in result["failures"]))

    def test_build_product_id_mismatch_blocks_release(self):
        self.build["product_id"] = "different-product"
        self._write_inputs()
        code, result = verify_release(
            self.manifest_path, self.build_path, self.acceptance_path, self.result_path
        )
        self.assertEqual(code, 1)
        self.assertTrue(any("product_id does not match" in item for item in result["failures"]))


if __name__ == "__main__":
    unittest.main()
