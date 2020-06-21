package space.siy.dj.yakamochi.music2.track.database

import space.siy.dj.yakamochi.music2.track.TrackHistory

/**
 * @author SIY1121
 */
interface TrackHistoryRepository {
    fun new(url: String, title: String, thumbnail: String, duration: Int, author: String): TrackHistory
    fun done(id: Int)
    fun remove(id: Int)
    fun list(done: Boolean, offset: Int, limit: Int): List<TrackHistory>
}