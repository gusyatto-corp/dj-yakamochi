package space.siy.dj.yakamochi.database

import org.jetbrains.exposed.dao.id.IntIdTable

/**
 * @author SIY1121
 */
object AuthTable: IntIdTable("auth") {
    val userID = reference("user_id", UsersTable)
    val provider = text("provider")
    val accessToken = text("access_token")
    val refreshToken = text("refresh_token")
}