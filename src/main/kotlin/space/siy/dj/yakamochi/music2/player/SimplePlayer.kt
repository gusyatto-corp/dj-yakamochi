package space.siy.dj.yakamochi.music2.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import space.siy.dj.yakamochi.music2.audio.AudioProvider
import space.siy.dj.yakamochi.music2.audio.LiveQueueAudioProvider
import space.siy.dj.yakamochi.music2.audio.QueueAudioProvider
import space.siy.dj.yakamochi.music2.secToSampleCount
import space.siy.dj.yakamochi.music2.track.TrackQueue
import java.nio.ByteBuffer

/**
 * @author SIY1121
 */
@ExperimentalStdlibApi
class SimplePlayer(override val trackQueue: TrackQueue) : Player {

    override var status = Player.Status.Stop

    private val onQueueChanged = {
        trackQueue.list().take(3).forEach { track ->
            if (!track.audioInitialized)
                track.prepareAudio {
                    if (track.duration == 0f)
                        LiveQueueAudioProvider(it)
                    else
                        QueueAudioProvider(it)
                }
        }
        play()
    }

    init {
        onQueueChanged()
        trackQueue.addQueueChangedListener(onQueueChanged)
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

    override suspend fun skip() = withContext(Dispatchers.IO) {
        if (track(0) != null)
            trackQueue.done()
    }

    override fun provide20MsAudio(): ByteBuffer = runBlocking {
        val buf = ByteBuffer.allocate(0.02f.secToSampleCount() * Short.SIZE_BYTES)
        buf.asShortBuffer().put(track(0)?.audioProvider?.read20Ms())
        if (track(0)?.audioProvider?.status == AudioProvider.Status.End)
            launch { trackQueue.done() }
        return@runBlocking buf
    }

    override fun canProvide() = status == Player.Status.Play && track(0)?.audioProvider?.canRead20Ms() ?: false

}