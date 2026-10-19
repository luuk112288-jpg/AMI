package com.ami.music.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Track::class, Playlist::class, PlaylistEntry::class, Like::class, History::class], version = 1, exportSchema = true)
abstract class AmiDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}
