package com.ami.music.data

object DemoCatalog {
    fun tracks(): List<Track> {
        val titles = listOf("Midnight Current", "Soft Focus", "Afterglow", "Blue Hour", "Velvet Sky", "Slow Motion", "First Light", "Coastline")
        val artists = listOf("Luma", "Northline", "Sora", "Velvet Hours")
        val albums = listOf("Midnight Current", "Soft Focus", "Afterglow", "Blue Hour")
        return titles.mapIndexed { index, title ->
            val group = index % 4
            Track("demo-$index", title, "artist-$group", artists[group], "album-$group", albums[group],
                "asset:///audio/demo-$index.wav", 40_000, group, addedAt = 1_000L - index)
        }
    }
}
