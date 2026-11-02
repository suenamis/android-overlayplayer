package com.example.overlay_player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DateiNameTest {
    @Test
    fun kennungenWerdenErkannt() {
        listOf(
            "",
            "msf:1000012345",
            "audio:48213",
            "1000012345.mp3",
            "3f2a9c1e-7b4d-4e11-9a0c-5d2e8f1b6a77",
            "a8f3k29d7q1m5x0z2b4c",
            "9f86d081884c7d659a2feaa0c55ad015.m4a",
        ).forEach { assertTrue(it, siehtWieKennungAus(it)) }
    }

    @Test
    fun echteNamenBleiben() {
        listOf(
            "Hypnose Teil 2.mp3",
            "Kapitel_12_Einleitung.m4a",
            "Hoerbuch2Kapitel12.mp3",
            "Track 01.flac",
            "2026-11-02 Aufnahme.wav",
            "Song",
        ).forEach { assertFalse(it, siehtWieKennungAus(it)) }
    }
}
