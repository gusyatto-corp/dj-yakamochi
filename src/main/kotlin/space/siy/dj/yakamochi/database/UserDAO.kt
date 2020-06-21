package space.siy.dj.yakamochi.database

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID

/**
 * @author SIY1121
 */
class UserDAO(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, UserDAO>(UsersTable)
    var name by UsersTable.name
    var icon by UsersTable.icon
}