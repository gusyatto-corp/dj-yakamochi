package space.siy.dj.yakamochi.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.jodatime.datetime

/**
 * @author SIY1121
 */
object TrackHistoriesTable : IntIdTable("track_histories"){
    val title = text("title")
    val url = text("url")
    val thumbnail = text("thumbnail")
    val duration = integer("duration")
    val author = reference("author", UsersTable)
    val guild = reference("guild", GuildsTable)
    val done = bool("done")
    val createdAt = datetime("created_at")
}