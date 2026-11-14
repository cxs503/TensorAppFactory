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

## Build the reference APK

The Android template requires JDK 17 and Gradle 8.9 (the workflow provisions both).

```bash
cd templates/android-native
gradle assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

## Current scope

This repository is the factory foundation, not yet a self-service product generator or app marketplace. The first milestone proves one end-to-end Android build. Subsequent milestones add manifest validation, template instantiation, parallel jobs, emulator tests, signing, and release automation.

## Quality rule

A successful build is not proof of a useful product. Every product must pass its declared functional acceptance criteria; a release candidate must include test evidence and a reproducible artifact.

## License

Apache-2.0. See [LICENSE](LICENSE).
