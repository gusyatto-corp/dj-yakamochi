package space.siy.dj.yakamochi.music2.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.dv8tion.jda.api.audio.AudioSendHandler
import space.siy.dj.yakamochi.music2.VideoInfo
import space.siy.dj.yakamochi.music2.audio.AudioProvider
import space.siy.dj.yakamochi.music2.sampleCountToSec
import space.siy.dj.yakamochi.music2.secToSampleCount
import space.siy.dj.yakamochi.music2.track.TrackQueue

/**
 * @author SIY1121
 */
@ExperimentalStdlibApi
interface Player : AudioSendHandler {
    val trackQueue: TrackQueue
    val videoInfo: VideoInfo?
        get() = trackQueue.list().firstOrNull()
    val position: Float
        get() = trackQueue.list().firstOrNull()?.audioProvider?.position?.sampleCountToSec() ?: 0f
    val status: Status

    val queue: List<VideoInfo>
        get() = trackQueue.list()

    fun play()
    fun pause()
    suspend fun skip()
    enum class Status {
        Play, Pause, Stop
    }
}

fun AudioProvider.read20Ms() = read(0.02f.secToSampleCount())

fun AudioProvider.canRead20Ms() = canRead(0.02f.secToSampleCount())