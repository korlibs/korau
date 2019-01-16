package com.soywiz.korau.sound

import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class NativeMp3DecoderTest {
    @Test
    fun test() = suspendTest {
        val decoder = NativeMp3DecoderFormat
        val file = resourcesVfs["mp31.mp3"]
        val stream = decoder.decodeStream(file.open()) ?: error("Can't open decoder")
        val audioData = stream.toData()
        assertEquals(1, audioData.channels)
        assertEquals(25344, audioData.totalSamples)
        assertEquals(44100, audioData.rate)
        assertEquals(574, audioData.totalTime.millisecondsInt)
        //localCurrentDirVfs["demo.out.raw"].write(MemorySyncStreamToByteArray { writeShortArrayLE(audioData.samples) })
    }
}
