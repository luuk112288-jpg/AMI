# AMI Android Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Build an installable Android music app named AMI using REA investigation, real playback, local imports, and persistent libraries.

**Architecture:** Kotlin and Compose present repository state through ViewModels and StateFlow. A Media3 MediaSessionService owns playback independently of navigation. Room stores catalog/library data, DataStore stores preferences, and imported files live in private app storage.

**Tech Stack:** Kotlin, Jetpack Compose, Navigation Compose, AndroidX Media3, Room, DataStore, Gradle Kotlin DSL, JUnit, Android instrumentation tests.

**Spec:** AMI-design.md (alongside this file).

## Global Constraints

- Application ID: com.ami.music. App label: AMI.
- Minimum Android API 26.
- Android is the primary deliverable; a companion website is deferred.
- No cross-device synchronization or account system is promised by this scope.
- Use a Media3 ExoPlayer owned by a MediaSessionService, with UI connected through MediaController.
- No broad storage permissions, embedded credentials, fake playback, or claims of Spotify catalog access.
- REA use requires a documented executable run with retained evidence.
- An APK is complete only after a successful build; device behavior is verified only by device/emulator checks.

## Review Focus

1. Document picker cancellation, malformed audio, and partial copies must not create broken library rows (Task 3).
2. Process recreation must restore the library and position without automatically starting sound (Tasks 2 and 4).
3. Audio interruptions and headphone disconnect must respect platform audio policy (Task 4).
4. Removing the current queue item or emptying a playlist must leave a valid player state (Tasks 4 and 5).
5. Large font settings, landscape, and TalkBack must leave navigation and player controls usable (Task 5).

## File structure

Project root: work/ami-android. Build configuration: settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml, gradle/wrapper/*, app/build.gradle.kts. Namespace sources: app/src/main/java/com/ami/music/.

Sources: MainActivity.kt; data/Models.kt, AmiDatabase.kt, LibraryDao.kt, LibraryRepository.kt, AudioImporter.kt, PlaybackPreferences.kt; playback/PlaybackPolicy.kt, AmiPlaybackService.kt, PlaybackConnection.kt; ui/AmiApp.kt, AmiViewModel.kt, theme/Theme.kt, components/Artwork.kt, TrackRow.kt, MiniPlayer.kt; ui/screens/HomeScreen.kt, SearchScreen.kt, LibraryScreen.kt, CollectionScreen.kt, NowPlayingScreen.kt, QueueScreen.kt. Resources: app/src/main/res/raw, drawable, values and AndroidManifest.xml. Evidence and handoff: docs/rea-investigation.md, README.md, ATTRIBUTION.md.

## Task 1: Establish REA evidence and an Android build

**Files:** Build configuration, app/src/main/AndroidManifest.xml, MainActivity.kt, docs/rea-investigation.md, README.md.

**Interfaces:** Produces a reproducible project and a provenance record containing upstream revision/package version, exact commands, target identity, observations, and explicit limitations.

- [ ] Inspect existing JDK/SDK locations and dependency caches; check shell DNS and package registry availability. Resolve a compatible stable Android toolchain using official release documentation and pin exact versions in the version catalog and wrapper.
- [ ] Obtain REA through the linked official repository or its official package. Read its applicable local instructions before use. Run CLI help/readiness, then a supported analysis of an accessible reference target. Save the actual output and evidence metadata. If target or tool access is unavailable, record the precise blocker and do not claim the REA requirement is fulfilled.
- [ ] Scaffold the Android application with minSdk 26, applicationId com.ami.music, label AMI, and a dark Compose activity. Create wrapper files through a trusted Gradle distribution rather than inventing a wrapper JAR.
- [ ] Run `gradlew.bat :app:assembleDebug` on Windows. Expected: BUILD SUCCESSFUL and an actual app-debug.apk. Record setup failures explicitly; do not describe uncompiled source as an installable app.
- [ ] Initialize project Git if needed and commit the reproducible scaffold and evidence; exclude local SDK paths, private captures, binaries, secrets, and signing keys.

## Task 2: Persistent catalog and library

**Files:** data/Models.kt, AmiDatabase.kt, LibraryDao.kt, LibraryRepository.kt, PlaybackPreferences.kt; app/src/test and app/src/androidTest equivalents.

**Interfaces:** Track(id, title, artistId, albumId, sourceUri, durationMs); Album and Artist with stable IDs; Playlist(id, name, createdAt, updatedAt); PlaylistEntry(playlistId, trackId, position). LibraryRepository exposes observeTracks(): Flow<List<Track>>, observePlaylists(): Flow<List<Playlist>>, suspend setLiked(trackId: String, liked: Boolean), createPlaylist(name: String): String, renamePlaylist(id: String, name: String), deletePlaylist(id: String), setPlaylistTracks(id: String, trackIds: List<String>), search(query: String): SearchResults. Mutation methods are suspend functions. Define SearchResults with tracks, albums, artists, playlists.

- [ ] Write Room instrumentation tests: createPlaylistPersistsAfterReopen; playlistOrderSurvivesEdit; deletePlaylistKeepsAudio; likesPersist; searchMatchesCaseInsensitively; blankPlaylistNameRejected. Assert missing track references never appear in playable results and playlist ordering is transactional.
- [ ] Run the targeted tests on an available device; confirm missing implementation fails before writing production code. If no device is available, retain the unexecuted status and run pure validation tests where possible.
- [ ] Implement entities, indexed DAO queries, transactions, repository methods, and versioned DataStore preferences. Seed original/licensed demo content once without duplicating it on restart. Store artwork and audio attribution.
- [ ] Run unit and database tests; verify reopening preserves likes, order, and playlist names. Commit.

## Task 3: Import playable phone audio

**Files:** data/AudioImporter.kt, related repository methods, document-picker integration; tests and fixtures.

**Interfaces:** suspend AudioImporter.importAudio(uri: Uri): ImportResult, where ImportResult is Success(trackId), Duplicate(trackId), or Failure(message). Picker cancellation bypasses import. Repository additions commit only after successful copying and metadata validation.

- [ ] Write tests: cancelledPickerDoesNotMutate; invalidAudioCreatesNoTrack; copyFailureRemovesPartialFile; duplicateImportDoesNotDuplicate; validImportSurvivesProviderRevocation. Use a small known-valid WAV fixture and an invalid byte fixture.
- [ ] Run these tests to establish failures.
- [ ] Implement ACTION_OPEN_DOCUMENT audio selection, bounded streamed copying on Dispatchers.IO, metadata extraction, stable duplicate identity, temporary-file cleanup, and commit to private app storage. Report empty/unreadable files and insufficient storage. Release metadata readers and streams on every exit path.
- [ ] Run tests and import a file through the system picker on a device. Confirm playback after restart and without the original provider URI. Commit.

## Task 4: Service-owned playback

**Files:** playback/PlaybackPolicy.kt, AmiPlaybackService.kt, PlaybackConnection.kt; manifest; unit and instrumentation tests.

**Interfaces:** PlaybackConnection exposes state: StateFlow<PlayerUiState>, playTracks(tracks: List<Track>, startIndex: Int), togglePlay(), next(), previous(), seekTo(positionMs: Long), setVolume(volume: Float), setShuffle(enabled: Boolean), setRepeat(mode: RepeatMode), moveQueueItem(from: Int, to: Int), removeQueueItem(index: Int). PlayerUiState includes current track, queue, index, elapsed/duration, buffering, error, playing, shuffle, repeat, volume.

- [ ] Write policy tests: previousAfterThreeSecondsRestarts; previousAtThreeSecondsReturnsPrevious; manualNextIgnoresRepeatOne; shuffleRetainsCurrentTrack; disablingShuffleRestoresOrder; emptyQueueStops; removingCurrentSelectsValidSuccessor; restoredPlayerDoesNotAutoplay.
- [ ] Run unit tests and confirm failures. Implement pure policy helpers and map commands into Media3; use its authoritative timeline rather than a second independent queue.
- [ ] Implement MediaSessionService, media metadata, foreground-service declarations, MediaController lifecycle, notification actions, audio focus, becoming-noisy handling, persisted restoration, and actionable playback errors. Release sessions/controllers correctly.
- [ ] Run policy tests. On device verify background/lock-screen playback, notification actions, incoming audio-focus interruption, disconnected headphones, repeat and shuffle, queue deletion, process restoration, and seek bounds. Commit.

## Task 5: Complete mobile UI

**Files:** ui files listed above, artwork resources, Compose UI tests.

**Interfaces:** AmiViewModel combines LibraryRepository and PlaybackConnection. AmiApp routes Home, Search, Library, Album(id), Artist(id), Playlist(id), NowPlaying, Queue. Screens receive immutable state and callbacks; none constructs another player.

- [ ] Write Compose tests: homeToAlbumStartsSelectedTrack; searchFiltersEntities; createRenameDeletePlaylist; likeUpdatesLibrary; addRemoveReorderPlaylistTracks; importCancellationReturnsToLibrary; backClosesPlayerBeforeLeavingApp. Assert empty states have working next actions.
- [ ] Run tests to establish failures, then implement the dark AMI theme, artwork cards, collections, search filters, library editing dialogs, import action, mini player, full player, queue ordering, volume/mute, seeking, shuffle/repeat, and clear loading/error states.
- [ ] Verify each prominent action changes actual data or playback. Check TalkBack labels, 48dp touch targets, system insets, portrait/landscape, and 200% font settings without clipped essential controls.
- [ ] Run unit and Compose tests; inspect small and large phone screens and verify playback continues across navigation and rotation. Commit.

## Task 6: Verify and deliver

**Files:** README.md, ATTRIBUTION.md, docs/rea-investigation.md; final source archive and APK if built.

**Interfaces:** Source archive with build instructions; debug APK labeled with version, package name, SHA-256, and test status. No release/store-publication claim.

- [ ] Run `gradlew.bat :app:testDebugUnitTest :app:assembleDebug`. Expected: BUILD SUCCESSFUL. Run `gradlew.bat :app:connectedDebugAndroidTest` when an emulator/device exists; distinguish skipped checks from passing checks.
- [ ] Perform the acceptance flows from the specification on a device, including offline demo audio, import persistence, library persistence, background playback, queue modes, and accessibility.
- [ ] Review the source for secrets, placeholder actions, missing attributions, and claimed REA evidence that does not exist. Resolve defects and repeat only affected checks.
- [ ] Package the exact validated source and APK, save deliverables, and provide download links. If dependencies or SDK access prevent compilation, deliver source with the precise build blocker and explicitly state that no verified APK is available.

## Self-review

The plan covers approved music features, native Android delivery, REA evidence, local-only persistence, original/licensed demo assets, playback lifecycle, failure handling, accessibility, and honest verification limits. Android replaces web-specific storage and deployment; there is no concurrent Sites publication task. Implementation remains pending plan review and execution-method selection.
