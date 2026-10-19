package com.ami.music

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ami.music.data.AmiDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @Test fun versionOneLibraryKeepsSongsAndFavorites() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-${System.nanoTime()}.db"
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        try {
            SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
                db.execSQL("CREATE TABLE tracks (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, artistId TEXT NOT NULL, artist TEXT NOT NULL, albumId TEXT NOT NULL, album TEXT NOT NULL, sourceUri TEXT NOT NULL, durationMs INTEGER NOT NULL, artwork INTEGER NOT NULL, imported INTEGER NOT NULL, fingerprint TEXT, addedAt INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX index_tracks_albumId ON tracks(albumId)")
                db.execSQL("CREATE INDEX index_tracks_artistId ON tracks(artistId)")
                db.execSQL("CREATE TABLE playlists (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE playlist_entries (playlistId TEXT NOT NULL, trackId TEXT NOT NULL, position INTEGER NOT NULL, PRIMARY KEY(playlistId,trackId), FOREIGN KEY(playlistId) REFERENCES playlists(id) ON DELETE CASCADE, FOREIGN KEY(trackId) REFERENCES tracks(id) ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX index_playlist_entries_trackId ON playlist_entries(trackId)")
                db.execSQL("CREATE UNIQUE INDEX index_playlist_entries_playlistId_position ON playlist_entries(playlistId,position)")
                db.execSQL("CREATE TABLE likes (trackId TEXT NOT NULL PRIMARY KEY, FOREIGN KEY(trackId) REFERENCES tracks(id) ON DELETE CASCADE)")
                db.execSQL("CREATE TABLE history (trackId TEXT NOT NULL PRIMARY KEY, playedAt INTEGER NOT NULL, FOREIGN KEY(trackId) REFERENCES tracks(id) ON DELETE CASCADE)")
                db.execSQL("INSERT INTO tracks VALUES ('saved','Saved song','a','Artist','b','Album','file:///saved.mp3',4000,0,1,NULL,1)")
                db.execSQL("INSERT INTO likes VALUES ('saved')")
                db.execSQL("INSERT INTO playlists VALUES ('p','Favorites',1,1)")
                db.execSQL("INSERT INTO playlist_entries VALUES ('p','saved',0)")
                db.version = 1
            }
            val migrated = Room.databaseBuilder(context, AmiDatabase::class.java, name).addMigrations(AmiDatabase.MIGRATION_1_2).build()
            try {
                val track = migrated.libraryDao().track("saved")!!
                assertEquals("Saved song", track.title)
                assertTrue(track.available)
                assertFalse(track.device)
                assertEquals("saved", migrated.libraryDao().playlistEntries("p").single().trackId)
                migrated.openHelper.readableDatabase.query("SELECT COUNT(*) FROM likes").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
            } finally { migrated.close() }
        } finally { context.deleteDatabase(name) }
    }
}
