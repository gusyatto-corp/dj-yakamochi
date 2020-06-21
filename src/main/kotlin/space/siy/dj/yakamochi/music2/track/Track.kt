package space.siy.dj.yakamochi.music2.track

import com.sapher.youtubedl.YoutubeDL
import com.sapher.youtubedl.mapper.VideoInfo
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
abstract class Track protected constructor(private val trackHistory: TrackHistory) : KoinComponent {
    private val repository: TrackHistoryRepository by inject()

    private var remoteAudioProvider: RemoteAudioProvider? = null
    var audioProvider: AudioProvider? = null

    val audioInitialized: Boolean
        get() = audioProvider != null


    val trackID: Int
        get() = trackHistory.id

    val title: String
        get() = trackHistory.title

    val thumbnail: String
        get() = trackHistory.thumbnail

    val duration: Float
        get() = audioProvider?.duration?.sampleCountToSec() ?: trackHistory.duration.toFloat()

    val url: String
        get() = trackHistory.url

    val author: User
        get() = trackHistory.author

    companion object : KoinComponent {
        private val repository = ExposedTrackHistoryRepository()

        private fun new(url: String, title: String, thumbnail: String, duration: Int, author: String, guild: String) =
                repository.new(url, title, thumbnail, duration, author, guild)

        fun newYoutubeDLTrack(url: String, author: String, guild: String, _info: VideoInfo? = null): Track {
            val info = _info ?: YoutubeDL.getVideoInfo(url)
            return YoutubeDLTrack(new(url, info.title, info.thumbnail, info.duration, author, guild), info)
        }
    }

    protected abstract fun prepareRemoteAudio(): RemoteAudioProvider

    fun prepareAudio(block: (it: RemoteAudioProvider) -> AudioProvider) {
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