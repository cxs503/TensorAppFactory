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

- `specs/`: product and delivery contracts.
- `templates/android-native/`: minimal native Android reference application.
- `.github/workflows/`: reproducible CI build and test pipeline.
- `docs/`: architecture, quality gates, security, and roadmap.

## Generate an Android product project

The first manifest-driven generator is executable and covered by Python tests:

```bash
python -m pip install -r requirements-dev.txt
python -m unittest discover -s tests -p 'test_*.py'
python factory/create_android_app.py specs/examples/hello-product.json --output-dir /tmp/hello-tensor
cd /tmp/hello-tensor
gradle testDebugUnitTest assembleDebug
```

It instantiates only the approved `templates/android-native` template, derives a Java-safe package from the product ID, applies the product display name, and preserves the validated manifest in the generated project. CI builds both the reference template and a generated project.

This is a buildable Android shell generator, not yet a generator of arbitrary product-specific features or finished commercial apps.

## Build the reference APK

The Android template requires JDK 17 and Gradle 8.9 (the workflow provisions both).

```bash
cd templates/android-native
gradle assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

## Current scope

This repository now has an executable manifest validator and an Android project generator, with CI coverage for generated-project builds. Product-specific feature implementation, broader template catalogues, parallel production jobs, release signing, store publication, and marketplace operations remain future milestones.

## Quality rule

A successful build is not proof of a useful product. Every product must pass its declared functional acceptance criteria; a release candidate must include test evidence and a reproducible artifact.

## License

Apache-2.0. See [LICENSE](LICENSE).
