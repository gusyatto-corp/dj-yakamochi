package space.siy.dj.yakamochi.music_service

import com.sapher.youtubedl.YoutubeDL
import com.sapher.youtubedl.YoutubeDLException
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.ClientRequestException
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.*
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import space.siy.dj.yakamochi.Outcome
import space.siy.dj.yakamochi.auth.AuthType
import space.siy.dj.yakamochi.database.AuthProvider
import space.siy.dj.yakamochi.database.AuthRepository
import space.siy.dj.yakamochi.music2.VideoInfo
import space.siy.dj.yakamochi.music2.VideoSourceInfoImpl
import space.siy.dj.yakamochi.music2.remote.RemoteAudioProvider
import space.siy.dj.yakamochi.music2.remote.YoutubeDLFFmpegRemoteAudioProvider

/**
 * @author SIY1121
 */
class Youtube : MusicService, KoinComponent {
    val authRepository: AuthRepository by inject()
    override val authType = AuthType.Google
    override val id = "youtube"

    override fun canHandle(url: String) = url.contains(Regex("^https:\\/\\/(youtu\\.be|www\\.youtube\\.com)"))
    override fun resourceType(url: String) = when {
        url.matchYoutubePlaylistID() != null -> MusicService.ResourceType.Playlist
        url.youtubeVideoID() != null -> MusicService.ResourceType.Video
        else -> MusicService.ResourceType.Unknown
    }

    private val API_KEY = System.getenv("GOOGLE_API_KEY")

    data class YoutubePagerResponse<T>(val kind: String, val nextPageToken: String?, val prevPageToken: String, val pageInfo: PageInfo, val items: Array<T>) {
        data class PageInfo(val totalResults: Int, val resultsPerPage: Int)
    }

    data class VideoResource(val kind: String, val id: String, val snippet: Snippet, val contentDetails: ContentDetails) {
        data class Snippet(val channelId: String, val title: String, val description: String, val thumbnails: Map<String, Thumbnail>, val channelTitle: String, val tags: Array<String>) {
            data class Thumbnail(val url: String, val width: Int, val height: Int)
        }

        data class ContentDetails(val duration: String)
    }

    data class PlaylistItemResource(val kind: String, val id: String, val snippet: Snippet) {
        data class Snippet(val channelId: String, val title: String, val description: String, val thumbnails: Map<String, Thumbnail>, val channelTitle: String, val position: Int, val resourceId: ResourceID) {
            data class Thumbnail(val url: String, val width: Int, val height: Int)
            data class ResourceID(val kind: String, val videoId: String)
        }
    }

    data class VideoInfoImpl(override val url: String, override val title: String, override val duration: Float, override val thumbnail: String) : VideoInfo

    private val client = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }
        defaultRequest {
            parameter("key", API_KEY)
        }
    }

    private val userClient = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }
    }

    override suspend fun search(q: String) = Outcome.Error(MusicService.ErrorReason.UnsupportedOperation, null)

    override suspend fun detail(url: String): Outcome<VideoInfo, MusicService.ErrorReason> = withContext(Dispatchers.IO) {
        return@withContext runCatching<Outcome<VideoInfo, MusicService.ErrorReason>> {
            val videoID = url.youtubeVideoID()
                    ?: return@withContext Outcome.Error(MusicService.ErrorReason.UnsupportedResource, null)

            val res = client.get<YoutubePagerResponse<VideoResource>>("https://www.googleapis.com/youtube/v3/videos") {
                parameter("part", "id,snippet,contentDetails")
                parameter("id", videoID)
            }.items.firstOrNull() ?: return@withContext Outcome.Error(MusicService.ErrorReason.Unavailable(MusicService.ErrorReason.Unavailable.Reason.NotFound), null)

            Outcome.Success(
                    VideoInfoImpl(
                            "https://www.youtube.com/watch?v=$videoID",
                            res.snippet.title,
                            res.contentDetails.duration.toSec().toFloat(),
                            res.snippet.thumbnails.entries.lastOrNull()?.value?.url ?: ""
                    )
            )
        }.recover { e ->
            when (e) {
                is ClientRequestException -> e.toOutcomeError()
                else -> Outcome.Error(MusicService.ErrorReason.Unhandled, e)
            }
        }.getOrThrow()
    }

    override suspend fun source(url: String): Outcome<RemoteAudioProvider, MusicService.ErrorReason> = withContext(Dispatchers.IO) {
        return@withContext runCatching<Outcome<RemoteAudioProvider, MusicService.ErrorReason>> {
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

    override suspend fun playlist(url: String, accessToken: String?): Outcome<List<VideoInfo>, MusicService.ErrorReason> = withContext(Dispatchers.IO) {
        return@withContext runCatching<Outcome<List<VideoInfo>, MusicService.ErrorReason>> {
            val playlistID = url.matchYoutubePlaylistID()
                    ?: return@runCatching Outcome.Error(MusicService.ErrorReason.UnsupportedResource, null)

            val res = ArrayList<VideoInfo>()
            var nextPageToken: String? = null
            while (true) {
                val r = client.get<YoutubePagerResponse<PlaylistItemResource>>("https://www.googleapis.com/youtube/v3/playlistItems") {
                    parameter("part", "snippet")
                    parameter("playlistId", playlistID)
                    parameter("maxResults", "50")
                    if (nextPageToken != null)
                        parameter("pageToken", nextPageToken)
                }
                res.addAll(r.items.map {
                    VideoInfoImpl(
                            "https://www.youtube.com/watch?v=${it.snippet.resourceId.videoId}",
                            it.snippet.title,
                            -1f,
                            it.snippet.thumbnails.entries.lastOrNull()?.value?.url ?: ""
                    )
                })
                if (r.nextPageToken == null) break
                nextPageToken = r.nextPageToken
            }

            Outcome.Success(res)
        }.recover { e ->
            when (e) {
                is ClientRequestException -> e.toOutcomeError()
                else -> Outcome.Error(MusicService.ErrorReason.Unhandled, e)
            }
        }.getOrThrow()
    }

    override suspend fun like(url: String, userID: String) = runCatching<Outcome<Unit, MusicService.ErrorReason>> {
        val auth = authRepository.find(userID, AuthProvider.Google)
                ?: return Outcome.Error(MusicService.ErrorReason.Unauthorized(AuthType.Google), null)
        userClient.post<String>("https://www.googleapis.com/youtube/v3/videos/rate") {
            header("Authorization", "Bearer ${auth.accessToken}")
            parameter("id", url.youtubeVideoID())
            parameter("rating", "like")
        }
        Outcome.Success(Unit)
    }.recover {
        Outcome.Error(MusicService.ErrorReason.Unhandled, it)
    }.getOrThrow()

    private fun String.toSec() = Regex("PT((\\d*?)M)?((\\d*?)S)?").find(this)?.run {
        ((groupValues[2].toIntOrNull() ?: 0) * 60) + (groupValues[4].toIntOrNull() ?: 0)
    } ?: 0

    private fun ClientRequestException.toOutcomeError() = when (response.status) {
        HttpStatusCode.NotFound -> Outcome.Error(MusicService.ErrorReason.Unavailable(MusicService.ErrorReason.Unavailable.Reason.NotFound), this)
        HttpStatusCode.Forbidden -> Outcome.Error(MusicService.ErrorReason.Unavailable(MusicService.ErrorReason.Unavailable.Reason.Forbidden), this)
        else -> Outcome.Error(MusicService.ErrorReason.Unavailable(MusicService.ErrorReason.Unavailable.Reason.Unknown), this)
    }

    private fun String.youtubeVideoID() =
            Regex("[?&]v=(.*?)(?=(\$|&))").find(this)?.groupValues?.get(1)
                    ?: Regex("youtu\\.be\\/(.*?)\$").find(this)?.groupValues?.get(1)

    private fun String.matchYoutubePlaylistID() = Regex("playlist\\?list=(.*?)(?=(\$|&))").find(this)?.groupValues?.get(1)
}