package com.ami.music.ui

import android.net.Uri
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.ami.music.data.*
import com.ami.music.ui.components.*
import com.ami.music.ui.screens.*
import com.ami.music.ui.theme.*

@Composable fun AmiApp(vm: AmiViewModel) {
    val library by vm.library.collectAsStateWithLifecycle()
    val player by vm.player.collectAsStateWithLifecycle()
    val importing by vm.importing.collectAsStateWithLifecycle()
    val scanning by vm.scanning.collectAsStateWithLifecycle()
    val scanMessage by vm.scanMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.scanPhone() else vm.musicPermissionDenied()
    }
    val scanPhone: () -> Unit = { if (DeviceMusicScanner.hasPermission(context)) vm.scanPhone() else permissionRequest.launch(DeviceMusicScanner.permission()) }
    DisposableEffect(lifecycle, vm) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME && DeviceMusicScanner.hasPermission(context)) vm.scanPhone() }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && DeviceMusicScanner.hasPermission(context)) vm.scanPhone()
        onDispose { lifecycle.removeObserver(observer) }
    }
    val ready by vm.ready.collectAsStateWithLifecycle()
    val startupError by vm.startupError.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"
    val snackbar = remember { SnackbarHostState() }
    var about by remember { mutableStateOf(false) }
    var create by remember { mutableStateOf(false) }
    var createFor by remember { mutableStateOf<Track?>(null) }
    var rename by remember { mutableStateOf<Playlist?>(null) }
    var delete by remember { mutableStateOf<Playlist?>(null) }
    var add by remember { mutableStateOf<Track?>(null) }
    var importPrompt by remember { mutableStateOf(false) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { vm.importAudio(it) }
    LaunchedEffect(vm) { vm.messages.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(player.error) { player.error?.let { snackbar.showSnackbar(it) } }
    val collection: (String, String) -> Unit = { kind, id -> nav.navigate("collection/$kind/${Uri.encode(id)}") { launchSingleTop = true } }
    val goSearch: () -> Unit = { nav.navigate("search") { launchSingleTop = true } }
    val back: () -> Unit = { nav.popBackStack(); Unit }
    val row: @Composable (Track, List<Track>, Int) -> Unit = { track, queue, index ->
        val currentPlaylist = if (entry?.arguments?.getString("kind") == "playlist") entry?.arguments?.getString("id") else null
        TrackRow(track, player.current?.id == track.id, track.id in library.likedIds,
            onPlay = { vm.playback.playTracks(queue, index) }, onLike = { vm.like(track) }, onAdd = { add = track },
            onAlbum = { collection("album", track.albumId) }, onArtist = { collection("artist", track.artistId) },
            onRemove = currentPlaylist?.let { id -> { vm.removeFromPlaylist(id, track); Unit } })
    }
    Scaffold(
        containerColor = Ink,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (ready && route != "player") Column {
                MiniPlayer(player, { nav.navigate("player") { launchSingleTop = true } }, vm.playback::togglePlay, vm.playback::next)
                NavigationBar(containerColor = Ink) {
                    listOf(Triple("home", "Home", Icons.Default.Home), Triple("search", "Search", Icons.Default.Search), Triple("library", "Your library", Icons.Default.LibraryMusic)).forEach { (target, label, icon) ->
                        NavigationBarItem(selected = route == target, onClick = {
                            nav.navigate(target) { popUpTo(nav.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true }
                        }, icon = { Icon(icon, label) }, label = { Text(label) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Lime, selectedTextColor = Lime, indicatorColor = Panel, unselectedIconColor = Muted, unselectedTextColor = Muted))
                    }
                }
            }
        },
    ) { padding ->
        if (!ready || library.error != null) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text("AMI", style = MaterialTheme.typography.headlineLarge, color = Lime)
                Spacer(Modifier.height(24.dp))
                if (startupError == null && library.error == null) CircularProgressIndicator()
                else {
                    Text(startupError ?: library.error ?: "Could not open your library.", Modifier.padding(24.dp))
                    Button(onClick = vm::initialize) { Text("Retry") }
                }
            }
        } else NavHost(navController = nav, startDestination = "home", modifier = Modifier.fillMaxSize().padding(padding)) {
            composable("home") { HomeScreen(library, player, collection, vm.playback::playTracks, row, { about = true }, goSearch) }
            composable("search") { SearchScreen(library, collection, row) }
            composable("library") { LibraryScreen(library, importing, collection, { create = true }, { importPrompt = true }, scanning, scanMessage, scanPhone, { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }, row) }
            composable("collection/{kind}/{id}", arguments = listOf(navArgument("kind") { type = NavType.StringType }, navArgument("id") { type = NavType.StringType })) { target ->
                val kind = target.arguments?.getString("kind") ?: "album"
                val id = target.arguments?.getString("id") ?: ""
                CollectionScreen(kind, id, library, player, back, vm.playback::playTracks, vm.playback::setShuffle,
                    { rename = it }, { delete = it }, goSearch,
                    { trackId, delta -> vm.movePlaylistTrack(id, trackId, delta) }, row)
            }
            composable("player") { NowPlayingScreen(player, library, vm.playback, back, { nav.navigate("queue") }, { vm.like(it) }, { add = it }, { collection("artist", it.artistId) }) }
            composable("queue") { QueueScreen(player, vm.playback, back) }
        }
    }
    if (about) AlertDialog(onDismissRequest = { about = false }, title = { Text("Your little world of music.") }, text = {
        Text("AMI 0.2 — Artificial Music Intelligence\n\nA home for your favorite sounds. Listen to original demo tracks or bring your own audio.\n\nYour library stays on this device. Scan your phone for local music and album artwork. No account needed.\n\nAMI is an independent app. Demo artist names are fictional; the included music was composed for this project.")
    }, confirmButton = { TextButton(onClick = { about = false }) { Text("Keep listening") } })
    if (importPrompt) AlertDialog(onDismissRequest = { importPrompt = false }, title = { Text("Bring your music") }, text = {
        Text("Choose audio files from your phone (up to 512 MB each). AMI saves a private copy on this device so you can listen offline. Uninstalling the app removes those copies.")
    }, confirmButton = { TextButton(onClick = { importPrompt = false; importer.launch(arrayOf("audio/*")) }, enabled = !importing) { Text("Choose audio") } }, dismissButton = { TextButton(onClick = { importPrompt = false }) { Text("Cancel") } })
    if (create) NameDialog("New playlist", onDismiss = { create = false; createFor = null }, onSave = { name ->
        val targetTrack = createFor
        vm.createPlaylist(name) { id -> if (targetTrack != null) vm.addToPlaylist(id, targetTrack) else collection("playlist", id) }
    })
    rename?.let { playlist -> NameDialog("Rename playlist", playlist.name, { rename = null }, { vm.renamePlaylist(playlist.id, it) }) }
    delete?.let { playlist -> AlertDialog(onDismissRequest = { delete = null }, title = { Text("Delete ${playlist.name}?") }, text = { Text("This removes the playlist. Your audio files and liked songs stay in your library.") },
        confirmButton = { TextButton(onClick = { delete = null; vm.deletePlaylist(playlist.id, back) }) { Text("Delete", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { delete = null }) { Text("Cancel") } }) }
    add?.let { track -> AlertDialog(onDismissRequest = { add = null }, title = { Text("Add to playlist") }, text = {
        if (library.playlists.isEmpty()) Text("Make a new playlist for ${track.title}.") else LazyColumn(Modifier.heightIn(max = 320.dp)) {
            items(library.playlists, key = { it.id }) { playlist -> ListItem(headlineContent = { Text(playlist.name) }, leadingContent = { Icon(Icons.Default.PlaylistPlay, null) }, modifier = Modifier.clickable { vm.addToPlaylist(playlist.id, track); add = null }) }
        }
    }, confirmButton = { TextButton(onClick = { createFor = track; add = null; create = true }) { Text("New playlist") } }, dismissButton = { TextButton(onClick = { add = null }) { Text("Cancel") } }) }
}
