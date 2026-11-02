package com.example.overlay_player

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Replay30
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.overlay_player.ui.theme.OverlayPlayerTheme

data class AudioFile(val uri: Uri, val name: String)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OverlayPlayerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PlayerScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    @Composable
    private fun PlayerScreen(modifier: Modifier = Modifier) {
        val audioFiles = remember { mutableStateListOf<AudioFile>() }
        var selectedIndex by remember { mutableIntStateOf(-1) }
        var isPlaying by remember { mutableStateOf(false) }
        var playingIndex by remember { mutableIntStateOf(-1) }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { /* permissions handled */ }

        val filePickerLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->
            for (uri in uris) {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val name = getFileName(uri)
                if (audioFiles.none { it.uri == uri }) {
                    audioFiles.add(AudioFile(uri, name))
                }
            }
            if (selectedIndex == -1 && audioFiles.isNotEmpty()) {
                selectedIndex = 0
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                text = "Audiodateien im Hintergrund abspielen",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Add files button
            Button(onClick = {
                requestPermissions(permissionLauncher)
                filePickerLauncher.launch(arrayOf("audio/*"))
            }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Audiodateien hinzufügen")
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
                                .clickable { selectedIndex = index },
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
                                Icon(
                                    imageVector = if (isCurrentlyPlaying) Icons.Filled.PlayArrow else Icons.Filled.MusicNote,
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
                                    onClick = {
                                        if (isCurrentlyPlaying) {
                                            sendAction(AudioPlayerService.ACTION_STOP)
                                            isPlaying = false
                                            playingIndex = -1
                                        }
                                        audioFiles.removeAt(index)
                                        if (selectedIndex >= audioFiles.size) {
                                            selectedIndex = audioFiles.size - 1
                                        }
                                        if (playingIndex == index) {
                                            playingIndex = -1
                                        } else if (playingIndex > index) {
                                            playingIndex--
                                        }
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
                                isPlaying = false
                            } else {
                                // Play selected file (new or different)
                                if (!checkOverlayPermission()) return@FilledTonalButton
                                startAudioService(selected.uri, selected.name)
                                isPlaying = true
                                playingIndex = selectedIndex
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

                Button(onClick = {
                    sendAction(AudioPlayerService.ACTION_STOP)
                    isPlaying = false
                    playingIndex = -1
                }) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop")
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp))
                Text(
                    text = "Keine Audiodateien geladen.\nTippe oben auf den Button, um Dateien hinzuzufügen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
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

    private fun getFileName(uri: Uri): String {
        var name = "Unbekannt"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }
}
