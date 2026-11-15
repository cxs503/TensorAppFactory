# Factory Core

The factory has an executable product-manifest validation and Android project-generation path.

## Validate and generate a product

From the repository root:

```bash
python -m pip install -r requirements-dev.txt
python factory/validate_manifest.py specs/examples/hello-product.json
python -m unittest discover -s tests -p 'test_*.py'
python factory/create_android_app.py specs/examples/hello-product.json --output-dir /tmp/hello-tensor
cd /tmp/hello-tensor
gradle testDebugUnitTest assembleDebug

# Or run the complete manifest-to-APK pipeline and emit evidence JSON:
cd -
python factory/build_product.py specs/examples/hello-product.json \
  --output-dir /tmp/hello-tensor-build \
  --result-file /tmp/hello-tensor-build-result.json
```

The `build_product.py` runner composes validation, project generation, Gradle unit tests, APK assembly, SHA-256 hashing, build-log capture, and a machine-readable result JSON. The result contract is defined in `specs/build-result.schema.json` and validated before the runner writes it. A successful result includes product ID, manifest SHA-256, relative APK path, APK SHA-256, command, exit code, and build-log path. CI publishes the result JSON beside the generated APK. Failed runs also emit a schema-valid result with a failure reason; the default result file is written next to (not inside) the generated project so it does not poison a retry.

The generator only uses the approved `templates/android-native` template. It rejects invalid manifests, products without Android in their platform list, non-empty output directories, and Android feature claims that are unknown or not implemented. The machine-readable registry is `specs/android-feature-catalog.json`; `factory/validate_android_features.py` enforces it before generation. The reference shell currently implements only `product-identity-home`. Navigation, launch screen, localization settings, progress persistence, content loading, and quiz engine are explicitly marked planned, so an Agent cannot claim them as generated capabilities. It derives a Java-safe package name from the product ID, substitutes the display name and project name, relocates the Kotlin source to its generated package path, and copies the validated manifest into the generated project as `product.json`.

## Release-readiness gate

A successful APK build is not a release approval. The separate `factory/verify_release.py` gate requires:

- A schema-valid product manifest and build-result JSON.
- A schema-valid acceptance-result JSON for the same product.
- Passing evidence for every acceptance criterion declared by the product manifest, with no undeclared or duplicate criteria.
- The APK to exist inside the generated project and its actual SHA-256 to match the build result.

Run it with:

```bash
python factory/verify_release.py \
  specs/examples/hello-product.json \
  /path/to/build-result.json \
  /path/to/acceptance-result.json \
  --result-file /path/to/release-gate.json
```

Exit code 0 means `release_ready`; exit code 1 means `blocked`. The result contract is `specs/release-gate-result.schema.json`. The gate is deliberately separate from APK generation: do not manufacture acceptance evidence or infer product quality from a successful compile.

## Current boundary

This first generator creates a buildable Android shell. The template now renders the product name and summary through Android string resources, with XML-safe escaping for special characters. It does not generate product-specific business logic, screens, backend services, store listings, signing keys, or production releases. Those remain explicit implementation and acceptance tasks; a generated shell is not a finished product.

## Versioned Android content packages

The native template includes `ContentPackageLoader` and a sample asset at `app/src/main/assets/content/demo-reading.json`. The loader accepts schema version `1.0`, validates package identity and language, rejects duplicate question IDs, and enforces type-specific rules for single-choice, TRUE/FALSE/NOT GIVEN, and short-answer questions. Short-answer entries can carry accepted alternatives and a maximum word count. The corresponding JSON Schema is `specs/android-content-package.schema.json`; JVM tests cover valid mixed packages and invalid version, duplicate ID, invalid choice answer, and missing accepted-answer cases.

This is a reusable data-loading and validation capability, not yet a complete learner-facing quiz UI. The feature catalog records only the loader as implemented; the quiz engine remains planned.

## Feature implementation contract

Every Android feature named in a product manifest must exist in `specs/android-feature-catalog.json` with `status: implemented`. Planned or unknown features block project generation. When a feature is implemented, update its catalog status only in the same change that adds its generator/template implementation and automated tests. The catalog is a capability gate, not a roadmap promise.

## Next components

1. Versioned, composable feature modules with real generated screens and behavior.
2. Content package loader and quiz engine, then progress persistence.
3. Build executor with captured logs and artifact hashes.
4. Acceptance-test executor and evidence bundle.
5. Release signing, provenance, and store publication gates.

Do not implement generation by copying arbitrary repository paths from untrusted manifests. Resolve only catalog-approved template IDs and validate all generated identifiers.
