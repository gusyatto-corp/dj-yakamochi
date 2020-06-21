package space.siy.dj.yakamochi.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

/**
 * @author SIY1121
 */
class TrackHistoryDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TrackHistoryDAO>(TrackHistoriesTable)

    var title by TrackHistoriesTable.title
    var url by TrackHistoriesTable.url
    var thumbnail by TrackHistoriesTable.thumbnail
    var duration by TrackHistoriesTable.duration
    var done by TrackHistoriesTable.done
    var author by UserDAO referencedOn TrackHistoriesTable.author
    var guild by GuildDAO referencedOn TrackHistoriesTable.guild
    var createdAt by TrackHistoriesTable.createdAt
}