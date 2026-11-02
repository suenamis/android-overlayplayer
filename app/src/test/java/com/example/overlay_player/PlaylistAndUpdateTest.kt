package com.example.overlay_player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import java.io.IOException
import java.security.MessageDigest

class PlaylistAndUpdateTest {

    private fun manifest(
        version: Long = 2,
        path: String = "/overlay-player-2.apk",
        size: Long = 1000,
        sha: String = "a".repeat(64)
    ) = """{"versionCode":$version,"versionName":"1.1.0","path":"$path","size":$size,"sha256":"$sha","notes":"Neu"}"""

    @Test
    fun playlistKeepsOrderNamesAndSpecialCharacters() {
        val entries = listOf(
            "content://a/1" to "Schöne \"Stelle\" 1.mp3",
            "content://a/2" to "Zwei.mp3"
        )
        assertEquals(entries, PlaylistCodec.decode(PlaylistCodec.encode(entries)))
    }

    @Test
    fun playlistDropsDuplicatesAndBrokenEntries() {
        val json = """[{"uri":"u","name":"a"},{"uri":"u","name":"b"},{"name":"c"},7]"""
        assertEquals(listOf("u" to "a"), PlaylistCodec.decode(json))
    }

    @Test
    fun manifestIsAccepted() {
        val update = UpdateRules.parseManifest(manifest())
        assertEquals(2L, update.versionCode)
        assertEquals("1.1.0", update.versionName)
    }

    @Test
    fun manifestWithForeignPathIsRejected() {
        assertThrows(IOException::class.java) {
            UpdateRules.parseManifest(manifest(path = "/../fremd.apk"))
        }
    }

    @Test
    fun manifestWithPathOfOtherVersionIsRejected() {
        assertThrows(IOException::class.java) {
            UpdateRules.parseManifest(manifest(version = 3))
        }
    }

    @Test
    fun manifestWithBadHashOrSizeIsRejected() {
        assertThrows(IOException::class.java) {
            UpdateRules.parseManifest(manifest(sha = "xyz"))
        }
        assertThrows(IOException::class.java) {
            UpdateRules.parseManifest(manifest(size = UpdateRules.MAX_APK_SIZE + 1))
        }
    }

    @Test
    fun bytesAreCheckedForSizeAndHash() {
        val apk = File.createTempFile("overlay", ".apk")
        try {
            apk.writeBytes("inhalt".toByteArray())
            val sha = MessageDigest.getInstance("SHA-256").digest(apk.readBytes())
                .joinToString("") { "%02x".format(it) }
            val good = UpdateInfo(2, "1.1.0", "/overlay-player-2.apk", apk.length(), sha, "")
            UpdateRules.verifyBytes(apk, good)
            assertThrows(IOException::class.java) {
                UpdateRules.verifyBytes(apk, good.copy(size = apk.length() + 1))
            }
            assertThrows(IOException::class.java) {
                UpdateRules.verifyBytes(apk, good.copy(sha256 = "0".repeat(64)))
            }
        } finally {
            apk.delete()
        }
    }
}
