package space.siy.dj.yakamochi.music2.track

import space.siy.dj.yakamochi.Outcome
import space.siy.dj.yakamochi.music2.audio.AudioProvider
import space.siy.dj.yakamochi.music2.remote.RemoteAudioProvider

/**
 * @author SIY1121
 */
class FallbackTrackProvider<T : AudioProvider> : TrackProvider<T>, ArrayList<TrackProvider<T>>() {
    override var audioProviderCreator: ((remoteAudioProvider: RemoteAudioProvider) -> T)? = null

    override fun canProvide() = any { it.canProvide() }

    override suspend fun requestTrack(): Outcome<Track<T>, TrackProvider.ErrorReason> {
        var lastError: Outcome.Error<TrackProvider.ErrorReason>? = null
        repeat(10) {
            val provider = find { it.canProvide() } ?: return Outcome.Error(TrackProvider.ErrorReason.NoTrack, null)
            when (val r = provider.requestTrack()) {
                is Outcome.Success -> return Outcome.Success(r.result)
                is Outcome.Error -> {
                    if(lastError != null && r.reason is TrackProvider.ErrorReason.NoTrack)
                        return lastError!!
                    lastError = r
                }
            }
        }

    return lastError!!
    }

    override fun add(element: TrackProvider<T>): Boolean {
        element.audioProviderCreator = audioProviderCreator
        return super.add(element)
    }
}