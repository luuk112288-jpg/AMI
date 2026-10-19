# Verification status

## Verified build — 2026-10-19

Source commit: db2d94c881eeaed053f6c008c58dff9c81cb283d.
[GitHub Actions run](https://github.com/luuk112288-jpg/AMI/actions/runs/39267528097).

- APK job: successful. Gradle :app:testDebugUnitTest :app:assembleDebug completed successfully.
- Device job: successful. All 14 instrumentation tests completed on Android 15 / API 35.
- Two Node audio tests passed; all eight generated demo audio assets passed validation.
- Debug APK artifact: AMI-debug-apk, 34,180,268 bytes as ZIP.
- Artifact ZIP SHA-256: bceef5a5959fc960de077a9a9977c5307f0612ca472bd604e143aaa1433d45c0.
- [Download](https://github.com/luuk112288-jpg/AMI/actions/runs/39267528097/artifacts/12193863155). GitHub sign-in required; extract app-debug.apk.
- Initial CI attempt failed because SDK setup requested the removed tools package. Explicit platform-tools selection fixed this; the next full run passed.

The APK is development-signed, not a production release or Play Store submission.

## Earlier local validation

Audio generation/tests, XML parsing, PowerShell syntax checks, and independent source review completed locally. Local Android compilation was unavailable; the verified Android results above come from GitHub Actions.

## Remaining acceptance checks

- API 26 devices, physical-device audio, notification/lock-screen controls, audio-focus interruptions and headphone disconnect.
- Process-death restoration, cancellation during a real Room commit, low-storage/provider failures and recovery.
- 200% font scale, TalkBack, tablets, narrow phones and landscape.
- The requested REA investigation has not run. See rea-investigation.md. No Spotify integration or reverse-engineered behavior is claimed.
