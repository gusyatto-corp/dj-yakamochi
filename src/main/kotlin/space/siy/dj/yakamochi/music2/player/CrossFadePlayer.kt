package space.siy.dj.yakamochi.music2.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import space.siy.dj.yakamochi.music2.audio.LiveQueueAudioProvider
import space.siy.dj.yakamochi.music2.audio.QueueGaplessAudioProvider
import space.siy.dj.yakamochi.music2.sampleCountToSec
import space.siy.dj.yakamochi.music2.secToSampleCount
import space.siy.dj.yakamochi.music2.toArray
import space.siy.dj.yakamochi.music2.track.TrackQueue
import java.nio.ByteBuffer
import kotlin.math.sqrt

/**
 * @author SIY1121
 */
@ExperimentalStdlibApi
class CrossFadePlayer(override val trackQueue: TrackQueue, override var agent: PlayerAgent? = null) : Player {

    override var status = Player.Status.Stop
    private var nextTrackCheck = false
    val crossFadeDuration = 2f
    var skiping = -1f
    private val onQueueChanged = {
        trackQueue.list().take(3).forEach { track ->
            if (!track.audioInitialized)
                track.prepareAudio { if (track.duration == 0f) LiveQueueAudioProvider(it) else QueueGaplessAudioProvider(it) }
        }
        play()
    }

    init {
        trackQueue.addQueueChangedListener(onQueueChanged)
        onQueueChanged()
    }

    fun track(i: Int) = trackQueue[i]

    override fun play() {
        if (track(0) != null)
            status = Player.Status.Play
    }

    override fun pause() {
        if (track(0) != null)
            status = Player.Status.Pause
    }

    override suspend fun skip() {
        if (track(1) == null)
            agent?.requestNewTrack(trackQueue)
        if (track(0) != null)
            skiping = crossFadeDuration
//        track(1)?.audioProvider?.position = track(0)?.audioProvider?.position ?: 0
    }

    override fun provide20MsAudio(): ByteBuffer = runBlocking {
        val buf = ByteBuffer.allocate(0.02f.secToSampleCount() * Short.SIZE_BYTES)
        val nowPlayingAudioProvider = track(0)?.audioProvider ?: return@runBlocking buf
        var nowPlayingAudioData = nowPlayingAudioProvider.read20Ms().toArray()


        val remainingSec = when {
            skiping > 0 -> skiping
            nowPlayingAudioProvider.duration == 0 -> crossFadeDuration + 1
            else -> nowPlayingAudioProvider.duration.sampleCountToSec() - nowPlayingAudioProvider.position.sampleCountToSec()
        }

        if (remainingSec < 15 && !nextTrackCheck && track(1) == null) {
            GlobalScope.launch(Dispatchers.IO) { agent?.requestNewTrack(trackQueue) }
            nextTrackCheck = true
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
            GlobalScope.launch(Dispatchers.IO) { trackQueue.done() }
            skiping = -1f
            nextTrackCheck = false
        }

        buf.asShortBuffer().put(nowPlayingAudioData)
        return@runBlocking buf
    }

    override fun canProvide() = status == Player.Status.Play && track(0)?.audioProvider?.canRead20Ms() ?: false

    private fun Short.fade(t: Float) = (this * (sqrt(0.5f * t))).toShort()
}