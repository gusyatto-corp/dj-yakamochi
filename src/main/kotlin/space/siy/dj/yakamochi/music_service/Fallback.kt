package space.siy.dj.yakamochi.music_service

import com.sapher.youtubedl.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.siy.dj.yakamochi.auth.AuthType
import space.siy.dj.yakamochi.music2.VideoInfo
import space.siy.dj.yakamochi.music2.VideoSourceInfo
import space.siy.dj.yakamochi.music2.VideoSourceInfoImpl
import space.siy.dj.yakamochi.music2.remote.RemoteAudioProvider
import space.siy.dj.yakamochi.music2.remote.YoutubeDLFFmpegRemoteAudioProvider

/**
 * @author SIY1121
 */
class Fallback : MusicService {
    override val id = "fallback"
    override val authType = AuthType.None

    data class VideoInfoImpl(override val url: String, override val title: String, override val duration: Float, override val thumbnail: String) : VideoInfo

    override fun canHandle(url: String) = true
    override fun resourceType(url: String) = MusicService.ResourceType.Video

    override suspend fun search(q: String): List<VideoInfo> {
        TODO("Not yet implemented")
    }

    override suspend fun detail(url: String) = withContext(Dispatchers.IO) {
        try {
            val res = YoutubeDL.getVideoInfo(url)
            return@withContext VideoInfoImpl(
                    url, res.title, res.duration.toFloat(), res.thumbnail
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    override suspend fun source(url: String): RemoteAudioProvider? = withContext(Dispatchers.IO) {
        val info = detail(url) ?: throw Exception("トラックの情報を取得できませんでした")
        return@withContext try {
            val formats = YoutubeDL.getFormats(url).map {
                VideoSourceInfoImpl(it.asr, it.tbr, it.abr, it.format, it.formatId, it.formatNote, it.ext, it.vcodec, it.acodec, it.width, it.height, it.filesize, it.fps, it.url)
            }
            YoutubeDLFFmpegRemoteAudioProvider(info, formats)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun playlist(id: String, accessToken: String?): List<VideoInfo> {
        TODO("Not yet implemented")
    }

    override suspend fun like(id: String, accessToken: String) {
        TODO("Not yet implemented")
    }
}