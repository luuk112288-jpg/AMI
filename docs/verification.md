# AMI 0.2.0 verification — 2026-10-19

Source commit: c8c667abf630dac68c90a748f54f032c4346e890.
[Successful GitHub Actions run](https://github.com/luuk112288-jpg/AMI/actions/runs/39275146863).

- APK build and all JVM unit tests passed.
- All 21 Android instrumentation tests passed on Android 15 / API 35.
- Tests cover phone MediaStore scanning, embedded title/artist/album/album artist/year/genre/artwork, duplicate prevention after rescan, scanned content-URI preparation in ExoPlayer, retained playlist/like relationships when files disappear and return, fresh-import artwork, existing-import metadata enrichment, v1-to-v2 database migration, navigation/branding and the existing playback/library features.
- Two Node audio tests and eight demo asset validations passed.
- Two new UI tests failed on the old implementation before implementation, as expected, and pass on this build.
- Independent read-only review identified two issues: URI-volume mismatch in the test and missing enrichment of earlier imports. Both were addressed and the final suite passed.
- The first device build stopped because FFmpeg was absent; the workflow now explicitly installs it before generating its synthetic tagged audio fixture.

APK ZIP: AMI-debug-apk, 35,159,737 bytes.
SHA-256: c376a627ad89efc021905ed532f94ca47725b7b6e5bf5e6c3ea511f98604219d.
[Download](https://github.com/luuk112288-jpg/AMI/actions/runs/39275146863/artifacts/12197625959).

The APK is development-signed. It is not a Play Store/production release. Signing keys from fresh CI runners can differ, preventing in-place updates; uninstalling removes AMI's private library/imports but does not remove the original scanned audio. Production distribution needs a persistent signing key.

## Remaining manual/device checks

- API 26–32 storage permissions and vendor-specific media providers.
- Real-device permission denial/settings return, headphone/audio-focus behavior and lock-screen display.
- Very large libraries, low-storage conditions, TalkBack and 200% font scale.
- REA investigation remains unexecuted; this update makes no Spotify integration or REA-derived behavior claim.

The phone scanner requests audio-read permission and queries MediaStore. It does not copy, modify or delete original audio. Unindexed downloads can be selected through the existing document-picker import flow. Embedded cover/tag availability depends on the user's files.
