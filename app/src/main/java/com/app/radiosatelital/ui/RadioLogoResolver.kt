package com.app.radiosatelital.ui

import com.app.radiosatelital.RadioStation
import java.net.URI

fun RadioStation.resolvedLogoUrl(): String? {
    if (!logoUrl.isNullOrBlank()) return logoUrl
    val host = runCatching { URI(url).host }.getOrNull()?.removePrefix("www.") ?: return null
    return "https://www.google.com/s2/favicons?sz=128&domain=$host"
}

fun RadioStation.fallbackSongInfo(): String {
    val genreText = genre.takeIf { it.isNotBlank() }
    return if (genreText != null) "$locationLabel · $genreText" else locationLabel
}
