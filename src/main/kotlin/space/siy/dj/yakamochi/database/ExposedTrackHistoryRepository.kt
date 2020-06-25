package space.siy.dj.yakamochi.database

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

/**
 * @author SIY1121
 */
class ExposedTrackHistoryRepository : TrackHistoryRepository {
    override fun new(url: String, title: String, thumbnail: String, duration: Int, author: String, guild: String) =
            transaction {
                TrackHistoryDAO.new {
                    this.url = url
                    this.title = title
                    this.thumbnail = thumbnail
                    this.duration = duration
                    this.done = false
                    this.author = UserDAO.findById(author) ?: throw Exception("指定されたユーザーは見つかりません")
                    this.guild = GuildDAO.findById(guild) ?: throw Exception("指定されたサーバーは見つかりません")
                    this.createdAt = DateTime.now()
                }.transform()
            }

    override fun remove(id: Int) {
        transaction { TrackHistoryDAO.findById(id)?.delete() }
    }

    override fun done(id: Int) {
        transaction { TrackHistoryDAO.findById(id)?.done = true }
    }

    override fun list(guild: String, done: Boolean, offset: Int, limit: Int) = transaction {
        TrackHistoryDAO.find {
            (TrackHistoriesTable.guild eq guild) and
                    (TrackHistoriesTable.done eq done)
        }.limit(limit, offset.toLong()).map { it.transform() }
    }

    override fun listAll(guild: String, done: Boolean): List<TrackHistory> = transaction {
        TrackHistoryDAO.find { (TrackHistoriesTable.guild eq guild) and (TrackHistoriesTable.done eq done) }.map { it.transform() }
    }

    private fun TrackHistoryDAO.transform() = TrackHistory(
            this.id.value, this.title, this.thumbnail, this.url, this.duration.toFloat(), this.author.transform(), this.guild.transform(), this.createdAt, this.done
    )

    private fun UserDAO.transform() = User(this.id.value, this.name, this.icon)
    private fun GuildDAO.transform() = Guild(this.id.value, this.name, this.icon)
}