package space.siy.dj.yakamochi.music2.track

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import space.siy.dj.yakamochi.database.TrackHistoryRepository
import space.siy.dj.yakamochi.music2.audio.AudioProvider
import space.siy.dj.yakamochi.music2.remote.RemoteAudioProvider

/**
 * @author SIY1121
 */
@ExperimentalStdlibApi
class TrackQueue<T : AudioProvider>(val guildID: String) : TrackProvider<T>, KoinComponent {

    val queue = ArrayDeque<Track<T>>()
    val trackHistoryRepository by inject<TrackHistoryRepository>()

    suspend fun loadFromHistory() {
        trackHistoryRepository.listAll(guildID, false).map { history ->
            val track = Track.fromHistory<T>(history)
            queue.add(track)
            track
        }.take(1).forEach { track ->
            track.prepareAudio { audioProviderCreator!!(it) }
        }
    }

    suspend fun queue(url: String, author: String, guild: String) = withContext(Dispatchers.IO) {
        if (audioProviderCreator == null) throw Exception("AudioProviderCreator is not set")

        val track = Track.newYoutubeDLTrack<T>(url, author, guild)
//        track.prepareAudio { audioProviderCreator!!(it) }
        queue.add(track)
    }

    override fun canProvide() = queue.size > 0

    override var audioProviderCreator: ((remoteAudioProvider: RemoteAudioProvider) -> T)? = null

    override suspend fun requestTrack(): Track<T> {
        val res = queue.removeFirst().apply {
            if (!audioInitialized) prepareAudio { audioProviderCreator!!(it) }
        }
        if (queue.firstOrNull() != null)
            queue.first().prepareAudio { audioProviderCreator!!(it) }
        return res
    }
}