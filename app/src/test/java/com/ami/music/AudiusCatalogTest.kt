package com.ami.music

import com.ami.music.data.AudiusCatalog
import com.ami.music.data.CatalogException
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class AudiusCatalogTest {
    private fun song(id: String = "abc12", extra: String = "") = """{
      "id":"$id","title":"A real song","duration":123,"is_streamable":true,
      "is_stream_gated":false,"is_unlisted":false,"access":{"stream":true},
      "permalink":"/artist/a-real-song","genre":"Electronic",
      "user":{"id":"user1","name":"Real Artist","handle":"artist"},
      "artwork":{"480x480":"https://creatornode.audius.co/cover.jpg"}$extra
    }"""
    @Test fun mapsPublicTrackToStableStreamAndAttribution() {
        val result = AudiusCatalog.parsePage("""{"data":[${song()}]}""", 0, 20)
        val track = result.tracks.single()
        assertEquals("audius-abc12", track.id)
        assertEquals("Real Artist", track.artist)
        assertEquals(123000L, track.durationMs)
        assertEquals("audius", track.provider)
        assertEquals("https://audius.co/artist/a-real-song", track.externalUrl)
        assertEquals("https://audius.co/artist", track.artistUrl)
        assertEquals("https://creatornode.audius.co/cover.jpg", track.artworkUri)
        assertEquals("https://api.audius.co/v1/tracks/abc12/stream?app_name=AMI", track.sourceUri)
        assertFalse(track.imported)
        assertFalse(track.device)
    }
    @Test fun excludesRestrictedDeletedUnlistedAndMalformedTracks() {
        val gated = song("gated").replace("\"is_stream_gated\":false", "\"is_stream_gated\":true")
        val unavailable = song("missing").replace("\"is_streamable\":true", "\"is_streamable\":false")
        val denied = song("denied").replace("\"stream\":true", "\"stream\":false")
        val unlisted = song("private").replace("\"is_unlisted\":false", "\"is_unlisted\":true")
        val restricted = song("restricted", ",\"stream_conditions\":{\"follow_user_id\":1}")
        val deleted = song("deleted", ",\"is_delete\":true")
        val missingPermission = song("unknown").replace(",\"is_streamable\":true", "")
        val page = AudiusCatalog.parsePage("""{"data":[${song()},$gated,$unavailable,$denied,$unlisted,$restricted,$deleted,$missingPermission,{}]}""", 0, 20)
        assertEquals(listOf("audius-abc12"), page.tracks.map { it.id })
    }
    @Test fun paginationUsesRawCountEvenWhenGatedTracksAreFilteredOut() {
        val restricted = song("gate").replace("\"is_stream_gated\":false", "\"is_stream_gated\":true")
        val page = AudiusCatalog.parsePage("""{"data":[$restricted,$restricted]}""", 20, 2)
        assertTrue(page.tracks.isEmpty())
        assertEquals(22, page.nextOffset)
        assertTrue(page.hasMore)
    }
    @Test fun invalidAndUnsafeArtworkNeverBecomesAnImageRequest() {
        val raw = song().replace("https://creatornode.audius.co/cover.jpg", "http://127.0.0.1/private")
        assertNull(AudiusCatalog.parsePage("""{"data":[$raw]}""", 0, 20).tracks.single().artworkUri)
    }
    @Test fun emptyDataIsValidButMalformedResponseIsAnError() {
        assertTrue(AudiusCatalog.parsePage("""{"data":[]}""", 0, 20).tracks.isEmpty())
        try { AudiusCatalog.parsePage("{}", 0, 20); fail("Missing data must not look like no results") }
        catch (_: CatalogException) { }
    }
    @Test fun searchEncodesInputAndUsesPagination() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"data":[${song()}]}"""))
            val catalog = AudiusCatalog(baseUrl = server.url("/v1/"))
            val result = catalog.load("rock & roll", 20)
            val request = server.takeRequest()
            assertEquals("rock & roll", request.requestUrl!!.queryParameter("query"))
            assertEquals("20", request.requestUrl!!.queryParameter("offset"))
            assertEquals("AMI", request.requestUrl!!.queryParameter("app_name"))
            assertEquals("audius-abc12", result.tracks.single().id)
        }
    }
    @Test fun rateLimitAndServerErrorsAreNotEmptySuccess() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "15"))
            try { AudiusCatalog(baseUrl = server.url("/v1/")).load("", 0); fail("Rate limit must be shown") }
            catch (error: CatalogException) { assertTrue(error.message!!.contains("15")) }
        }
    }
}
