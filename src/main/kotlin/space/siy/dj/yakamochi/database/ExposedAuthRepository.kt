package space.siy.dj.yakamochi.database

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * @author SIY1121
 */
class ExposedAuthRepository : AuthRepository {
    override fun upsert(auth: Auth) = transaction {
        val target = AuthDAO.find { (AuthTable.userID eq auth.userID) and (AuthTable.provider eq auth.provider.toString()) }.firstOrNull()
        if (target == null) {
            AuthDAO.new {
                userID = UserDAO.findById(auth.userID)?.id ?: throw Exception("指定されたユーザーが見つかりません")
                provider = auth.provider.toString()
                accessToken = auth.accessToken
                refreshToken = auth.refreshToken
            }
        } else {
            target.apply {
                accessToken = auth.accessToken
                refreshToken = auth.refreshToken
            }
        }
        return@transaction
    }

    override fun find(userID: String, provider: AuthProvider) = transaction {
        AuthDAO.find {
            (AuthTable.userID eq userID) and (AuthTable.provider eq provider.toString())
        }.limit(1).firstOrNull()?.transform()
    }

    override fun delete(userID: String, provider: AuthProvider) = transaction {
        AuthDAO.find { (AuthTable.userID eq userID) and (AuthTable.provider eq provider.toString()) }.forEach { it.delete() }
    }

    private fun AuthDAO.transform() = Auth(userID.value, AuthProvider.valueOf(provider), accessToken, refreshToken)
}