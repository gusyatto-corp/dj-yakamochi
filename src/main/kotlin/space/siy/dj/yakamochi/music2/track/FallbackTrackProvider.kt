package space.siy.dj.yakamochi.music2.track

import space.siy.dj.yakamochi.music2.audio.AudioProvider
import space.siy.dj.yakamochi.music2.remote.RemoteAudioProvider

/**
 * @author SIY1121
 */
class FallbackTrackProvider<T: AudioProvider> : TrackProvider<T>, ArrayList<TrackProvider<T>>() {
    override var audioProviderCreator: ((remoteAudioProvider: RemoteAudioProvider) -> T)? = null

    override fun canProvide() = any { it.canProvide() }

    override suspend fun requestTrack() = find { it.canProvide() }?.requestTrack()

    override fun add(element: TrackProvider<T>): Boolean {
        element.audioProviderCreator = audioProviderCreator
        return super.add(element)
    }
}