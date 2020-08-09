package space.siy.dj.yakamochi.music2.track

import kotlinx.coroutines.coroutineScope
import org.koin.core.KoinComponent
import org.koin.core.inject
import space.siy.dj.yakamochi.Outcome
import space.siy.dj.yakamochi.database.TrackHistoryRepository
import space.siy.dj.yakamochi.music2.audio.AudioProvider
import space.siy.dj.yakamochi.music2.remote.RemoteAudioProvider

/**
 * @author SIY1121
 */

/**
 * 過去のトラックからランダムに曲を供給する
 */
class HistoryTrackProvider<T : AudioProvider>(val guildID: String) : TrackProvider<T>, KoinComponent {
    val repository: TrackHistoryRepository by inject()
    override var audioProviderCreator: ((remoteAudioProvider: RemoteAudioProvider) -> T)? = null

    override fun canProvide() = true

    override suspend fun requestTrack() = runCatching<Outcome<Track<T>, TrackProvider.ErrorReason>> {
        val track = Track.fromHistory<T>(repository.rand(guildID, true))
                .apply {
                    val r = prepareAudio { audioProviderCreator!!(it) }
                    if (r is Outcome.Error) {
                        remove()
                        return@runCatching when (r.reason) {
                            is Track.ErrorReason.MusicServiceError -> Outcome.Error(TrackProvider.ErrorReason.MusicServiceError(r.reason.reason), r.cause)
                            is Track.ErrorReason.Unhandled -> Outcome.Error(TrackProvider.ErrorReason.Unhandled, r.cause)
                        }
                    }
                }

        Outcome.Success(track)
    }.recover { Outcome.Error(TrackProvider.ErrorReason.Unhandled, it) }
            .getOrThrow()
}