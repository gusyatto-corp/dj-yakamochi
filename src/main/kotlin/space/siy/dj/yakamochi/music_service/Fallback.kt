package space.siy.dj.yakamochi.music_service

import com.sapher.youtubedl.YoutubeDL
import com.sapher.youtubedl.YoutubeDLException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.siy.dj.yakamochi.Outcome
import space.siy.dj.yakamochi.auth.AuthType
import space.siy.dj.yakamochi.music.VideoInfo
import space.siy.dj.yakamochi.music.VideoSourceInfoImpl
import space.siy.dj.yakamochi.music.remote.RemoteAudioProvider
import space.siy.dj.yakamochi.music.remote.YoutubeDLFFmpegRemoteAudioProvider

/**
 * @author SIY1121
 */

/**
 * 専用MusicServiceが存在しない場合のフォールバック
 * 情報の取得にすべてYoutubeDLの使用を試みる
 */
class Fallback : MusicService {
    override val id = "fallback"
    override val authType = AuthType.None

    data class VideoInfoImpl(override val url: String, override val title: String, override val duration: Float, override val thumbnail: String) : VideoInfo

    override fun canHandle(url: String) = true
    override fun resourceType(url: String) = MusicService.ResourceType.Video

    override suspend fun search(q: String) = Outcome.Error(MusicService.ErrorReason.UnsupportedOperation, null)

    override suspend fun detail(url: String) = withContext(Dispatchers.IO) {
        runCatching<Outcome<VideoInfo, MusicService.ErrorReason>> {
            val res = YoutubeDL.getVideoInfo(url)

            Outcome.Success(
                    VideoInfoImpl(
                            url, res.title, res.duration.toFloat(), res.thumbnail
                    )
            )
        }.recover { e ->
            when (e) {
                is YoutubeDLException -> Outcome.Error(MusicService.ErrorReason.Unavailable(MusicService.ErrorReason.Unavailable.Reason.Unknown), e)
                else -> Outcome.Error(MusicService.ErrorReason.Unhandled, e)
            }
        }.getOrThrow()
    }

    override suspend fun source(url: String) = withContext(Dispatchers.IO) {
        runCatching<Outcome<RemoteAudioProvider, MusicService.ErrorReason>> {
            val info = when (val r = detail(url)) {
                is Outcome.Success -> r.result
                is Outcome.Error -> return@runCatching Outcome.Error(r.reason, r.cause)
            }

            val formats = YoutubeDL.getFormats(url).map {
                VideoSourceInfoImpl(it.asr, it.tbr, it.abr, it.format, it.formatId, it.formatNote, it.ext, it.vcodec, it.acodec, it.width, it.height, it.filesize, it.fps, it.url)
            }
            Outcome.Success(

                    YoutubeDLFFmpegRemoteAudioProvider(info, formats)
            )
        }.recover { e ->
            when (e) {
                is YoutubeDLException -> Outcome.Error(MusicService.ErrorReason.Unavailable(MusicService.ErrorReason.Unavailable.Reason.Unknown), e)
                else -> Outcome.Error(MusicService.ErrorReason.Unhandled, e)
            }
        }.getOrThrow()
    }

    override suspend fun playlist(id: String, accessToken: String?) = Outcome.Error(MusicService.ErrorReason.UnsupportedOperation, null)

    override suspend fun like(id: String, accessToken: String) = Outcome.Error(MusicService.ErrorReason.UnsupportedOperation, null)
}