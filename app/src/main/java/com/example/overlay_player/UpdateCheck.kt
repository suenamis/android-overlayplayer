package com.example.overlay_player

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class UpdateInfo(
    val versionCode: Long,
    val versionName: String,
    val path: String,
    val size: Long,
    val sha256: String,
    val notes: String
)

/** Prüfregeln nach dem Airhold-Muster; rein, damit sie ohne Gerät testbar sind. */
object UpdateRules {
    const val MAX_APK_SIZE = 100_000_000L

    fun parseManifest(json: String): UpdateInfo {
        val manifest = JSONObject(json)
        val version = manifest.getLong("versionCode")
        val size = manifest.getLong("size")
        val path = manifest.getString("path")
        val sha256 = manifest.getString("sha256")
        if (version < 1 || version > Int.MAX_VALUE ||
            size < 1 || size > MAX_APK_SIZE ||
            path != "/overlay-player-$version.apk" ||
            !sha256.matches(Regex("[a-fA-F0-9]{64}"))
        ) {
            throw IOException("Ungültige Update-Angaben")
        }
        return UpdateInfo(
            version,
            manifest.getString("versionName"),
            path,
            size,
            sha256,
            manifest.optString("notes", "Neue Version verfügbar.")
        )
    }

    fun verifyBytes(apk: File, update: UpdateInfo) {
        if (apk.length() != update.size) throw IOException("Update-Dateigröße stimmt nicht")
        val hash = MessageDigest.getInstance("SHA-256")
        FileInputStream(apk).use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                hash.update(buffer, 0, count)
            }
        }
        val hex = hash.digest().joinToString("") { "%02x".format(it) }
        if (!hex.equals(update.sha256, ignoreCase = true)) {
            throw IOException("Update-Prüfsumme stimmt nicht. Installation gestoppt.")
        }
    }
}

/** Sucht, lädt, prüft und übergibt Updates an Android; die Installation bestätigt Android. */
class UpdateController(private val context: Context, private val scope: CoroutineScope) {
    private val prefs = context.getSharedPreferences(
        AudioPlayerService.PREFS_NAME, Context.MODE_PRIVATE
    )
    private var checking = false
    private var nextAutomaticCheck = 0L

    var status by mutableStateOf("")
        private set
    var offer by mutableStateOf<UpdateInfo?>(null)
        private set
    var downloading by mutableStateOf(false)
        private set
    var needsInstallPermission by mutableStateOf(false)
        private set

    private val apkFile get() = File(File(context.cacheDir, "updates"), "overlay-player.apk")

    fun check(automatic: Boolean) {
        if (checking || downloading || offer != null) return
        if (automatic && SystemClock.elapsedRealtime() < nextAutomaticCheck) return
        checking = true
        nextAutomaticCheck = SystemClock.elapsedRealtime() + RETRY_MS
        status = "Suche nach Updates …"
        scope.launch {
            try {
                val update = withContext(Dispatchers.IO) {
                    UpdateRules.parseManifest(fetchManifest())
                }
                nextAutomaticCheck = SystemClock.elapsedRealtime() + CHECK_INTERVAL_MS
                if (update.versionCode > BuildConfig.VERSION_CODE) {
                    status = "Update verfügbar: ${update.versionName}"
                    offer = update
                } else {
                    status = "Aktuell"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Ohne Tailscale ist das der Normalfall, deshalb keine Meldung beim Selbstcheck.
                status = if (automatic) "" else "Update-Server nicht erreichbar. Ist Tailscale an?"
            } finally {
                checking = false
            }
        }
    }

    fun dismissOffer() {
        offer = null
    }

    fun download(update: UpdateInfo) {
        offer = null
        downloading = true
        status = "Update wird heruntergeladen …"
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    downloadTo(update, apkFile)
                    verify(apkFile, update)
                }
                prefs.edit().putString(KEY_PENDING, JSONObject()
                    .put("versionCode", update.versionCode)
                    .put("versionName", update.versionName)
                    .put("path", update.path)
                    .put("size", update.size)
                    .put("sha256", update.sha256)
                    .toString()).apply()
                install()
            } catch (e: Exception) {
                e.printStackTrace()
                status = e.message ?: "Update fehlgeschlagen"
            } finally {
                downloading = false
            }
        }
    }

    /** Nach der Rückkehr aus den Android-Einstellungen läuft die Installation weiter. */
    fun resumePendingInstall() {
        if (prefs.contains(KEY_PENDING) && canInstall()) install()
    }

    fun cancelInstall() {
        needsInstallPermission = false
        prefs.edit().remove(KEY_PENDING).apply()
    }

    fun openInstallSettings() {
        needsInstallPermission = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun install() {
        try {
            val update = UpdateRules.parseManifest(prefs.getString(KEY_PENDING, "") ?: "")
            verify(apkFile, update)
            if (!canInstall()) {
                needsInstallPermission = true
                return
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apkFile)
            prefs.edit().remove(KEY_PENDING).apply()
            status = "Installation in Android bestätigen"
            // Die Installation beendet den Prozess; die Hörstelle muss vorher gesichert sein.
            if (AudioPlayerService.state.value.uri != null) {
                context.startService(
                    Intent(context, AudioPlayerService::class.java)
                        .setAction(AudioPlayerService.ACTION_SAVE_POSITION)
                )
            }
            context.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                    )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            prefs.edit().remove(KEY_PENDING).apply()
            status = "Installation gestoppt: ${e.message}"
        }
    }

    private fun canInstall(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()

    private fun open(path: String): HttpURLConnection {
        val url = URL(BuildConfig.UPDATE_BASE_URL + path)
        if (url.protocol != "https" && !BuildConfig.UPDATE_ALLOW_CLEARTEXT) {
            throw IOException("Updates nur über HTTPS")
        }
        val connection = url.openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 8_000
        connection.readTimeout = 20_000
        if (connection.responseCode != 200) {
            connection.disconnect()
            throw IOException("Update-Server antwortet nicht wie erwartet")
        }
        return connection
    }

    private fun fetchManifest(): String {
        val connection = open("/latest.json")
        try {
            return connection.inputStream.use {
                String(it.readBytes(), Charsets.UTF_8)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadTo(update: UpdateInfo, target: File) {
        target.parentFile?.mkdirs()
        val connection = open(update.path)
        try {
            var total = 0L
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > update.size) throw IOException("Update zu groß")
                        output.write(buffer, 0, count)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun verify(apk: File, update: UpdateInfo) {
        UpdateRules.verifyBytes(apk, update)
        val manager = context.packageManager
        val next = readArchive(manager, apk)
        val installed = manager.getPackageInfo(context.packageName, signatureFlags())
        if (next == null ||
            next.packageName != context.packageName ||
            PackageInfoCompat.getLongVersionCode(next) != update.versionCode ||
            update.versionCode <= PackageInfoCompat.getLongVersionCode(installed) ||
            !sameSigner(signers(installed), signers(next))
        ) {
            throw IOException("App-Signatur oder Version stimmt nicht. Installation gestoppt.")
        }
    }

    @Suppress("DEPRECATION")
    private fun signatureFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }

    @Suppress("DEPRECATION")
    private fun readArchive(manager: PackageManager, apk: File): PackageInfo? {
        // Android 9/10 liefern Archiv-Zertifikate nur mit GET_SIGNATURES zusätzlich.
        return manager.getPackageArchiveInfo(
            apk.path, signatureFlags() or PackageManager.GET_SIGNATURES
        )
    }

    @Suppress("DEPRECATION")
    private fun signers(info: PackageInfo): List<Signature> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.let { return it.apkContentsSigners.toList() }
        }
        return info.signatures?.toList().orEmpty()
    }

    private fun sameSigner(a: List<Signature>, b: List<Signature>): Boolean =
        a.size == 1 && b.size == 1 && a[0] == b[0]

    private companion object {
        const val KEY_PENDING = "pending_update"
        const val RETRY_MS = 60_000L
        const val CHECK_INTERVAL_MS = 15 * 60_000L
    }
}
