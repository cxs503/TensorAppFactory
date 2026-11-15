# English Exam Coach — Android Delivery Specification

**Status:** Initial implementation contract  
**Product ID:** `english-exam-coach`  
**Factory:** TensorAppFactory  
**Target:** Installable Android APK built through the repository's reproducible pipeline.

## 1. Delivery principle

A successful Gradle build is necessary but not sufficient. The APK must implement the learner journey and pass functional acceptance tests. No screen may be presented as functional when it is only a static mock or placeholder.

## 2. Minimum app journeys

### J1 — First launch and onboarding
- App launches without network access to a blank or broken screen.
- Learner can set target band, target date, and daily study time.
- Choices survive process death and app relaunch.
- The app explains that results are preparation estimates, not official scores.

### J2 — Diagnostic
- Learner can start, pause where appropriate, finish, and review a diagnostic.
- Each question is linked to a content-pack version and answer-key version.
- Result reports skill coverage and limitations; missing skill evidence is not fabricated.

### J3 — Daily practice
- Learner can open a task, answer it, submit, and receive a result.
- Objective items are deterministically scored from a versioned key.
- Explanations and review state are persisted.
- A learner can revisit incorrect answers.

### J4 — Writing practice
- Learner can enter and save a response.
- Feedback is clearly labelled formative/AI-generated unless a human-reviewed evaluation is supplied.
- Rubric dimensions are displayed separately; the app must not present an uncalibrated estimate as an official band.

### J5 — Speaking practice
- Learner can record, review, and submit a response, subject to runtime permission.
- Permission denial has a usable text-entry fallback.
- Recording state is visible; cancel/delete behavior works.
- Audio is not uploaded or retained without clear disclosure and explicit consent.

### J6 — Progress and study plan
- Learner can see completed sessions, skill-level evidence, and next tasks.
- Progress survives app restart.
- The plan does not claim improvement without comparable assessment evidence.

## 3. Content package contract

Every shipped content item must include stable IDs and:
- exam adapter and skill;
- task type and difficulty metadata;
- prompt/instructions and response schema;
- answer key and explanation where objectively scorable;
- rubric reference where subjectively assessed;
- content version and schema version;
- provenance and rights state;
- review status and reviewer evidence.

The runtime must reject release content with missing required fields, unknown rights, unresolved ambiguity, or failed QA status.

## 4. Technical expectations

- Use the existing native Android template and pinned toolchain in TensorAppFactory.
- Keep domain logic separate from UI rendering.
- Use deterministic local persistence for learner profile, sessions, answers, and progress.
- Handle configuration changes, process recreation, and app relaunch.
- Provide accessible touch targets, readable typography, loading/error/empty states, and clear navigation.
- Do not require an account or backend for the first end-to-end local MVP.
- Network-dependent AI feedback must fail gracefully and preserve the learner's draft.

## 5. Required automated verification

- Unit tests for scoring, plan progression, content validation, and persistence.
- UI/instrumentation tests for J1, J3, J4, J5 permission fallback, and J6.
- Build test that produces a non-empty APK and records its SHA-256 checksum.
- Package metadata/version verification.
- At least one emulator smoke test in CI where the runner supports it; otherwise document the exact limitation and provide a reproducible local command.
- A manual test record on a physical Android device before public release.

## 6. Release evidence

A release candidate must attach:
- APK and SHA-256;
- source commit and build workflow run;
- automated test summary;
- emulator/physical-device test record;
- content-pack version and rights report;
- known limitations and privacy/data handling statement.

## 7. Explicit non-goals for first APK

- No fake live AI score or invented learner analytics.
- No hardcoded demo user progress presented as real.
- No commercial question-bank imports without documented rights.
- No claim that app functionality is complete merely because navigation screens render.
- No official IELTS score claims or guaranteed outcomes.
