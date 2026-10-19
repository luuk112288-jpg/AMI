package com.ami.music.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Track::class, Playlist::class, PlaylistEntry::class, Like::class, History::class], version = 2, exportSchema = true)
abstract class AmiDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                listOf("artworkUri", "albumArtist", "genre", "year", "trackNumber", "fileName", "mimeType", "bitrate").forEach {
                    db.execSQL("ALTER TABLE tracks ADD COLUMN $it TEXT")
                }
                db.execSQL("ALTER TABLE tracks ADD COLUMN device INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tracks ADD COLUMN available INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE tracks ADD COLUMN sizeBytes INTEGER")
            }
        }
    }
}
