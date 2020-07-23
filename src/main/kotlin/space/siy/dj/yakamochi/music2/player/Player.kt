package space.siy.dj.yakamochi.music2.player

import kotlinx.coroutines.coroutineScope
import net.dv8tion.jda.api.audio.AudioSendHandler
import space.siy.dj.yakamochi.database.TrackHistoryRepository
import space.siy.dj.yakamochi.music2.VideoInfo
import space.siy.dj.yakamochi.music2.audio.AudioProvider
import space.siy.dj.yakamochi.music2.sampleCountToSec
import space.siy.dj.yakamochi.music2.secToSampleCount
import space.siy.dj.yakamochi.music2.track.*

/**
 * @author SIY1121
 */
@ExperimentalStdlibApi
abstract class Player<T : AudioProvider>(val guildID: String) : AudioSendHandler {
    var initialized = false
    protected var trackProviders = FallbackTrackProvider<T>()
    protected var trackQueue = TrackQueue<T>(guildID)
    protected var playlistTrackProvider = PlaylistTrackProvider<T>(guildID)
    protected var historyTrackProvider: HistoryTrackProvider<T>? = null

    protected var nowPlayingTrack: Track<T>? = null

    val doneCallbackMap = HashMap<Int, () -> Unit>()

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

    suspend fun queue(url: String, author: String, guild: String, doneCallback: (() -> Unit)? = null) =
            try {
                coroutineScope {
                    val track = trackQueue.queue(url, author, guild)
                    if (doneCallback != null)
                        doneCallbackMap[track.trackID] = doneCallback
                    play()
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }


    suspend fun setPlaylist(url: String, author: String, doneCallback: (() -> Unit)? = null) {
        playlistTrackProvider.setPlaylist(url, author, doneCallback)
        play()
    }

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

    enum class Status {
        Play, Pause, Stop
    }
}

fun AudioProvider.read20Ms() = read(0.02f.secToSampleCount())

fun AudioProvider.canRead20Ms() = canRead(0.02f.secToSampleCount())