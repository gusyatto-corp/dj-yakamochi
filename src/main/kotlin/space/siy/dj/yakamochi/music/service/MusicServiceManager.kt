package space.siy.dj.yakamochi.music.service

import space.siy.dj.yakamochi.Outcome
import space.siy.dj.yakamochi.auth.AuthType

/**
 * @author SIY1121
 */

/**
 * 処理を適切なMusicServiceに振り分ける
 */
object MusicServiceManager : MusicService {
    class NotAuthorizedError(val authType: AuthType) : Exception()

    override val authType = AuthType.None
    override val id: String = "manager"

    private val services = listOf(Youtube(), Fallback())

    private fun getSupportedService(url: String) = services.find { it.canHandle(url) }

    override fun canHandle(url: String) = true
    override fun resourceType(url: String) = getSupportedService(url)?.resourceType(url)
            ?: MusicService.ResourceType.Unknown

    override suspend fun search(q: String) = Outcome.Error(MusicService.ErrorReason.UnsupportedOperation, null)

    override suspend fun detail(url: String) = getSupportedService(url)?.detail(url)
            ?: Outcome.Error(MusicService.ErrorReason.UnsupportedResource, null)

    override suspend fun source(url: String) = getSupportedService(url)?.source(url)
            ?: Outcome.Error(MusicService.ErrorReason.UnsupportedResource, null)

    override suspend fun playlist(url: String, accessToken: String?) = getSupportedService(url)?.playlist(url)
            ?: Outcome.Error(MusicService.ErrorReason.UnsupportedResource, null)

    override suspend fun like(url: String, userID: String) = getSupportedService(url)?.like(url, userID) ?: Outcome.Error(MusicService.ErrorReason.UnsupportedResource, null)
}