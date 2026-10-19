package com.ami.music.data

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CatalogException(message: String) : IOException(message)
data class CatalogPage(val tracks: List<Track>, val nextOffset: Int, val hasMore: Boolean)
data class CatalogUiState(
    val query: String = "", val tracks: List<Track> = emptyList(), val loading: Boolean = false,
    val error: String? = null, val nextOffset: Int = 0, val hasMore: Boolean = false, val loaded: Boolean = false,
)

/** Anonymous public catalog only. No accounts, gated streams, downloads, or persisted signed URLs. */
class AudiusCatalog(
    private val client: OkHttpClient = MusicHttp.client,
    private val baseUrl: HttpUrl = "https://api.audius.co/v1/".toHttpUrl(),
) {
    suspend fun load(query: String, offset: Int): CatalogPage {
        val url = baseUrl.newBuilder().addPathSegment("tracks").addPathSegment(if (query.isBlank()) "trending" else "search")
            .addQueryParameter("app_name", "AMI").addQueryParameter("limit", "20")
            .addQueryParameter("offset", offset.coerceAtLeast(0).toString())
        if (query.isNotBlank()) url.addQueryParameter("query", query.trim().take(200))
        val bytes = MusicHttp.get(client, url.build(), 2 * 1024 * 1024)
        return parsePage(bytes.toString(Charsets.UTF_8), offset, 20)
    }
    companion object {
        fun parsePage(json: String, offset: Int, limit: Int): CatalogPage {
            try {
                val array = JSONObject(json).optJSONArray("data") ?: throw CatalogException("Audius returned an unexpected response. Please retry.")
                val tracks = (0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.let(::parseTrack) }.distinctBy { it.id }
                return CatalogPage(tracks, offset + array.length(), array.length() >= limit)
            } catch (error: CatalogException) { throw error }
            catch (_: Exception) { throw CatalogException("Audius returned an unreadable response. Please retry.") }
        }
        private fun parseTrack(row: JSONObject): Track? {
            val id = row.optString("id").takeIf { it.matches(Regex("[A-Za-z0-9]{1,64}")) } ?: return null
            if (!row.optBoolean("is_streamable", false) || row.optBoolean("is_stream_gated") || row.optBoolean("is_unlisted") || row.optBoolean("is_delete")) return null
            if (!row.isNull("stream_conditions") || row.optJSONArray("allowed_api_keys")?.length()?.let { it > 0 } == true) return null
            if (row.has("is_available") && !row.optBoolean("is_available")) return null
            val access = row.optJSONObject("access")
            if (access != null && !access.optBoolean("stream", false)) return null
            val user = row.optJSONObject("user") ?: return null
            if (user.optBoolean("is_deactivated")) return null
            val title = row.text("title") ?: return null
            val artist = user.text("name") ?: user.text("handle") ?: return null
            val userId = user.text("id") ?: return null
            val seconds = row.optLong("duration", 0).takeIf { it in 1..604800 } ?: return null
            val art = row.optJSONObject("artwork")
            val cover = listOf("480x480", "150x150", "1000x1000").firstNotNullOfOrNull { key -> art?.text(key)?.let(::safeRemoteImage) }
            val link = row.text("permalink")?.takeIf { it.startsWith("/") && !it.startsWith("//") }?.let { "https://audius.co$it" }
                ?: "https://audius.co/tracks/$id"
            val artistUrl = user.text("handle")?.let { "https://audius.co/".toHttpUrl().newBuilder().addPathSegment(it).build().toString().trimEnd('/') }
            val album = row.optJSONObject("album_backlink")
            val albumName = album?.text("playlist_name") ?: title
            val albumId = album?.text("playlist_id") ?: "single-$id"
            return Track("audius-$id", title, "audius-artist-$userId", artist, "audius-album-$albumId", albumName,
                "https://api.audius.co/v1/tracks/$id/stream?app_name=AMI", seconds * 1000,
                artworkUri = cover, genre = row.text("genre"), year = row.text("release_date")?.take(4)?.takeIf { it.all(Char::isDigit) },
                mimeType = "audio/mpeg", metadataVersion = 1, provider = "audius", externalUrl = link, artistUrl = artistUrl)
        }
        private fun JSONObject.text(key: String) = if (isNull(key)) null else optString(key).trim().takeIf { it.isNotEmpty() && it != "null" }
    }
}

/** Covers are supplied by Audius nodes. Reject non-HTTPS/local addresses before opening an image. */
fun safeRemoteImage(value: String): String? = runCatching {
    val url = value.toHttpUrl()
    val host = url.host.lowercase()
    value.takeIf { url.isHttps && url.username.isEmpty() && url.password.isEmpty() && host.contains('.') &&
        !host.endsWith(".local") && !host.endsWith(".localhost") && !host.all { it.isDigit() || it == '.' } && !host.contains(':') }
}.getOrNull()

object MusicHttp {
    val client = OkHttpClient.Builder().connectTimeout(12, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS).followSslRedirects(false).build()
    suspend fun get(client: OkHttpClient, url: HttpUrl, maxBytes: Int): ByteArray = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(Request.Builder().url(url).header("User-Agent", "AMI/0.3.0").build())
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) {
                if (continuation.isActive) continuation.resumeWithException(CatalogException("Cannot reach Audius. Check your connection and try again."))
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val bytes = response.use {
                        if (it.code == 429) throw CatalogException("Audius is busy. Try again in ${it.header("Retry-After")?.toIntOrNull()?.coerceIn(1, 3600) ?: 30} seconds.")
                        if (it.code in listOf(401, 403)) throw CatalogException("Audius is not allowing this request right now. Your local music is still available.")
                        if (!it.isSuccessful) throw CatalogException("Audius is temporarily unavailable. Please retry.")
                        val body = it.body ?: throw CatalogException("Audius sent an empty response.")
                        if (body.contentLength() > maxBytes) throw CatalogException("The response was too large. Try another search.")
                        body.byteStream().use { input ->
                            val output = ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                if (output.size() + count > maxBytes) throw CatalogException("The response was too large. Try another search.")
                                output.write(buffer, 0, count)
                            }
                            output.toByteArray()
                        }
                    }
                    if (continuation.isActive) continuation.resume(bytes)
                } catch (error: Exception) {
                    if (continuation.isActive) continuation.resumeWithException(if (error is CatalogException) error else CatalogException("Connection interrupted. Please retry."))
                }
            }
        })
    }
}
