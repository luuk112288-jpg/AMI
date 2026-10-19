package com.ami.music

import android.app.Application
import androidx.room.Room
import com.ami.music.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class AmiApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val playbackSnapshots = Channel<SavedPlayback>(Channel.CONFLATED)
    val database by lazy { Room.databaseBuilder(this, AmiDatabase::class.java, "ami-library.db").build() }
    val repository by lazy { LibraryRepository(database) }
    val preferences by lazy { PlaybackPreferences(this) }
    val importer by lazy { AudioImporter(this, repository) }
    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            for (snapshot in playbackSnapshots) runCatching { preferences.save(snapshot) }
        }
    }
}
