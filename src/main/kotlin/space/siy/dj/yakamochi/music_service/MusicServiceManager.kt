package space.siy.dj.yakamochi.music_service

import space.siy.dj.yakamochi.music2.VideoInfo
import space.siy.dj.yakamochi.music2.VideoSourceInfo

/**
 * @author SIY1121
 */
object MusicServiceManager : MusicService {
    override val id: String = "manager"

    private val services = listOf(Youtube(), Fallback())

    private fun getSupportedService(url: String) = services.find { it.canHandle(url) }

    override fun canHandle(url: String) = true
    override fun resourceType(url: String) = getSupportedService(url)?.resourceType(url)
            ?: MusicService.ResourceType.Unknown

    override suspend fun search(q: String): List<VideoInfo> {
        TODO("Not yet implemented")
    }

    override suspend fun detail(url: String) = getSupportedService(url)?.detail(url)

    override suspend fun source(url: String) = getSupportedService(url)?.source(url)

    override suspend fun playlist(url: String, accessToken: String?) = getSupportedService(url)?.playlist(url)
            ?: emptyList()

    override suspend fun like(id: String, accessToken: String) {
        TODO("Not yet implemented")
    }
}