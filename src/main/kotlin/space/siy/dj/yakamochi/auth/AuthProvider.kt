package space.siy.dj.yakamochi.auth

/**
 * @author SIY1121
 */
enum class AuthType {
    Self, Youtube
}

interface AuthProvider{
    fun requestAuth(userID: String, type: AuthType): String
}