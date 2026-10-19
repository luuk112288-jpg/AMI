# REA investigation record

## Current evidence boundary

**No REA executable was run. No Spotify application, APK, website, or network capture was analyzed. This requirement remains blocked.** The source application is an independent implementation of the approved product contract, informed by Android's public APIs.

User-requested upstream: https://github.com/morluto/rea.

Connected GitHub reads obtained:

| File | Observed Git blob |
| --- | --- |
| README.md | 31238bae529b49f6068361e6efa3c120bcc28d80 |
| docs/browser-observation.md | 3ea8b510123de7d106673fc28e64f2a2a7e39e53 |
| docs/android-analysis.md | 50de2ad6c0a95a1bafb3fc12264a2eac8fe25939 |
| package.json | 58c354e11e72f6e3f656a380c39a0d463b9bf26e |

The package metadata declared `rea-agents` 6.7.0. These are upstream source blob identities, not a checked-out commit, installed package digest, runtime capture, or REA Evidence IDs.

## Actual environment attempts

- `git clone --depth 1 https://github.com/morluto/rea.git work/rea` failed: could not resolve host github.com.
- DNS checks for registry.npmjs.org and dl.google.com were denied.
- An HTTPS request for registry.npmjs.org/rea-agents/latest failed with unknown host.
- Node.js v24.19.0 was present; npm/npx, Java, Android SDK, Gradle, and ADB were not found on PATH or conventional checked install locations.
- Android build bootstrap stopped with the explicit missing-JDK error, before compilation.

## Reproduce a real Android investigation

Use the official upstream installation and Android analysis guide. The observed guide specifies a full JDK 17+, a compatible Windows x64 REA package/native controls, and a caller-supplied JADX headless MCP engine. The observed pinned JADX engine was 0.7.1; verify its archive hash against the upstream `src/android/JadxRelease.ts` metadata before execution. Set `JAVA_HOME` and `REA_JADX_MCP_JAR` according to that guide.

Use a target APK you are authorized to inspect. AMI source does not include a reference APK. After prerequisites are ready, use `scripts/inspect-with-rea.ps1` to invoke `inspect-android-package`, or directly run:

```text
npx --yes rea-agents@6.7.0 inspect-android-package <absolute-apk-path> --json
```

Follow with narrowly selected `search-android-classes`, `inspect-android-class`, `inspect-android-method`, and `trace-android-references` commands using names actually returned by REA. Record the artifact SHA-256, command, tool version, returned Evidence ID, coverage exclusions, observed behavior, and how that observation changes AMI. Static analysis does not verify reference runtime behavior.

For website observation, the documented provider requires a compatible user-owned browser with a loopback CDP endpoint. Do not assume an agent browser tab supplies one. No browser/CDP investigation was attempted here.

Keep private reference captures out of source control. Only share approved, relevant evidence summaries. The ignored `captures/` directory is the helper's output location.

## Android implementation references

- https://developer.android.com/media/media3/session/background-playback
- https://developer.android.com/training/data-storage/shared/documents-files
- https://developer.android.com/build/releases/agp-8-9-0-release-notes
- https://developer.android.com/jetpack/androidx/releases/compose-kotlin
- https://developer.android.com/jetpack/androidx/releases/room
- https://developer.android.com/jetpack/androidx/releases/media3

These primary sources describe the implementation APIs; they are not observations about Spotify.
