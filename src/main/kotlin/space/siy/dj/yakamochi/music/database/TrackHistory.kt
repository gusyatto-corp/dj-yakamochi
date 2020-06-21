package space.siy.dj.yakamochi.music.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

/**
 * @author SIY1121
 */
class TrackHistory(id : EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TrackHistory>(TrackHistories)
    var title by TrackHistories.title
    var url by TrackHistories.url
    var thumbnail by TrackHistories.thumbnail
    var duration by TrackHistories.duration
    var done by TrackHistories.done
}