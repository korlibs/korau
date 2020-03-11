package com.soywiz.korau.impl.jna

import com.soywiz.klock.*
import com.soywiz.korau.format.*
import com.soywiz.korau.format.mp3.*
import com.soywiz.korau.sound.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.stream.*
import kotlinx.coroutines.*

fun ByteArray.asMemoryVfsFile(name: String = "temp.bin"): VfsFile = MemoryVfs(mapOf(name to openAsync()))[name]
suspend fun VfsFile.cachedToMemory(): VfsFile = this.readAll().asMemoryVfsFile(this.fullName)

object JnaSoundProviderSample {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            //val data = resourcesVfs["monkey_drama.mp3"].readNativeMusic()
            val group = NativeSoundChannelGroup(volume = 0.2)
            println(resourcesVfs["monkey_drama.mp3"].cachedToMemory().readSoundInfo(props = AudioDecodingProps(exactTimings = false))?.decodingTime)
            println(resourcesVfs["monkey_drama.mp3"].cachedToMemory().readSoundInfo(props = AudioDecodingProps(exactTimings = true))?.decodingTime)
            //val data = resourcesVfs["mp31_joint_stereo_vbr.mp3"].readNativeMusic()
            //val data = resourcesVfs["mp31_joint_stereo_vbr.mp3"].readNativeMusic()
            val stream = resourcesVfs["monkey_drama.mp3"].readAudioStream(MP3Decoder)
            println(resourcesVfs["monkey_drama.mp3"].readAll().asMemoryVfsFile("temp.mp3").readSoundInfo(MP3Decoder)!!.duration)
            val data = resourcesVfs["monkey_drama.mp3"].readNativeMusic()
            //val data = resourcesVfs["mp31_joint_stereo_vbr.mp3"].readNativeSound()

            //println(data.length)
            //val result = data.playForever().attachTo(group)
            println(data.length)
            val result = data.play(2.playbackTimes).attachTo(group)
            println(result.total)
            //group.volume = 0.2

            //group.pitch = 1.5
            group.pitch = 1.0
            for (n in -10 .. +10) {
                group.panning = n.toDouble() / 10.0
                println(group.panning)
                com.soywiz.korio.async.delay(0.1.seconds)
            }
            println("Waiting...")
            group.await()
            println("Stop...")
            //com.soywiz.korio.async.delay(1.seconds)
            group.stop()
        }
    }
}
