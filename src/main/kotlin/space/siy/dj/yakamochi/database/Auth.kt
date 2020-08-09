package space.siy.dj.yakamochi.database

/**
 * @author SIY1121
 */

enum class AuthProvider {
    Google
}

data class Auth(val userID: String, val provider: AuthProvider, val accessToken: String, val refreshToken: String)