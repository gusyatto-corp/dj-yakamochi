package space.siy.dj.yakamochi.music2.player

import org.koin.core.KoinComponent
import org.koin.core.inject
import space.siy.dj.yakamochi.database.TrackHistoryRepository
import space.siy.dj.yakamochi.music2.track.Track
import space.siy.dj.yakamochi.music2.track.TrackQueue

/**
 * @author SIY1121
 */
@ExperimentalStdlibApi
class RandomHistoryPlayerAgent(val guildID: String, val djID: String) : PlayerAgent, KoinComponent {
    private val trackHistoryRepository: TrackHistoryRepository by inject()
    override suspend fun requestNewTrack(trackQueue: TrackQueue) {
        val newTrack = Track.fromHistory(trackHistoryRepository.rand(guildID, true))
        trackQueue.addTrack(newTrack.url, djID, guildID)
    }
}