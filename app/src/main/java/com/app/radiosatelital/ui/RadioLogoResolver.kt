package com.app.radiosatelital.ui

import com.app.radiosatelital.RadioStation
import java.net.URI

fun RadioStation.resolvedLogoUrl(): String? {
    val manualLogo = VERIFIED_STATION_LOGOS_BY_STREAM_URL[url]
    return sanitizeLogoUrl(manualLogo)
}

fun RadioStation.fallbackSongInfo(): String {
    val genreText = genre.takeIf { it.isNotBlank() }
    return if (genreText != null) "$locationLabel · $genreText" else locationLabel
}

private fun sanitizeLogoUrl(rawUrl: String?): String? {
    val trimmed = rawUrl?.trim()?.takeIf { it.isNotBlank() } ?: return null
    val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase() ?: return null
    if (scheme != "http" && scheme != "https") return null
    val host = uri.host?.trim()?.takeIf { it.isNotBlank() } ?: return null
    if (host == "localhost" || host == "127.0.0.1") return null
    return uri.toString()
}

private val VERIFIED_STATION_LOGOS_BY_STREAM_URL: Map<String, String> = emptyMap()
