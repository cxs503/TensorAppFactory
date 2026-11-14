# Agent Operating Contract

This file is the entry point for autonomous coding agents working in TensorAppFactory.

## Mission

Deliver complete end-user software products from approved product specifications. Do not optimize for code volume, document volume, task count, or APK count.

## Mandatory workflow

1. Read `VISION.md`, `ARCHITECTURE.md`, `docs/QUALITY-GATES.md`, and the relevant product manifest.
2. Identify the user, the core job, and observable acceptance criteria before implementation.
3. Create an isolated work package and branch for each product or independently reviewable change.
4. Implement the smallest complete vertical slice. Do not leave the primary journey as a mock, placeholder, or TODO.
5. Run manifest validation, unit tests, build, and relevant acceptance tests.
6. Inspect the generated artifact and verify package ID, version, permissions, and declared capabilities.
7. Report exact commands, results, artifact checksum, limitations, and evidence links.
8. Submit a PR. Do not merge or publish a production release unless explicitly authorized.

## Role boundaries

- **Product agent:** market evidence, target user, problem, differentiation, monetization hypothesis, acceptance criteria.
- **UX agent:** information architecture, interaction flows, accessibility, responsive states.
- **Implementation agent:** working application behavior, persistence, networking, error handling.
- **Test agent:** independent tests against acceptance criteria; must not merely repeat the implementation agent's claims.
- **Security agent:** permissions, privacy/data flows, secrets, dependency and license review.
- **Release agent:** reproducible build, version, artifact integrity, provenance, release notes.

One agent may hold multiple roles in a small work package, but test and release evidence must remain independently inspectable.

## Prohibited shortcuts

- No fake backend, fabricated data, nonfunctional buttons, or screenshots presented as working features.
- No declaring success because compilation passed.
- No silently weakening acceptance criteria to make tests pass.
- No embedding secrets, signing keys, personal data, or credentials in source.
- No generating near-duplicate apps merely to inflate output counts.

## Completion report

Every work package must report:
- product/work-package ID and source revision;
- user problem and completed user journeys;
- acceptance criteria: pass/fail/not tested, with evidence;
- build command, build result, artifact path and SHA-256;
- test commands and exact results;
- permissions, data collection, network and dependency review;
- known limitations and next blocking issue.

If evidence is absent, mark the claim **not verified**.
