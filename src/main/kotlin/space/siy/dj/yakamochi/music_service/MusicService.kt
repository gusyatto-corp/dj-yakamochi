package space.siy.dj.yakamochi.music_service

import space.siy.dj.yakamochi.auth.AuthType
import space.siy.dj.yakamochi.music2.VideoInfo
import space.siy.dj.yakamochi.music2.remote.RemoteAudioProvider

/**
 * @author SIY1121
 */
interface MusicService {
    enum class ResourceType {
        Video, Playlist, Unknown
    }
    val id: String
    val authType: AuthType

    fun canHandle(url: String): Boolean
    fun resourceType(url: String): ResourceType
    suspend fun search(q: String):List<VideoInfo>
    suspend fun detail(url: String): VideoInfo?
    suspend fun source(url: String): RemoteAudioProvider?
    suspend fun playlist(url: String, accessToken: String? = null): List<VideoInfo>
    suspend fun like(url: String, userID: String)
}