package space.siy.dj.yakamochi.music2.track

import kotlinx.coroutines.coroutineScope
import org.koin.core.KoinComponent
import org.koin.core.inject
import space.siy.dj.yakamochi.database.TrackHistoryRepository
import space.siy.dj.yakamochi.music2.audio.AudioProvider
import space.siy.dj.yakamochi.music2.remote.RemoteAudioProvider

/**
 * @author SIY1121
 */
class HistoryTrackProvider<T : AudioProvider>(val guildID: String) : TrackProvider<T>, KoinComponent {
    val repository: TrackHistoryRepository by inject()
    override var audioProviderCreator: ((remoteAudioProvider: RemoteAudioProvider) -> T)? = null

    override fun canProvide() = true

    override suspend fun requestTrack(): Track<T>? {
        return try {
            coroutineScope {
                Track.fromHistory<T>(repository.rand(guildID, true)).apply {
                    prepareAudio { audioProviderCreator!!(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            requestTrack()
        }
    }
}