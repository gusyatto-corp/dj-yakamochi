package space.siy.dj.yakamochi.database

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

/**
 * @author SIY1121
 */
object GuildsTable : IdTable<String>("guilds") {
    val name = text("name")
    val icon = text("icon")
    override val id: Column<EntityID<String>> = text("id").entityId()
}