package space.siy.dj.yakamochi.music2.track

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import space.siy.dj.yakamochi.Outcome
import space.siy.dj.yakamochi.database.ExposedTrackHistoryRepository
import space.siy.dj.yakamochi.database.TrackHistory
import space.siy.dj.yakamochi.database.TrackHistoryRepository
import space.siy.dj.yakamochi.database.User
import space.siy.dj.yakamochi.music2.VideoInfo
import space.siy.dj.yakamochi.music2.audio.AudioProvider
import space.siy.dj.yakamochi.music2.remote.RemoteAudioProvider
import space.siy.dj.yakamochi.music2.sampleCountToSec
import space.siy.dj.yakamochi.music_service.MusicService
import space.siy.dj.yakamochi.music_service.MusicServiceManager

/**
 * @author SIY1121
 */

/**
 * トラックを表し、ドメイン知識・責務を持つ
 */
class Track<T : AudioProvider> private constructor(private val trackHistory: TrackHistory) : KoinComponent, VideoInfo {

    /**
     * Trackが発生させる可能性のあるエラーを表す
     */
    sealed class ErrorReason {
        data class MusicServiceError(val reason: MusicService.ErrorReason) : ErrorReason()
        object Unhandled : ErrorReason()
    }

    /**
     * トラックを保持しておくためのレポジトリ
     */
    private val repository: TrackHistoryRepository by inject()

    /**
     * このトラックの音声データを取得するためのRemoteAudioProvider
     */
    private var remoteAudioProvider: RemoteAudioProvider? = null

    /**
     * このトラックの音声データを取得するためのAudioProvider
     */
    var audioProvider: T? = null

    /**
     * AudioProviderが初期化されているかを返す
     */
    val audioInitialized: Boolean
        get() = audioProvider != null

    /**
     * トラックのIDを返す
     * リクエストごとに一意のID
     */
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

        suspend fun <T : AudioProvider> newTrack(url: String, author: String, guild: String, _info: VideoInfo? = null) =
                withContext(Dispatchers.IO) {
                    runCatching<Outcome<Track<T>, ErrorReason>> {
                        val info = _info ?: when (val r = MusicServiceManager.detail(url)) {
                            is Outcome.Success -> r.result
                            is Outcome.Error -> return@runCatching Outcome.Error(ErrorReason.MusicServiceError(r.reason), r.cause)
                        }

                        Outcome.Success(Track(new(url, info.title, info.thumbnail, info.duration.toInt(), author, guild)))
                    }.recover { Outcome.Error(ErrorReason.Unhandled, it) }
                            .getOrThrow()
                }

        fun <T : AudioProvider> fromHistory(trackHistory: TrackHistory) = Track<T>(trackHistory)
    }

    suspend fun prepareAudio(block: (it: RemoteAudioProvider) -> T) = runCatching<Outcome<Unit, ErrorReason>> {
        val _remoteAudioProvider = when (val r = MusicServiceManager.source(trackHistory.url)) {
            is Outcome.Success -> r.result
            is Outcome.Error -> {
                remove()
                return@runCatching Outcome.Error(ErrorReason.MusicServiceError(r.reason), r.cause)
            }
        }
        remoteAudioProvider = _remoteAudioProvider
        audioProvider = block(_remoteAudioProvider).apply { start() }

        Outcome.Success(Unit)
    }.recover { Outcome.Error(ErrorReason.Unhandled, null) }
            .getOrThrow()

    fun remove() {
        repository.remove(trackID)
    }

    fun done() {
        audioProvider?.release()
        repository.done(trackID)
    }
}