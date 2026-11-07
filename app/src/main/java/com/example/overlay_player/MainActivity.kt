package com.example.overlay_player

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.graphics.SurfaceTexture
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Replay30
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.overlay_player.ui.theme.OverlayPlayerTheme
import java.io.File
import java.io.IOException
import java.util.Date

data class AudioFile(val uri: Uri, val name: String)

class MainActivity : ComponentActivity() {

    private lateinit var playlistStore: PlaylistStore
    private lateinit var updates: UpdateController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playlistStore = PlaylistStore(this)
        updates = UpdateController(applicationContext, lifecycleScope)
        enableEdgeToEdge()
        Handler(Looper.getMainLooper()).postDelayed(::writeDiagnostics, DIAGNOSTICS_DELAY_MS)
        setContent {
            OverlayPlayerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PlayerScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        AudioPlayerService.setAppVisible(true)
    }

    override fun onStop() {
        AudioPlayerService.setAppVisible(false)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        updates.resumePendingInstall()
        updates.check(automatic = true)
    }

    @Composable
    private fun PlayerScreen(modifier: Modifier = Modifier) {
        val audioFiles = remember { loadPlaylist().toMutableStateList() }
        var selectedIndex by remember {
            val saved = playlistStore.selectedUri
            mutableIntStateOf(
                audioFiles.indexOfFirst { it.uri.toString() == saved }
                    .takeIf { it >= 0 } ?: if (audioFiles.isEmpty()) -1 else 0
            )
        }
        val playback by AudioPlayerService.state.collectAsState()
        val isPlaying = playback.isPlaying
        val playingIndex = audioFiles.indexOfFirst { it.uri == playback.uri }
        var renaming by remember { mutableStateOf<AudioFile?>(null) }

        fun select(index: Int) {
            selectedIndex = index
            playlistStore.selectedUri = audioFiles.getOrNull(index)?.uri?.toString()
        }

        fun savePlaylist() {
            playlistStore.save(audioFiles.map { it.uri.toString() to it.name })
        }

        // Beim Wiederöffnen während der Wiedergabe steht die Auswahl auf der laufenden Datei.
        LaunchedEffect(playback.uri) {
            if (playingIndex >= 0) select(playingIndex)
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { /* permissions handled */ }

        val filePickerLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->
            for (uri in uris) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    e.printStackTrace()
                    Toast.makeText(
                        this,
                        "Diese Quelle erlaubt keinen dauerhaften Zugriff",
                        Toast.LENGTH_LONG
                    ).show()
                }
                val name = getFileName(uri)
                if (audioFiles.none { it.uri == uri }) {
                    audioFiles.add(AudioFile(uri, name))
                }
            }
            savePlaylist()
            if (selectedIndex == -1 && audioFiles.isNotEmpty()) {
                select(0)
            }
        }

        UpdateDialogs()

        renaming?.let { file ->
            RenameDialog(
                file = file,
                onDismiss = { renaming = null },
                onSave = { name ->
                    val index = audioFiles.indexOf(file)
                    if (index >= 0) {
                        audioFiles[index] = file.copy(name = name)
                        savePlaylist()
                        if (index == playingIndex) sendTitle(name)
                    }
                    renaming = null
                }
            )
        }

        val showVideo = playback.uri != null && playback.videoWidth > 0 && playback.videoHeight > 0

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showVideo) {
                VideoView(playback.videoWidth.toFloat() / playback.videoHeight)
            } else {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Overlay Player",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Audio und Video im Overlay abspielen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add files button
            Button(onClick = {
                requestPermissions(permissionLauncher)
                filePickerLauncher.launch(arrayOf("audio/*", "video/*"))
            }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Audio oder Video hinzufügen")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // File list
            if (audioFiles.isNotEmpty()) {
                Text(
                    text = "Playlist (${audioFiles.size} Dateien)",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    itemsIndexed(audioFiles) { index, audioFile ->
                        val isSelected = index == selectedIndex
                        val isCurrentlyPlaying = index == playingIndex && isPlaying

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { select(index) },
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isVideo = remember(audioFile.uri) { isVideo(audioFile.uri) }
                                Icon(
                                    imageVector = when {
                                        isCurrentlyPlaying -> Icons.Filled.PlayArrow
                                        isVideo -> Icons.Filled.Movie
                                        else -> Icons.Filled.MusicNote
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = if (isCurrentlyPlaying)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = audioFile.name,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                IconButton(
                                    onClick = { renaming = audioFile },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "Umbenennen",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (index == playingIndex) {
                                            sendAction(AudioPlayerService.ACTION_STOP)
                                        }
                                        audioFiles.removeAt(index)
                                        releaseFileAccess(audioFile.uri)
                                        savePlaylist()
                                        select(
                                            when {
                                                selectedIndex > index -> selectedIndex - 1
                                                selectedIndex >= audioFiles.size -> audioFiles.size - 1
                                                else -> selectedIndex
                                            }
                                        )
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Entfernen",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Now playing info
                if (playingIndex >= 0 && playingIndex < audioFiles.size) {
                    Text(
                        text = "Spielt: ${audioFiles[playingIndex].name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Player controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { sendAction(AudioPlayerService.ACTION_BACKWARD_30) }) {
                        Icon(Icons.Filled.Replay30, contentDescription = "30s zurück",
                            modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = { sendAction(AudioPlayerService.ACTION_BACKWARD_10) }) {
                        Icon(Icons.Filled.Replay10, contentDescription = "10s zurück",
                            modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                    FilledTonalButton(
                        onClick = {
                            if (selectedIndex < 0 || selectedIndex >= audioFiles.size) return@FilledTonalButton

                            val selected = audioFiles[selectedIndex]

                            if (isPlaying && playingIndex == selectedIndex) {
                                // Pause current
                                sendAction(AudioPlayerService.ACTION_PAUSE)
                            } else {
                                // Play selected file (new or different)
                                if (!checkOverlayPermission()) return@FilledTonalButton
                                startAudioService(selected.uri, selected.name)
                            }
                        },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            if (isPlaying && playingIndex == selectedIndex) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = { sendAction(AudioPlayerService.ACTION_FORWARD_10) }) {
                        Icon(Icons.Filled.Forward10, contentDescription = "10s vor",
                            modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = { sendAction(AudioPlayerService.ACTION_FORWARD_30) }) {
                        Icon(Icons.Filled.Forward30, contentDescription = "30s vor",
                            modifier = Modifier.size(36.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { sendAction(AudioPlayerService.ACTION_STOP) }) {
                        Icon(Icons.Filled.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop")
                    }
                    if (playback.uri != null && !playback.overlayVisible) {
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(onClick = {
                            if (checkOverlayPermission()) {
                                sendAction(AudioPlayerService.ACTION_SHOW_OVERLAY)
                            }
                        }) {
                            Text("Overlay zeigen")
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp))
                Text(
                    text = "Noch keine Dateien geladen.\nTippe oben auf den Button, um Audios oder Videos hinzuzufügen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            UpdateRow()
        }
    }

    /** Großes Bild in der App; beim Wechsel in eine andere App übernimmt das kleine Fenster. */
    @Composable
    private fun VideoView(aspect: Float) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { context ->
                    TextureView(context).apply {
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            private var surface: Surface? = null

                            override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
                                surface = Surface(texture).also { AudioPlayerService.setAppSurface(it) }
                            }

                            override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                                AudioPlayerService.setAppSurface(null)
                                surface?.release()
                                surface = null
                                return true
                            }

                            override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {}
                            override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}
                        }
                    }
                },
                modifier = Modifier
                    .aspectRatio(aspect, matchHeightConstraintsFirst = aspect < 1f)
                    .clip(RoundedCornerShape(12.dp))
            )
        }
    }

    @Composable
    private fun UpdateRow() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = updates.status.ifEmpty { "Version ${BuildConfig.VERSION_NAME}" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { updates.check(automatic = false) }) {
                Text("Auf Updates prüfen")
            }
        }
    }

    @Composable
    private fun UpdateDialogs() {
        updates.offer?.let { update ->
            AlertDialog(
                onDismissRequest = { updates.dismissOffer() },
                title = { Text("Update verfügbar · ${update.versionName}") },
                text = {
                    Text("${update.notes}\n\nJetzt von bums herunterladen und installieren?")
                },
                confirmButton = {
                    TextButton(onClick = { updates.download(update) }) {
                        Text("Aktualisieren")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { updates.dismissOffer() }) { Text("Später") }
                }
            )
        }
        if (updates.needsInstallPermission) {
            AlertDialog(
                onDismissRequest = { updates.cancelInstall() },
                title = { Text("Installation erlauben") },
                text = {
                    Text(
                        "Erlaube Android einmalig, Updates aus dem Overlay-Player zu " +
                            "installieren. Danach kehrst du hierher zurück."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { updates.openInstallSettings() }) {
                        Text("Einstellungen")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { updates.cancelInstall() }) { Text("Abbrechen") }
                }
            )
        }
    }

    /** Beim ersten Start nach 1.0 kommt die alte Liste aus Androids Lesefreigaben zurück. */
    private fun loadPlaylist(): List<AudioFile> {
        val saved = playlistStore.load()
        if (saved != null) {
            // Einträge von vor 1.1.1 tragen womöglich noch die Kennung der Quelle als Namen.
            val files = saved.map { (uri, name) ->
                val parsed = Uri.parse(uri)
                AudioFile(parsed, if (siehtWieKennungAus(name)) getFileName(parsed) else name)
            }
            if (files.map { it.name } != saved.map { it.second }) {
                playlistStore.save(files.map { it.uri.toString() to it.name })
            }
            return files
        }
        val restored = contentResolver.persistedUriPermissions
            .sortedBy { it.persistedTime }
            .mapNotNull { permission ->
                try {
                    if (permission.isReadPermission) {
                        AudioFile(permission.uri, getFileName(permission.uri))
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        playlistStore.save(restored.map { it.uri.toString() to it.name })
        return restored
    }

    private fun isVideo(uri: Uri): Boolean = try {
        contentResolver.getType(uri)?.startsWith("video/") == true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }

    private fun releaseFileAccess(uri: Uri) {
        try {
            contentResolver.releasePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /** Bericht ohne Dateinamen, damit er sich gefahrlos weitergeben lässt. */
    private fun writeDiagnostics() {
        val directory = File(getExternalFilesDir(null), "Diagnose")
        try {
            directory.mkdirs()
            File(directory, "diagnose.txt").writeText(
                buildString {
                    appendLine("Zeit: ${Date()}")
                    appendLine("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                    appendLine("Gerät: ${Build.MANUFACTURER} ${Build.MODEL}")
                    appendLine("Overlay-Berechtigung: ${Settings.canDrawOverlays(this@MainActivity)}")
                    appendLine("Playlist-Einträge: ${playlistStore.load()?.size ?: 0}")
                    appendLine("Dauerhafte Lesefreigaben: ${contentResolver.persistedUriPermissions.size}")
                    appendLine("Wiedergabe aktiv: ${AudioPlayerService.state.value.uri != null}")
                }
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun requestPermissions(
        launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>
    ) {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        if (permissions.isNotEmpty()) {
            launcher.launch(permissions.toTypedArray())
        }
    }

    private fun checkOverlayPermission(): Boolean {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(
                this,
                "Bitte Overlay-Berechtigung erteilen",
                Toast.LENGTH_LONG
            ).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return false
        }
        return true
    }

    private fun startAudioService(uri: Uri, title: String) {
        val intent = Intent(this, AudioPlayerService::class.java).apply {
            action = AudioPlayerService.ACTION_PLAY
            putExtra(AudioPlayerService.EXTRA_URI, uri)
            putExtra(AudioPlayerService.EXTRA_TITLE, title)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun sendAction(action: String) {
        val intent = Intent(this, AudioPlayerService::class.java).apply {
            this.action = action
        }
        startService(intent)
    }

    private fun sendTitle(title: String) {
        startService(Intent(this, AudioPlayerService::class.java).apply {
            action = AudioPlayerService.ACTION_SET_TITLE
            putExtra(AudioPlayerService.EXTRA_TITLE, title)
        })
    }

    /** Ändert nur den Namen in der Liste, die Datei selbst bleibt unberührt. */
    @Composable
    private fun RenameDialog(file: AudioFile, onDismiss: () -> Unit, onSave: (String) -> Unit) {
        // Komplett markiert: Tippen ersetzt die Kennung, statt sie zu ergänzen.
        var name by remember {
            mutableStateOf(TextFieldValue(file.name, TextRange(0, file.name.length)))
        }
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) { focus.requestFocus() }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Umbenennen") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    modifier = Modifier.focusRequester(focus)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onSave(name.text.trim()) },
                    enabled = name.text.isNotBlank()
                ) { Text("Speichern") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Abbrechen") }
            }
        )
    }

    private fun getFileName(uri: Uri): String {
        val anzeigename = try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        if (anzeigename != null && !siehtWieKennungAus(anzeigename)) return anzeigename
        return getAudioTitle(uri)
            ?: anzeigename
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: "Unbekannt"
    }

    /** Titel aus den Angaben in der Audiodatei, falls die Quelle keinen brauchbaren Namen liefert. */
    private fun getAudioTitle(uri: Uri): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(this, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.trim()
                ?.takeUnless { it.isEmpty() || siehtWieKennungAus(it) }
        } catch (e: RuntimeException) {
            e.printStackTrace()
            null
        } finally {
            retriever.release()
        }
    }

    private companion object {
        const val DIAGNOSTICS_DELAY_MS = 5_000L
    }
}
