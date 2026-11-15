# English Exam Coach — Android MVP

This is the first product-specific Android target. It is intentionally separate from the generic TensorAppFactory reference template.

## Current vertical slice

- First-run target-band selection, persisted locally.
- Chinese/English interface toggle for the current screens.
- Reading practice content loaded from the versioned original benchmark JSON asset.
- Deterministic answer checking and explanation.
- Correct-session count persisted locally.
- Explicit disclosure that this build is not affiliated with IELTS and does not provide official scores.

## Not implemented yet

Listening audio playback, Writing/Speaking evaluation, recording, adaptive planning, timed mock exams, account sync, and commercial payments are not implemented. Do not market this build as a complete IELTS preparation product.

## Build

Requires JDK 17 and Gradle 8.9:

```bash
cd products/english-exam-coach/android
gradle --no-daemon :app:testDebugUnitTest
gradle --no-daemon :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

The GitHub Actions workflow `.github/workflows/english-exam-coach-android.yml` runs unit tests, builds the debug APK, and uploads the APK plus SHA-256 checksum as an artifact.

## Content source

The packaged question is copied from the original, versioned benchmark fixture in TensorProductFactory. It is a draft benchmark item and must not be treated as independently reviewed or as an official IELTS question.
