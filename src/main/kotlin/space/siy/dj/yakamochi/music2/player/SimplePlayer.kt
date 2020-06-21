package space.siy.dj.yakamochi.music2.player

import space.siy.dj.yakamochi.music2.audio.AudioProvider
import space.siy.dj.yakamochi.music2.audio.GaplessAudioProvider
import space.siy.dj.yakamochi.music2.audio.SimpleAudioProvider
import space.siy.dj.yakamochi.music2.secToSampleCount
import space.siy.dj.yakamochi.music2.track.TrackQueue
import java.nio.ByteBuffer

/**
 * @author SIY1121
 */
@ExperimentalStdlibApi
class SimplePlayer(val trackQueue: TrackQueue) : Player {

    private val onQueueChanged = {
        trackQueue.list().take(3).forEach { track ->
            if (!track.audioInitialized)
                track.prepareAudio { SimpleAudioProvider(it) }
        }
    }

    init {
        trackQueue.addQueueChangedListener(onQueueChanged)
    }

    fun track(i: Int) = trackQueue[i]

    override fun play() {

    }

    override fun pause() {

    }

    override fun skip() {
        trackQueue.done()
    }

    override fun provide20MsAudio(): ByteBuffer {
        val buf = ByteBuffer.allocate(0.02f.secToSampleCount() * Short.SIZE_BYTES)
        buf.asShortBuffer().put(track(0)?.audioProvider?.read20Ms())
        if (track(0)?.audioProvider?.status == AudioProvider.Status.End)
            trackQueue.done()
        return buf
    }

    override fun canProvide() = track(0)?.audioProvider?.canRead20Ms() ?: false

}