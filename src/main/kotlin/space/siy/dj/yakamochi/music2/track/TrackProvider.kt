package space.siy.dj.yakamochi.music2.track

import space.siy.dj.yakamochi.Outcome
import space.siy.dj.yakamochi.music2.audio.AudioProvider
import space.siy.dj.yakamochi.music2.remote.RemoteAudioProvider
import space.siy.dj.yakamochi.music_service.MusicService

/**
 * @author SIY1121
 */
interface TrackProvider<T: AudioProvider> {
    sealed class ErrorReason {
        object NoTrack: ErrorReason()
        data class MusicServiceError(val reason: MusicService.ErrorReason): ErrorReason()
        object Unhandled: ErrorReason()
        object NoAudioProviderCreatorFound: ErrorReason()
    }

    var audioProviderCreator: ((remoteAudioProvider: RemoteAudioProvider) -> T)?
    fun canProvide(): Boolean
    suspend fun requestTrack(): Outcome<Track<T>, ErrorReason>
}