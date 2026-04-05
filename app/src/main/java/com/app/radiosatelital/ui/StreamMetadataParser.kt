package com.app.radiosatelital.ui

data class NowPlayingData(
    val artist: String?,
    val title: String?,
)

object StreamMetadataParser {

    fun parse(artistRaw: String?, titleRaw: String?, displayTitleRaw: String?): NowPlayingData {
        val artist = artistRaw?.trim().orEmpty().ifBlank { null }
        val title = titleRaw?.trim().orEmpty().ifBlank { null }
        val displayTitle = displayTitleRaw?.trim().orEmpty().ifBlank { null }

        // Streams often send "Artist - Song" in title/displayTitle.
        val combined = title ?: displayTitle
        if (artist == null && !combined.isNullOrBlank()) {
            val split = splitCombinedTitle(combined)
            if (split != null) return split
        }

        return NowPlayingData(
            artist = artist,
            title = combined,
        )
    }

    private fun splitCombinedTitle(value: String): NowPlayingData? {
        val delimiters = listOf(" - ", " – ", " — ", " | ")
        val delimiter = delimiters.firstOrNull { value.contains(it) } ?: return null
        val parts = value.split(delimiter, limit = 2).map { it.trim() }
        if (parts.size < 2 || parts[0].isBlank() || parts[1].isBlank()) return null
        return NowPlayingData(
            artist = parts[0],
            title = parts[1],
        )
    }
}
