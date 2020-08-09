package space.siy.dj.yakamochi.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

/**
 * @author SIY1121
 */
class AuthDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AuthDAO>(AuthTable)
    var userID by AuthTable.userID
    var provider by AuthTable.provider
    var accessToken by AuthTable.accessToken
    var refreshToken by AuthTable.refreshToken
}