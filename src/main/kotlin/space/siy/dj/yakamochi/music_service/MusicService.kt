package space.siy.dj.yakamochi.music_service

import space.siy.dj.yakamochi.Outcome
import space.siy.dj.yakamochi.auth.AuthType
import space.siy.dj.yakamochi.music2.VideoInfo
import space.siy.dj.yakamochi.music2.remote.RemoteAudioProvider

/**
 * @author SIY1121
 */
interface MusicService {
    sealed class ErrorReason {
        data class Unavailable(val reason: Reason): ErrorReason() {
            enum class Reason {
                NotFound, Forbidden, Unknown
            }
        }
        object Unhandled: ErrorReason()
        data class Unauthorized(val type: AuthType): ErrorReason()
        object UnsupportedResource: ErrorReason()
        object UnsupportedOperation: ErrorReason()
    }

    enum class ResourceType {
        Video, Playlist, Unknown
    }
    val id: String
    val authType: AuthType

    fun canHandle(url: String): Boolean
    fun resourceType(url: String): ResourceType
    suspend fun search(q: String): Outcome<List<VideoInfo>, ErrorReason>
    suspend fun detail(url: String): Outcome<VideoInfo, ErrorReason>
    suspend fun source(url: String): Outcome<RemoteAudioProvider, ErrorReason>
    suspend fun playlist(url: String, accessToken: String? = null): Outcome<List<VideoInfo>, ErrorReason>
    suspend fun like(url: String, userID: String): Outcome<Unit, ErrorReason>
}