package space.siy.dj.yakamochi.music2.track.database

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import space.siy.dj.yakamochi.music2.track.TrackHistory

/**
 * @author SIY1121
 */
class ExposedTrackHistoryRepository : TrackHistoryRepository {
    override fun new(url: String, title: String, thumbnail: String, duration: Int, author: String) =
            transaction {
                TrackHistoryDAO.new {
                    this.url = url
                    this.title = title
                    this.thumbnail = thumbnail
                    this.duration = duration
                    this.done = false
                    this.author = author
                }.transform()
            }

    override fun remove(id: Int) {
        transaction { TrackHistoryDAO.findById(id)?.delete() }
    }

    override fun done(id: Int) {
        transaction { TrackHistoryDAO.findById(id)?.done = true }
    }

    override fun list(done: Boolean, offset: Int, limit: Int) = transaction {
        TrackHistoryDAO.find {
            TrackHistoriesTable.done eq done
        }.limit(limit, offset.toLong()).map { it.transform() }
    }

    private fun TrackHistoryDAO.transform() = TrackHistory(
            this.id.value, this.title, this.thumbnail, this.url, this.duration, this.author, this.done
    )
}