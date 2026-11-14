# Roadmap

## M0 — Reference build
- [x] Establish repository, mission, and architecture.
- [x] Add Android native reference app and CI APK build.
- [ ] Observe successful remote workflow and inspect artifact.

## M1 — Product contract
- [ ] Finalize JSON Schema and validator.
- [ ] Define package identity, permissions, data classification, acceptance criteria.

## M2 — Repeatable generation
- [ ] Template catalog and compatibility metadata.
- [ ] Safe manifest-driven project instantiation.
- [ ] Generate two isolated apps from one template.

## M3 — Quality gates
- [ ] Unit and instrumentation conventions.
- [ ] Emulator smoke tests.
- [ ] Dependency/license/secret/permission scans.
- [ ] Machine-readable quality report; missing mandatory evidence fails closed.

## M4 — Release engineering
- [ ] Protected signing secrets and key custody.
- [ ] Versioned APK/AAB release workflow.
- [ ] Checksums, provenance, changelog, rollback.
- [ ] Store handoff with explicit human approval.

## M5 — Agent-scale production
- [ ] TensorPlatform work-package interface.
- [ ] Product, UX, implementation, test, security, release role contracts.
- [ ] Parallel isolated builds, quotas, retries, cost accounting.
- [ ] Product quality and commercial outcome metrics.

A checkbox is complete only when reproducible acceptance evidence exists.
