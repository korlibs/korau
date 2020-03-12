package com.soywiz.korau.sound

import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korau.error.*
import com.soywiz.korau.format.*
import com.soywiz.korau.internal.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import kotlin.coroutines.*

class HtmlNativeSoundProvider : NativeSoundProvider() {
	override fun initOnce() {
	}

	override fun createAudioStream(coroutineContext: CoroutineContext, freq: Int): PlatformAudioOutput = JsPlatformAudioOutput(coroutineContext, freq)

	override suspend fun createSound(data: ByteArray, streaming: Boolean, props: AudioDecodingProps): NativeSound =
        AudioBufferNativeSound(HtmlSimpleSound.loadSound(data), coroutineContext)

	override suspend fun createSound(vfs: Vfs, path: String, streaming: Boolean, props: AudioDecodingProps): NativeSound = when (vfs) {
		is LocalVfs, is UrlVfs -> {
			val rpath = when (vfs) {
				is LocalVfs -> path
				is UrlVfs -> vfs.getFullUrl(path)
				else -> invalidOp
			}
			AudioBufferNativeSound(HtmlSimpleSound.loadSound(rpath), coroutineContext)
		}
		else -> {
			super.createSound(vfs, path)
		}
	}
}

class AudioBufferNativeSound(val buffer: AudioBuffer?, val coroutineContext: CoroutineContext) : NativeSound() {
	override val length: TimeSpan = ((buffer?.duration) ?: 0.0).seconds

	override suspend fun decode(): AudioData = if (buffer == null) {
		AudioData.DUMMY
	} else {
		val nchannels = buffer.numberOfChannels
		val nsamples = buffer.length
		val data = AudioSamples(nchannels, nsamples)
		var m = 0
		for (c in 0 until nchannels) {
			val channelF = buffer.getChannelData(c)
			for (n in 0 until nsamples) {
				data[c][m++] = SampleConvert.floatToShort(channelF[n])
			}
		}
		AudioData(buffer.sampleRate, data)
	}

	override fun play(params: PlaybackParameters): NativeSoundChannel {
        //println("AudioBufferNativeSound.play: $params")
		return object : NativeSoundChannel(this) {
			val channel = if (buffer != null) HtmlSimpleSound.playSound(buffer, params,coroutineContext) else null

			override var volume: Double
				get() = channel?.volume ?: 1.0
				set(value) { channel?.volume = value}
			override var pitch: Double
				get() = channel?.pitch ?: 1.0
				set(value) { channel?.pitch = value }
			override var panning: Double
				get() = channel?.panning ?: 0.0
				set(value) { channel?.panning = value }
			override var current: TimeSpan
                get() = channel?.currentTime ?: 0.seconds
                set(value) = run { channel?.currentTime = value }
			override val total: TimeSpan = buffer?.duration?.seconds ?: 0.seconds
			override val playing: Boolean get() = channel?.playing ?: (current < total)

			override fun stop(): Unit = run { channel?.stop() }
		}.also {
            //it.current = params.startTime
            it.copySoundPropsFrom(params)
        }
	}
}
