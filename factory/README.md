# Factory Core

The first executable factory service is the product-manifest validator.

## Validate a product

From the repository root:

```bash
python -m pip install -r requirements-dev.txt
python factory/validate_manifest.py specs/examples/hello-product.json
python -m unittest discover -s tests -p 'test_*.py'
```

Exit code 0 means the manifest conforms to the schema. Non-zero means it must not enter project generation.

## Next components

1. Template catalog with version and platform compatibility.
2. Manifest-driven project instantiation in an isolated output directory.
3. Generated app identity and display-name substitution.
4. Build executor with captured logs and artifact hashes.
5. Acceptance-test executor and evidence bundle.

Do not implement generation by copying arbitrary repository paths from untrusted manifests. Resolve only catalog-approved template IDs and validate all generated identifiers.
