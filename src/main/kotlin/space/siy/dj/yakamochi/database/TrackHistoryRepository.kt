package space.siy.dj.yakamochi.database

/**
 * @author SIY1121
 */
interface TrackHistoryRepository {
    fun new(url: String, title: String, thumbnail: String, duration: Int, author: String, guild: String): TrackHistory
    fun done(id: Int)
    fun remove(id: Int)
    fun list(guild: String, done: Boolean, offset: Int, limit: Int): List<TrackHistory>
    fun listAll(guild:String, done: Boolean): List<TrackHistory>
    fun rand(guild: String, distinct: Boolean): TrackHistory
}