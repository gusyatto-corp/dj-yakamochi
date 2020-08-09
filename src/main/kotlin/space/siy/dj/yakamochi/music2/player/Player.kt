package space.siy.dj.yakamochi.music2.player

import kotlinx.coroutines.coroutineScope
import net.dv8tion.jda.api.audio.AudioSendHandler
import space.siy.dj.yakamochi.Outcome
import space.siy.dj.yakamochi.database.TrackHistoryRepository
import space.siy.dj.yakamochi.logger
import space.siy.dj.yakamochi.music2.VideoInfo
import space.siy.dj.yakamochi.music2.audio.AudioProvider
import space.siy.dj.yakamochi.music2.sampleCountToSec
import space.siy.dj.yakamochi.music2.secToSampleCount
import space.siy.dj.yakamochi.music2.track.*
import space.siy.dj.yakamochi.music_service.MusicService
import space.siy.dj.yakamochi.stackTraceString
import java.io.PrintStream
import java.io.PrintWriter

/**
 * @author SIY1121
 */
@ExperimentalStdlibApi
abstract class Player<T : AudioProvider>(val guildID: String) : AudioSendHandler {
    sealed class ErrorReason {
        data class MusicServiceError(val reason: MusicService.ErrorReason) : ErrorReason()
        object Unhandled : ErrorReason()
    }

    var initialized = false
    protected var trackProviders = FallbackTrackProvider<T>()
    private var trackQueue = TrackQueue<T>(guildID)
    private var playlistTrackProvider = PlaylistTrackProvider<T>(guildID)
    private var historyTrackProvider: HistoryTrackProvider<T>? = null

    protected var nowPlayingTrack: Track<T>? = null

    private val doneCallbackMap = HashMap<Int, () -> Unit>()

    var onErrorHandler: ((e: Outcome.Error<ErrorReason>) -> Unit)? = null

    val videoInfo: VideoInfo?
        get() = nowPlayingTrack

    val position: Float
        get() = nowPlayingTrack?.audioProvider?.position?.sampleCountToSec() ?: 0f

    abstract val status: Status

    abstract val queue: List<VideoInfo>

    suspend fun init() {
        trackProviders.add(trackQueue)
        trackQueue.loadFromHistory()
        trackProviders.add(playlistTrackProvider)
        initialized = true
    }

    abstract suspend fun play()
    abstract suspend fun pause()
    abstract suspend fun skip()

    protected suspend fun doneTrack(track: Track<T>) {
        track.done()
        doneCallbackMap.remove(track.trackID)?.invoke()
    }

    suspend fun queue(url: String, author: String, guild: String, doneCallback: (() -> Unit)? = null) = runCatching<Outcome<Unit, ErrorReason>> {
        val track = when (val r = trackQueue.queue(url, author, guild)) {
            is Outcome.Success -> r.result
            is Outcome.Error -> return@runCatching when (r.reason) {
                is TrackQueue.ErrorReason.MusicServiceError -> Outcome.Error(ErrorReason.MusicServiceError(r.reason.reason), r.cause)
                is TrackQueue.ErrorReason.Unhandled -> Outcome.Error(ErrorReason.Unhandled, r.cause)
            }
        }
        if (doneCallback != null)
            doneCallbackMap[track.trackID] = doneCallback
        play()

        Outcome.Success(Unit)
    }.recover { Outcome.Error(ErrorReason.Unhandled, it) }
            .getOrThrow()


    suspend fun setPlaylist(url: String, author: String, doneCallback: (() -> Unit)? = null) = runCatching<Outcome<Unit, ErrorReason>> {
        when (val r = playlistTrackProvider.setPlaylist(url, author, doneCallback)) {
            is Outcome.Success -> {
                play()
                Outcome.Success(Unit)
            }
            is Outcome.Error -> when (r.reason) {
                is PlaylistTrackProvider.ErrorReason.MusicServiceError -> Outcome.Error(ErrorReason.MusicServiceError(r.reason.reason), r.cause)
                is PlaylistTrackProvider.ErrorReason.Unhandled -> Outcome.Error(ErrorReason.Unhandled, r.cause)
            }
        }
    }.recover { Outcome.Error(ErrorReason.Unhandled, it) }
            .getOrThrow()

    fun setPlaylistRandom(random: Boolean) {
        playlistTrackProvider.random = random
    }

    fun setPlaylistRepeat(repeat: Boolean) {
        playlistTrackProvider.repeat = repeat
    }

    fun clearPlaylist() {
        playlistTrackProvider.clearPlaylist()
    }

    suspend fun setHistoryFallback(enable: Boolean) {
        if (enable && historyTrackProvider == null) {
            historyTrackProvider = HistoryTrackProvider(guildID)
            trackProviders.add(historyTrackProvider!!)
            play()
        } else if (!enable && historyTrackProvider != null) {
            trackProviders.remove(historyTrackProvider!!)
        }
    }

    protected fun onError(e: Outcome.Error<ErrorReason>) {
        onErrorHandler?.invoke(e)
        logger().let { logger ->
            logger.error(e.reason.toString())
            logger.error(e.cause?.stackTraceString())
        }
    }

    suspend fun requestTrack(): Track<T>? = when (val r = trackProviders.requestTrack()) {
        is Outcome.Success -> r.result
        is Outcome.Error -> {
            if (r.reason is TrackProvider.ErrorReason.NoTrack) null
            else {
                onError(
                        when (r.reason) {
                            is TrackProvider.ErrorReason.MusicServiceError -> Outcome.Error(ErrorReason.MusicServiceError(r.reason.reason), r.cause)
                            else -> Outcome.Error(ErrorReason.Unhandled, r.cause)
                        }
                )
                null
            }
        }
    }

    enum class Status {
        Play, Pause, Stop
    }
}

fun AudioProvider.read20Ms() = read(0.02f.secToSampleCount())

fun AudioProvider.canRead20Ms() = canRead(0.02f.secToSampleCount())