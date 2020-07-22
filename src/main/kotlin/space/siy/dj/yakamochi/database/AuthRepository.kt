package space.siy.dj.yakamochi.database

/**
 * @author SIY1121
 */
interface AuthRepository {
    fun upsert(auth: Auth)
    fun find(userID: String, provider: AuthProvider): Auth?
    fun delete(userID: String, provider: AuthProvider)
}