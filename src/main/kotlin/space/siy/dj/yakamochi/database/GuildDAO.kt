package space.siy.dj.yakamochi.database

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID

/**
 * @author SIY1121
 */
class GuildDAO(id: EntityID<String>): Entity<String>(id){
    companion object : EntityClass<String, GuildDAO>(GuildsTable)
    var name by GuildsTable.name
    var icon by GuildsTable.icon
}