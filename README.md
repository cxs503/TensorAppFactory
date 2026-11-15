# TensorAppFactory

**AI-native end-product software factory** — turn validated product specifications into tested, installable software releases.

TensorAppFactory is the production layer for the Tensor ecosystem. It standardizes the path from product definition to source code, reproducible builds, automated verification, and release artifacts.

## First delivery target

Android applications packaged as APKs. The initial reference app lives in `templates/android-native/` and is built by GitHub Actions.

## Factory pipeline

1. **Specify** — describe the product with a versioned, machine-readable product manifest.
2. **Generate / develop** — instantiate a supported application template and implement product behavior.
3. **Build** — produce APK/AAB artifacts with a pinned toolchain.
4. **Verify** — run static checks, unit tests, package checks, and emulator/device tests where available.
5. **Release** — publish versioned artifacts with checksums, provenance, and release notes.

## Repository map

- `specs/`: product, delivery, and machine-readable build-result contracts.
- `factory/`: manifest validator, Android project generator, and manifest-to-APK build runner.
- `templates/android-native/`: minimal native Android reference application.
- `.github/workflows/`: reproducible CI build and test pipeline.
- `docs/`: architecture, quality gates, security, and roadmap.

## Generate an Android product project

The first manifest-driven generator is executable and covered by Python tests:

```bash
python -m pip install -r requirements-dev.txt
python -m unittest discover -s tests -p 'test_*.py'
python factory/build_product.py specs/examples/hello-product.json \
  --output-dir /tmp/hello-tensor \
  --result-file /tmp/hello-tensor-build-result.json
```

The runner validates the manifest, instantiates only the approved `templates/android-native` template, derives a Java-safe package, injects the product name and summary through XML-safe resources, runs Gradle unit tests and APK assembly, captures `build.log`, and emits a machine-readable result validated against `specs/build-result.schema.json`. The result records manifest and APK SHA-256 hashes, relative artifact path, command, exit code, and failure reason. CI publishes the generated APK and result JSON together.

This is a buildable Android shell generator and reproducible build runner, not yet a generator of arbitrary product-specific features or finished commercial apps.

## Build the reference APK

The Android template requires JDK 17 and Gradle 8.9 (the workflow provisions both).

```bash
cd templates/android-native
gradle assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

## Current scope

This repository now has an executable manifest validator and an Android project generator, with CI coverage for generated-project builds. Product-specific feature implementation, broader template catalogues, parallel production jobs, release signing, store publication, and marketplace operations remain future milestones.

## Release readiness

APK generation and release approval are separate states. `factory/verify_release.py` validates the build result, verifies the manifest and APK hashes, and requires passing evidence for every criterion in the product manifest. Its output is governed by `specs/release-gate-result.schema.json`; a build-only artifact is never automatically marked release-ready.

## Quality rule

A successful build is not proof of a useful product. Every product must pass its declared functional acceptance criteria; a release candidate must include test evidence and a reproducible artifact.

## License

Apache-2.0. See [LICENSE](LICENSE).
