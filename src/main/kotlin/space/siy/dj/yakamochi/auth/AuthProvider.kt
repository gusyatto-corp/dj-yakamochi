package space.siy.dj.yakamochi.auth

/**
 * @author SIY1121
 */
enum class AuthType {
    Self, Google, None
}

/**
 * ユーザーに認証を促す文字列を返却する
 */
interface AuthProvider{
    fun requestAuth(userID: String, type: AuthType, done: (() -> Unit)? = null): String
}