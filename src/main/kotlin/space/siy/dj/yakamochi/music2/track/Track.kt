package space.siy.dj.yakamochi.music2.track

import com.sapher.youtubedl.YoutubeDL
import com.sapher.youtubedl.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import space.siy.dj.yakamochi.music2.audio.AudioProvider
import space.siy.dj.yakamochi.music2.remote.RemoteAudioProvider
import space.siy.dj.yakamochi.music2.sampleCountToSec
import space.siy.dj.yakamochi.database.ExposedTrackHistoryRepository
import space.siy.dj.yakamochi.database.TrackHistory
import space.siy.dj.yakamochi.database.TrackHistoryRepository
import space.siy.dj.yakamochi.database.User

/**
 * @author SIY1121
 */
abstract class Track<T : AudioProvider> protected constructor(protected val trackHistory: TrackHistory) : KoinComponent, space.siy.dj.yakamochi.music2.VideoInfo {
    private val repository: TrackHistoryRepository by inject()

    private var remoteAudioProvider: RemoteAudioProvider? = null
    var audioProvider: T? = null

    val audioInitialized: Boolean
        get() = audioProvider != null

    val trackID: Int
        get() = trackHistory.id

    override val title: String
        get() = trackHistory.title

    override val thumbnail: String
        get() = trackHistory.thumbnail

    override val duration: Float
        get() = audioProvider?.duration?.sampleCountToSec() ?: trackHistory.duration

    override val url: String
        get() = trackHistory.url

    val author: User
        get() = trackHistory.author

    companion object : KoinComponent {
        private val repository = ExposedTrackHistoryRepository()

        private fun new(url: String, title: String, thumbnail: String, duration: Int, author: String, guild: String) =
                repository.new(url, title, thumbnail, duration, author, guild)

        suspend fun <T: AudioProvider> newYoutubeDLTrack(url: String, author: String, guild: String, _info: VideoInfo? = null): Track<T> =
                withContext(Dispatchers.IO) {
                    val info = _info ?: YoutubeDL.getVideoInfo(url)
                    return@withContext YoutubeDLTrack<T>(new(url, info.title, info.thumbnail, info.duration, author, guild), info.formats)
                }

        fun <T: AudioProvider> fromHistory(trackHistory: TrackHistory) = YoutubeDLTrack<T>(trackHistory, YoutubeDL.getFormats(trackHistory.url))
    }

    protected abstract fun prepareRemoteAudio(): RemoteAudioProvider

    fun prepareAudio(block: (it: RemoteAudioProvider) -> T) {
        val _remoteAudioProvider = prepareRemoteAudio()
        remoteAudioProvider = _remoteAudioProvider
        audioProvider = block(_remoteAudioProvider).apply { start() }
    }

    fun remove() {
        repository.remove(trackID)
    }

    fun done() {
        audioProvider?.release()
        repository.done(trackID)
    }
}