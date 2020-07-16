package space.siy.dj.yakamochi.music2.track

import space.siy.dj.yakamochi.music2.audio.AudioProvider
import space.siy.dj.yakamochi.music2.remote.RemoteAudioProvider

/**
 * @author SIY1121
 */
interface TrackProvider<T: AudioProvider> {
    var audioProviderCreator: ((remoteAudioProvider: RemoteAudioProvider) -> T)?
    fun canProvide(): Boolean
    suspend fun requestTrack(): Track<T>?
}