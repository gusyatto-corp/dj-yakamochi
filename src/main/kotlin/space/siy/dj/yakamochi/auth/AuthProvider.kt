package space.siy.dj.yakamochi.auth

/**
 * @author SIY1121
 */
enum class AuthType {
    Self, Google, None
}

interface AuthProvider{
    fun requestAuth(userID: String, type: AuthType, done: (() -> Unit)? = null): String
}