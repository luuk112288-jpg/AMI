# AMI

A native Android music app with a dark, lime-accented interface, original demo music, and a private on-device library.

**Delivery status:** APK compiled successfully; JVM unit tests and all 14 Android instrumentation tests passed on API 35. [Download the APK ZIP](https://github.com/luuk112288-jpg/AMI/actions/runs/39267528097/artifacts/12193863155) (GitHub sign-in required). Extract and install `app-debug.apk`. REA has not run; see `docs/rea-investigation.md`.

## Features in source

- Home with album collections, artists, and recent listening.
- Search across tracks, artists, albums, and playlists.
- Persistent playlists, ordering, liked songs, and playback preferences.
- Multi-file audio import through Android's document picker; private copies, metadata extraction, duplicate detection, and cleanup on failure.
- Media3 service-owned playback: background/lock-screen controls, play/pause, seeking, volume/mute, shuffle, repeat, previous/next, and editable queue.
- Full now-playing screen, mini player, and phone navigation.
- Eight original offline demo compositions, abstract artwork, and app icon. Demo artist names are fictional.
- No Spotify credentials, Spotify catalog, account requirement, ads, or purchases.

Requires Android 8.0 (API 26) or newer. Package name: `com.ami.music`. The initial library and imports remain on one device. Uninstalling the app removes the library and its private audio copies.

## Build on Windows

1. Install Android Studio with a JDK 17 (or configure its Gradle JDK to 17), Android SDK Platform 35, and Build Tools 35.0.0. Set `JAVA_HOME` to the JDK directory and `ANDROID_HOME` to the SDK directory. Accept the SDK licenses in SDK Manager.
2. Open this folder in Android Studio. The source package intentionally contains no fabricated Gradle wrapper binary. Run the bootstrap below once to download the official Gradle distribution, verify its SHA-256 against Gradle's HTTPS checksum endpoint, and generate the wrapper.
3. From a PowerShell terminal in this directory:

```powershell
node scripts/generate-audio.mjs
pwsh -File scripts/build.ps1
```

This runs unit tests and `assembleDebug`. The APK is produced at `app/build/outputs/apk/debug/app-debug.apk`. It uses Android's development signing key. It is not a production release or Play Store submission.

For emulator/device tests, start an emulator or connect a developer-enabled Android device and run:

```powershell
pwsh -File scripts/build.ps1 -DeviceTests
```

Alternatively use an installed **Gradle 8.11.1** (JDK 17 and SDK 35 still required):

```text
gradle :app:testDebugUnitTest :app:assembleDebug
gradle :app:connectedDebugAndroidTest
gradle wrapper --gradle-version 8.11.1 --distribution-type bin
```

The GitHub Actions workflow builds an APK and runs emulator tests. Both jobs passed in [run 39267528097](https://github.com/luuk112288-jpg/AMI/actions/runs/39267528097).

## Use AMI

Start with the bundled AMI Originals. Tap any track, album play button, or collection to listen. The mini player opens Now playing; the queue icon opens track ordering and removal. In Your library, use **+** for a playlist and **Import audio** for local files. The track menu adds music to a playlist or opens an artist/album. Hearts populate Liked songs. Playlist options support rename and delete; up/down controls change track order.

Audio files must contain a decoder-supported audio stream and be at most 512 MB each. The Android media decoder determines format support. AMI copies selected files into app-private storage and detects duplicate content with SHA-256. It does not scan your device or request broad storage access.

## REA

The requested upstream toolkit is https://github.com/morluto/rea (MIT). It is a development/investigation dependency, not an Android UI framework or music backend. This source uses its documentation to specify an evidence-based investigation path; no Spotify internals were analyzed here. The original requirement to rely on a **completed REA investigation remains unmet** because downloads and analysis prerequisites were unavailable.

For an authorized local APK, after installing the prerequisites described in `docs/rea-investigation.md`:

```powershell
pwsh -File scripts/inspect-with-rea.ps1 -ApkPath 'C:\path\to\authorized-reference.apk'
```

Review the evidence and amend `docs/rea-investigation.md` with actual findings before claiming reverse-engineered behavior. The script pins the official npm package `rea-agents@6.7.0`, as observed in the upstream package metadata at authoring time. Target APKs and REA providers are not bundled.

## Project map

- `app/src/main/java/com/ami/music/data`: Room models, repository, imports, demo catalog, preferences.
- `app/src/main/java/com/ami/music/playback`: MediaSessionService, controller, player UI state and policy.
- `app/src/main/java/com/ami/music/ui`: Compose screens, navigation, dialogs, reusable components and theme.
- `app/src/main/assets/audio`: original WAV tracks bundled for offline playback.
- `app/src/test`: playback policy and search tests.
- `app/src/androidTest`: database, import, navigation and media integration tests.
- `scripts`: Gradle bootstrap, REA helper, deterministic audio generation and asset tests.

## Regenerate demo audio

Node.js is needed to generate demo assets in a GitHub checkout before building; it is not an Android runtime dependency.

```text
node --test scripts/audio.test.mjs
node scripts/generate-audio.mjs
```

The source ZIP includes the original PCM files. GitHub checkouts generate them from the deterministic source. See `ATTRIBUTION.md` for asset provenance.


GitHub checkouts generate the original demo audio with `node scripts/generate-audio.mjs` before building. GitHub Actions performs this automatically.
