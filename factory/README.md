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
```

The generator only uses the approved `templates/android-native` template. It rejects invalid manifests, products without Android in their platform list, and non-empty output directories. It derives a Java-safe package name from the product ID, substitutes the display name and project name, relocates the Kotlin source to its generated package path, and copies the validated manifest into the generated project as `product.json`.

## Current boundary

This first generator creates a buildable Android shell. It does not generate product-specific business logic, screens, backend services, store listings, signing keys, or production releases. Those remain explicit implementation and acceptance tasks; a generated shell is not a finished product.

## Next components

1. Template catalog with version and platform compatibility.
2. Product-specific feature modules and screen generation.
3. Build executor with captured logs and artifact hashes.
4. Acceptance-test executor and evidence bundle.
5. Release signing, provenance, and store publication gates.

Do not implement generation by copying arbitrary repository paths from untrusted manifests. Resolve only catalog-approved template IDs and validate all generated identifiers.
