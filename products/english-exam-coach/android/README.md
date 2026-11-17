# English Exam Coach — Android MVP

This is the first product-specific Android target. It is intentionally separate from the generic TensorAppFactory reference template.

## Implemented vertical slice

- First-run IELTS Academic target-band selection (6.0–8.0), persisted locally.
- Chinese/English interface toggle for the current screens.
- All three items in the versioned original reading benchmark are loaded from the packaged JSON asset:
  - Single-answer multiple choice.
  - TRUE / FALSE / NOT GIVEN.
  - Short answer requiring all accepted answer phrases.
- Deterministic objective-answer checking with explanations.
- Tappable option rows, retry after an incorrect answer, and next-question progression.
- Correct-answer and answer-attempt counters persisted locally.
- Resume an interrupted reading practice from the study space, including the current item,
  selection or short-answer draft, and submitted feedback. The saved draft and counts use the
  same app-private snapshot. Content-pack revision changes invalidate old drafts and per-item review
  records while retaining aggregate counters.
- Review mistakes by question: see total wrong submissions, up to five recent wrong answers,
  the expected answer and rationale, and whether a later attempt corrected the mistake.
  Correction is an in-app practice status, not mastery or a band estimate. Reviewing alone
  is read-only and does not count as another attempt.
- Start a focused retry from a review card without replacing a normal practice draft.
  Finish or exit the focused retry to return to the saved session. A repeated attempt counts
  as practice evidence, not a fresh diagnostic.
- The interface language preference is stored locally. Practice content and explanations remain
  in English, and drafts and wrong answers are not sent anywhere.
- Explicit disclosure that this build is not affiliated with IELTS and does not provide official scores.

## Not implemented yet

Listening audio playback, Writing/Speaking evaluation, recording, adaptive planning, timed mock exams, account sync, and commercial payments are not implemented. Do not market this build as a complete IELTS preparation product.

## Build and verification

Requires JDK 17 and Gradle 8.9:

```bash
cd products/english-exam-coach/android
gradle --no-daemon :app:testDebugUnitTest
gradle --no-daemon :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

The GitHub Actions workflow `.github/workflows/english-exam-coach-android.yml` runs unit tests, builds the debug APK, generates a SHA-256 checksum, and uploads both files as the `english-exam-coach-debug-apk` artifact.

Verified CI build: [run #43676223926](https://github.com/cxs503/TensorAppFactory/actions/runs/43676223926). The successful CI build is not a substitute for real-device installation and usability testing.

## Content source and release status

The packaged questions come from the original, versioned benchmark fixture in TensorProductFactory. They are draft benchmark items, not official IELTS questions, and have not yet passed independent expert review. The app's deterministic answer checking is limited to the current objective reading formats; it is not an IELTS band-score estimator.
