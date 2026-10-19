# Audius integration — AMI 0.3.0

User-selected direction: add the best practical online music source alongside local music, without depending on Spotify playback or advertisements.

## Delivered behavior

Search has On device and Online views. The online view loads public Audius trending tracks, searches with debouncing/cancellation and supports offset pagination. Track rows, player and source menus retain attribution. Public online tracks use the existing Media3 player, queue, background controls, likes and mixed playlists.

Online queue metadata is cached in Room; it only enters the visible library after being played, liked or added to a playlist. The source URI is the stable public API stream endpoint, so temporary signed content URLs are not persisted. Audio is streamed, not downloaded. Local imports/scanning continue to work independently.

## API and access boundaries

- Base: https://api.audius.co/v1
- GET /tracks/trending and /tracks/search; app_name=AMI, limit=20, offset, encoded query.
- GET /tracks/{id}/stream?app_name=AMI for playback. The service enforces current track access on each stream request.
- The parser requires is_streamable=true and excludes stream-gated, unlisted, deleted, unavailable, API-key restricted and explicitly denied tracks. Missing required identity/title/artist/duration is excluded. Download eligibility does not imply streaming eligibility.
- Artwork requests accept HTTPS URLs, use bounded response sizes, sampled bitmaps, a bounded in-memory cache and limited concurrency.
- Requests are anonymous and have timeouts/cancellation; rate limits and server/network errors are shown separately from empty results.
- Online searches, artwork requests and playback connect to Audius/content nodes and use network data. Local audio is not uploaded. No Spotify SDK, Spotify login or ad SDK is used.

Audius has its own catalog. Tracks may disappear or become restricted; AMI does not bypass gates or offer protected content.

## Verification

Source: fc6ff02e4e05a6ab0baaebeb339977e7b97eab37.
Build: https://github.com/luuk112288-jpg/AMI/actions/runs/39279442544

- APK build and JVM unit tests passed.
- All 25 Android instrumentation tests passed, including real Audius playback reaching ExoPlayer isPlaying, local MediaStore playback, mixed-library persistence, v1→v2→v3 migration and saved Online-tab restoration.
- JVM tests verify metadata/source mapping, exclusion of restricted tracks, pagination across filtered results, bad responses, URL encoding and rate-limit errors.
- A separate public API probe returned real trending/search results and opened an audio/mpeg stream. This is a point-in-time compatibility check, not an uptime guarantee.
- Independent code review found a restored-tab loading issue; it was fixed and a restoration regression test passed.
- The initial new Online-navigation test failed against the prior app before implementation.

Artifact ZIP SHA-256: 5f11431398399e2639551f7829ba0d6e62551e6df86f0b7e67ba6eedd1302f51.
Release publishes that exact APK after checking its package, version 0.3.0/versionCode 3, build/unit steps and successful device job.

Remaining checks: physical devices/older Android versions, long sessions and spotty connectivity, large catalog paging, TalkBack/large fonts. Production signing still needs a persistent private key. REA investigation remains unexecuted.

## Primary references

- https://docs.audius.co/
- https://docs.audius.co/api/
- https://github.com/AudiusProject/api/blob/main/api/v1_track_stream.go
- https://github.com/AudiusProject/api/blob/main/api/dbv1/tracks.go
