package space.siy.dj.yakamochi.music2.player

import space.siy.dj.yakamochi.music2.audio.GaplessAudioProvider
import space.siy.dj.yakamochi.music2.audio.SimpleAudioProvider
import space.siy.dj.yakamochi.music2.sampleCountToSec
import space.siy.dj.yakamochi.music2.secToSampleCount
import space.siy.dj.yakamochi.music2.toArray
import space.siy.dj.yakamochi.music2.track.TrackQueue
import java.nio.ByteBuffer
import java.nio.ShortBuffer

/**
 * @author SIY1121
 */
@ExperimentalStdlibApi
class CrossFadePlayer(val trackQueue: TrackQueue) : Player {
    val crossFadeDuration = 2f
    var skiping = -1f
    private val onQueueChanged = {
        trackQueue.list().take(3).forEach { track ->
            if (!track.audioInitialized)
                track.prepareAudio { GaplessAudioProvider(it) }
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
        skiping = crossFadeDuration
//        track(1)?.audioProvider?.position = track(0)?.audioProvider?.position ?: 0
    }

    override fun provide20MsAudio(): ByteBuffer {
        val buf = ByteBuffer.allocate(0.02f.secToSampleCount() * Short.SIZE_BYTES)
        val nowPlayingAudioProvider = track(0)?.audioProvider ?: return buf
        var nowPlayingAudioData = nowPlayingAudioProvider.read20Ms().toArray()


        val remainingSec = when {
            skiping > 0 -> skiping
            nowPlayingAudioProvider.duration == 0 -> crossFadeDuration + 1
            else -> nowPlayingAudioProvider.duration.sampleCountToSec() - nowPlayingAudioProvider.position.sampleCountToSec()
        }

        if (remainingSec <= crossFadeDuration) {
            val t = remainingSec / crossFadeDuration
            val nextAudioProvider = track(1)?.audioProvider
            // 次のトラックの準備ができていたらフェードインする
            nowPlayingAudioData = if (nextAudioProvider != null && nextAudioProvider.canRead20Ms()) {
                val nextTrackAudioData = nextAudioProvider.read20Ms()
                nowPlayingAudioData.mapIndexed { index, s ->
                    val volume = t - index / 48000f / 2
                    (s.fade(volume).toInt() + nextTrackAudioData[index].fade(1 - volume).toInt()).toShort()
                }.toShortArray()
            } else // 準備ができていない、または次のトラックが存在しない場合は何もしない
                nowPlayingAudioData.mapIndexed { index, s -> s.fade(t - index / 48000f / 2) }.toShortArray()
        }

        if (skiping > 0) skiping -= 0.02f


        if (remainingSec <= 0.03f) {
            trackQueue.done()
            skiping = -1f
        }

        buf.asShortBuffer().put(nowPlayingAudioData)
        return buf
    }

    override fun canProvide() = track(0)?.audioProvider?.canRead20Ms() ?: false

    private fun Short.fade(v: Float) = (this * (v)).toShort()
}