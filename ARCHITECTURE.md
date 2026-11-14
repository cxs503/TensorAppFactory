# Architecture

TensorAppFactory owns the end-product delivery lifecycle. TensorPlatform can orchestrate agents and work packages; this repository defines product contracts, build contracts, quality gates, and release evidence.

```text
Market brief -> Product manifest -> Template + implementation
            -> Isolated source project -> Build -> APK/AAB
            -> Verification -> Release candidate + evidence
            -> Human/policy approval -> Distribution
```

## Core components

- **Product contract:** platforms, package identity, capabilities, data handling, monetization, acceptance tests, outputs.
- **Template catalog:** versioned starter projects with explicit compatibility.
- **Project instantiator:** validates a manifest and creates an isolated workspace.
- **Build executor:** pinned SDK/JDK/Gradle and dependency versions.
- **Verification executor:** tests declared behavior and emits machine-readable evidence.
- **Release manager:** versioning, signing boundary, checksums, provenance, release notes.

Each app has an independent application ID, secrets, data stores, version stream, and artifacts. Shared templates are versioned; app-specific changes must not mutate other products.

## Initial proof

A Kotlin + Jetpack Compose Android reference app and CI workflow prove source -> debug APK -> artifact upload. This does not yet claim emulator acceptance or production signing.
