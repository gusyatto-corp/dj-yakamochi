package space.siy.dj.yakamochi.music2.track.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

/**
 * @author SIY1121
 */
object TrackHistoriesTable : IntIdTable("track_histories"){
    val title = text("title")
    val url = text("url")
    val thumbnail = text("thumbnail")
    val duration = integer("duration")
    val author = text("author")
    val done = bool("done")
}