package space.siy.dj.yakamochi.music2.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.dv8tion.jda.api.audio.AudioSendHandler
import space.siy.dj.yakamochi.music2.VideoInfo
import space.siy.dj.yakamochi.music2.audio.AudioProvider
import space.siy.dj.yakamochi.music2.sampleCountToSec
import space.siy.dj.yakamochi.music2.secToSampleCount
import space.siy.dj.yakamochi.music2.track.FallbackTrackProvider
import space.siy.dj.yakamochi.music2.track.Track
import space.siy.dj.yakamochi.music2.track.TrackProvider
import space.siy.dj.yakamochi.music2.track.TrackQueue

/**
 * @author SIY1121
 */
@ExperimentalStdlibApi
abstract class Player<T : AudioProvider>(val guildID: String) : AudioSendHandler {
    protected var trackProviders = FallbackTrackProvider<T>()
    protected var trackQueue = TrackQueue<T>(guildID)

    protected var nowPlayingTrack: Track<T>? = null

    val videoInfo: VideoInfo?
        get() = nowPlayingTrack

    val position: Float
        get() = nowPlayingTrack?.audioProvider?.position?.sampleCountToSec() ?: 0f

    abstract val status: Status

    abstract val queue: List<VideoInfo>

    suspend fun init() {
        trackProviders.add(trackQueue)
        trackQueue.loadFromHistory()
    }

    abstract suspend fun play()
    abstract suspend fun pause()
    abstract suspend fun skip()

    suspend fun queue(url: String, author: String, guild: String) {
        trackQueue.queue(url, author, guild)
        play()
    }

    enum class Status {
        Play, Pause, Stop
    }
}

fun AudioProvider.read20Ms() = read(0.02f.secToSampleCount())

fun AudioProvider.canRead20Ms() = canRead(0.02f.secToSampleCount())