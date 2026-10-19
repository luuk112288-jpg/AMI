# AMI — application specification

Date: 2026-10-19
Status: Approved design snapshot. Source has since been implemented; see verification.md for current build and testing limits.

## Goal

Create an installable native Android Spotify-style music application named AMI. The user approved home, search, albums, artists, playlists, liked songs, a working audio player, saved library state, demo audio, and personal audio imports, then explicitly required a mobile Android app. The user explicitly requires reliance on https://github.com/morluto/rea. Android is the primary deliverable; a companion website is deferred.

## REA's role and evidence

REA is the required investigation tool, not a music app template or streaming service. Use its documented website observation and JavaScript analysis workflows to investigate accessible reference behavior, retain provenance, and translate observations into independently implemented AMI behavior. Do not represent inferred or unobserved behavior as REA findings.

Reviewed upstream files: README.md (blob 31238bae529b49f6068361e6efa3c120bcc28d80) and docs/browser-observation.md (blob 3ea8b510123de7d106673fc28e64f2a2a7e39e53). README documents Node.js 24.11+ support, CLI/MCP setup, and JavaScript/website analysis. Browser observation requires a compatible browser exposing a loopback CDP endpoint. Existing agent-controlled browser access does not by itself satisfy that requirement.

Environment finding: Node.js v24.19.0 is available. Git clone failed because github.com could not be resolved by the shell. Connected GitHub reads succeeded. No REA executable has been installed or run, and no Spotify target has been captured. Resolve installation and target access before claiming REA-derived implementation. If these remain unavailable, report that dependency as blocked rather than silently substituting a generic implementation.

## Approach selection

Recommended: a Kotlin Android application with Jetpack Compose, Media3 ExoPlayer, Room, and DataStore, with REA used during investigation and verification. Minimum Android API 26. Resolve and pin a compatible stable build toolchain during implementation from official Android documentation. Application ID: com.ami.music. App label: AMI.

Alternative: a visual-only static demonstration is cheaper but does not satisfy the approved working music application. Alternative: a Spotify-account client depends on external credentials and service playback permissions and would not supply an independent audio catalog. Neither is the selected scope.

## User experience

Use a dark charcoal interface, vivid green primary controls, clear AMI branding, large album artwork, bottom navigation, a compact player, and an expandable now-playing screen. Tablet layouts may use a library rail. Support Android back navigation, system bars, portrait and landscape, TalkBack labels, font scaling, accessible touch targets, and reduced animation preferences.

Home shows curated music collections and recently played items. Search matches tracks, albums, artists, and playlists, with filters, loading feedback when applicable, and a useful empty state. Album and artist pages expose playable tracks and navigation between related entities. No navigation item or prominent control is a decorative dead end.

Library contains liked tracks, personal playlists, and imported audio. Users can create, rename, and delete playlists, add or remove tracks, and change playback order. Destructive playlist deletion uses a clear confirmation. Playback and library actions update consistently across screens.

## Playback contract

Use a Media3 ExoPlayer owned by a MediaSessionService, with UI connected through MediaController. Implement play/pause, previous/next, seek, volume, mute, shuffle, repeat-off/repeat-all/repeat-one, queue visibility, queue editing, and track completion. Starting an album or playlist establishes that collection's queue. Manual next advances through the queue; repeat-one applies to natural track completion. Previous restarts the current track after three seconds, otherwise returns to the previous queue item. Shuffle preserves the current track and retains an original order for switching it off. Support background and lock-screen playback, notification controls, audio focus, and pausing when headphones disconnect. Declare required foreground-service media-playback permissions and lifecycle behavior.

Display actual metadata and playback duration. Do not use timers to simulate playback. Handle rejected autoplay, failed media loads, unsupported formats, unavailable tracks, empty queues, and buffering with understandable feedback. Do not auto-start audio on first load.

## Data and persistence

Define Track, Album, Artist, Playlist, LibraryState, and PlayerState models with stable IDs. Track references include title, artist IDs, album ID, artwork, duration when known, source type, and playable source. Playlist stores ordered track IDs and creation/update times. PlayerState stores queue, current item, modes, volume, and last position; restoration must require a user action before sound starts.

Use Room for tracks, playlists, membership order, likes, and history; DataStore for playback preferences and restoration state. Import audio through Android's system document picker and copy accepted audio into app-private storage so later playback does not depend on provider URI permissions. Imported audio stays on that device; explain that clearly. Validate media, extract metadata, handle cancellation and duplicate imports, remove incomplete copies after failures, and report insufficient storage without losing previously saved items. Avoid broad storage permissions. No cross-device synchronization or account system is promised by this scope.

Bundle licensed or original playable demo audio and artwork with attribution as needed. Clearly identify demonstration content. Spotify's commercial catalog is not supplied by REA; any future integration requires its own authorized service connection. Never ship credentials in client code.

## Architecture and delivery

Build a Kotlin Android app with separate catalog/data, persistence, playback, and Compose view modules. Use ViewModels and StateFlow for lifecycle-aware UI state. Use one service-owned player. Retain source, REA investigation notes, setup instructions, and attribution together in the project. Tests cover pure playback policy, Room data contracts, imports, and key Compose flows.

Deliver Android Studio source and, when the toolchain is available and the build succeeds, an installable debug APK. Clearly label debug signing; do not claim a Play Store release. Keep credentials and release signing keys out of source. A production release and store publication require separate release configuration. Current checks found no Java, Gradle, ADB, or Android SDK on PATH/environment; inspect conventional SDK locations before concluding they are absent.

## Verification and acceptance

Validate queue transitions, repeat and shuffle semantics, playlist persistence and edits, imported audio persistence, search filtering, and storage/media failure handling. Run unit tests and assembleDebug. On an emulator or device, verify audio playback, seeking, notification controls, lock-screen playback, audio interruptions, process recreation, import cancellation, and navigation continuity. Inspect small and large phone layouts, font scaling, and landscape for overflow and usable controls. Without an emulator/device, report runtime verification as incomplete.

Success means AMI installs on Android, plays audio, supports all specified navigation, and retains the user's local library after app restart. No fake sign-in, nonfunctional payment features, simulated playback, or unsupported claims about Spotify access. Report device checks that could not be executed. The REA dependency is satisfied only by an actual documented run with retained evidence, not by merely linking its repository.

## Review boundary

This specification records the approved concept and makes the implementation contract reviewable. After approval, prepare the implementation plan and select execution according to the required development workflow. The remaining immediate blocker to REA use is obtaining its runnable tooling and a compatible reference target in this environment.
