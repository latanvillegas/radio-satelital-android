package com.app.radiosatelital.data.artwork

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ArtworkState(
    val artworkUrl: String? = null,
    val artistImageUrl: String? = null,
)

interface ArtworkDataSource {
    suspend fun resolveArtwork(artist: String, songTitle: String): ArtworkState?
}

class ItunesArtworkDataSource : ArtworkDataSource {
    override suspend fun resolveArtwork(artist: String, songTitle: String): ArtworkState? = withContext(Dispatchers.IO) {
        runCatching {
            val term = "${artist.trim()} ${songTitle.trim()}".replace(" ", "+")
            val endpoint = "https://itunes.apple.com/search?term=$term&entity=song&limit=1"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "RadioSatelital/1.0")
            }
            val payload = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val json = JSONObject(payload)
            if (json.optInt("resultCount", 0) <= 0) return@runCatching null
            val first = json.optJSONArray("results")?.optJSONObject(0) ?: return@runCatching null
            val artwork = first.optString("artworkUrl100").takeIf { it.isNotBlank() }
            val artistImage = first.optString("artistViewUrl").takeIf { it.isNotBlank() }
            ArtworkState(artworkUrl = artwork, artistImageUrl = artistImage)
        }.getOrNull()
    }
}

class ArtworkRepository(
    private val dataSource: ArtworkDataSource = ItunesArtworkDataSource(),
) {
    suspend fun fetchArtworkIfPossible(artist: String?, songTitle: String?): ArtworkState {
        if (artist.isNullOrBlank() || songTitle.isNullOrBlank()) return ArtworkState()
        return dataSource.resolveArtwork(artist, songTitle) ?: ArtworkState()
    }
}
