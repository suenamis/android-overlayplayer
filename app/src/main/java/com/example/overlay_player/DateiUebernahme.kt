package com.example.overlay_player

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Übernimmt Dateien, die eine andere App mit „Öffnen mit“ oder „Teilen“ an den Player gibt.
 *
 * Solche Freigaben gelten meist nur, solange die App offen ist. Die Playlist braucht aber einen
 * Zugriff, der einen Neustart übersteht. Reihenfolge, damit möglichst nichts doppelt Platz belegt:
 * 1. dauerhafte Freigabe der Quelle, wenn sie eine anbietet;
 * 2. dieselbe Datei in Androids Medienverzeichnis (gleicher Name, gleiche Größe);
 * 3. erst dann eine Kopie im App-Speicher (`Android/data/<paket>/files/Importe`).
 */
class DateiUebernahme(private val context: Context) {

    sealed class Ergebnis {
        data class Fertig(val uri: Uri, val name: String?, val kopiert: Boolean) : Ergebnis()
        data class Fehler(val meldung: String) : Ergebnis()
    }

    private val resolver get() = context.contentResolver
    private val ordner: File? get() = context.getExternalFilesDir(ORDNER)

    suspend fun uebernehmen(uri: Uri, fortschritt: (Int) -> Unit): Ergebnis =
        withContext(Dispatchers.IO) {
            val info = angaben(uri)
            if (dauerhaftFreigeben(uri)) return@withContext Ergebnis.Fertig(uri, info.name, false)
            // Ein Medien-Link übersteht den Neustart nur mit eigenen Leserechten, nicht mit der
            // kurzen Freigabe, die beim Öffnen mitkommt.
            if (medienRechte(info.typ)) {
                if (uri.scheme == "content" && uri.authority == MediaStore.AUTHORITY && lesbarOhneFreigabe(uri)) {
                    return@withContext Ergebnis.Fertig(uri, info.name, false)
                }
                medienEintrag(info)?.let { return@withContext Ergebnis.Fertig(it, info.name, false) }
            }
            kopieren(uri, info, fortschritt)
        }

    fun istKopie(uri: Uri): Boolean =
        uri.scheme == "content" && uri.authority == authority &&
            uri.pathSegments.firstOrNull() == ORDNER_NAME

    /** Löscht die Kopie, wenn der Eintrag aus der Liste entfernt wird. */
    fun kopieLoeschen(uri: Uri) {
        if (!istKopie(uri)) return
        val name = uri.lastPathSegment ?: return
        val dir = ordner ?: return
        val datei = File(dir, name)
        if (datei.parentFile?.canonicalPath == dir.canonicalPath) datei.delete()
    }

    private data class Angaben(val name: String?, val groesse: Long, val typ: String?)

    private fun angaben(uri: Uri): Angaben {
        var name: String? = null
        var groesse = -1L
        try {
            resolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val s = c.getColumnIndex(OpenableColumns.SIZE)
                    if (n >= 0 && !c.isNull(n)) name = c.getString(n)
                    if (s >= 0 && !c.isNull(s)) groesse = c.getLong(s)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (name == null && uri.scheme == "file") name = uri.lastPathSegment
        if (groesse < 0 && uri.scheme == "file") groesse = uri.path?.let { File(it).length() } ?: -1L
        val typ = try { resolver.getType(uri) } catch (e: Exception) { null }
        return Angaben(name, groesse, typ)
    }

    private fun dauerhaftFreigeben(uri: Uri): Boolean = try {
        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        true
    } catch (e: SecurityException) {
        false
    } catch (e: IllegalArgumentException) {
        false
    }

    private fun medienRechte(typ: String?): Boolean {
        val noetig = when {
            Build.VERSION.SDK_INT < 33 -> listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            typ?.startsWith("audio/") == true -> listOf(Manifest.permission.READ_MEDIA_AUDIO)
            typ?.startsWith("video/") == true -> listOf(Manifest.permission.READ_MEDIA_VIDEO)
            else -> listOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
        }
        return noetig.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /** Lesbar aus eigenem Recht: Die Prüfung über eine neue Abfrage zählt die kurze Freigabe nicht mit. */
    private fun lesbarOhneFreigabe(uri: Uri): Boolean {
        val id = try { ContentUris.parseId(uri) } catch (e: Exception) { return false }
        val sammlung = uri.buildUpon().path(uri.path?.substringBeforeLast('/')).build()
        return try {
            resolver.query(sammlung, arrayOf(MediaStore.MediaColumns._ID), "${MediaStore.MediaColumns._ID}=?",
                arrayOf(id.toString()), null)?.use { it.count > 0 } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun lesbar(uri: Uri): Boolean = try {
        resolver.openFileDescriptor(uri, "r")?.use { true } ?: false
    } catch (e: Exception) {
        false
    }

    /** Sucht dieselbe Datei in Androids Medienverzeichnis; dafür braucht es die Medien-Leserechte. */
    private fun medienEintrag(info: Angaben): Uri? {
        val name = info.name ?: return null
        if (info.groesse <= 0) return null
        val sammlungen = buildList {
            if (info.typ?.startsWith("audio/") != false) add(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
            if (info.typ?.startsWith("video/") != false) add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        }
        for (sammlung in sammlungen) {
            try {
                resolver.query(
                    sammlung,
                    arrayOf(MediaStore.MediaColumns._ID),
                    "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.SIZE}=?",
                    arrayOf(name, info.groesse.toString()),
                    null
                )?.use { c ->
                    while (c.moveToNext()) {
                        val treffer = ContentUris.withAppendedId(sammlung, c.getLong(0))
                        if (lesbar(treffer)) return treffer
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    private fun kopieren(uri: Uri, info: Angaben, fortschritt: (Int) -> Unit): Ergebnis {
        val dir = ordner ?: return Ergebnis.Fehler("Der App-Speicher ist gerade nicht verfügbar")
        dir.mkdirs()
        val name = sichererName(info.name ?: uri.lastPathSegment ?: "Datei")
        var ziel = File(dir, name)
        if (ziel.exists() && info.groesse > 0 && ziel.length() == info.groesse) {
            return Ergebnis.Fertig(dateiUri(ziel), info.name, true)
        }
        ziel = freierName(dir, name)
        if (info.groesse > 0 && dir.usableSpace < info.groesse + RESERVE) {
            return Ergebnis.Fehler(
                "Zu wenig Speicher: Die Datei braucht ${mb(info.groesse)} MB, frei sind ${mb(dir.usableSpace)} MB"
            )
        }
        // Erst als Teil-Datei schreiben, damit eine abgebrochene Kopie nie vollständig aussieht.
        val teil = File(dir, ziel.name + ".teil")
        try {
            val eingang = resolver.openInputStream(uri)
                ?: return Ergebnis.Fehler("Die Datei ließ sich nicht öffnen")
            eingang.use { input ->
                teil.outputStream().use { output ->
                    val puffer = ByteArray(1 shl 16)
                    var gesamt = 0L
                    var gemeldet = -1
                    while (true) {
                        val n = input.read(puffer)
                        if (n < 0) break
                        output.write(puffer, 0, n)
                        gesamt += n
                        if (info.groesse > 0) {
                            val prozent = (gesamt * 100 / info.groesse).toInt()
                            if (prozent != gemeldet) {
                                gemeldet = prozent
                                fortschritt(prozent)
                            }
                        }
                    }
                    if (info.groesse > 0 && gesamt != info.groesse) {
                        throw IOException("unvollständig: $gesamt von ${info.groesse} Bytes")
                    }
                }
            }
            if (!teil.renameTo(ziel)) throw IOException("Umbenennen fehlgeschlagen")
            return Ergebnis.Fertig(dateiUri(ziel), info.name, true)
        } catch (e: Exception) {
            e.printStackTrace()
            teil.delete()
            return Ergebnis.Fehler("Die Datei ließ sich nicht übernehmen")
        }
    }

    private fun dateiUri(datei: File): Uri = FileProvider.getUriForFile(context, authority, datei)

    private val authority get() = "${context.packageName}.files"

    private companion object {
        const val ORDNER = "Importe"
        const val ORDNER_NAME = "importe"
        const val RESERVE = 200L * 1024 * 1024

        fun mb(bytes: Long) = bytes / (1024 * 1024)

        fun sichererName(name: String): String =
            name.replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001f]"), "_").trim().ifEmpty { "Datei" }.take(150)

        fun freierName(dir: File, name: String): File {
            var datei = File(dir, name)
            val basis = name.substringBeforeLast('.')
            val endung = name.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }
            var i = 2
            while (datei.exists()) {
                datei = File(dir, "$basis ($i)$endung")
                i++
            }
            return datei
        }
    }
}

/** Alle Dateien, die mit „Öffnen mit“ oder „Teilen“ ankommen. */
fun eingehendeDateien(intent: Intent?): List<Uri> {
    intent ?: return emptyList()
    if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return emptyList()
    return when (intent.action) {
        Intent.ACTION_VIEW -> listOfNotNull(intent.data)
        Intent.ACTION_SEND -> listOfNotNull(
            if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
        )
        Intent.ACTION_SEND_MULTIPLE -> (
            if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION") intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            }
        ).orEmpty().filterNotNull()
        else -> emptyList()
    }
}
