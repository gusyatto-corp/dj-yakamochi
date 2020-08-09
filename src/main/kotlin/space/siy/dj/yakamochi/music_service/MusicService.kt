package space.siy.dj.yakamochi.music_service

import space.siy.dj.yakamochi.Outcome
import space.siy.dj.yakamochi.auth.AuthType
import space.siy.dj.yakamochi.music2.VideoInfo
import space.siy.dj.yakamochi.music2.remote.RemoteAudioProvider

/**
 * @author SIY1121
 */

/**
 * 外部音楽サービスを表し、連携する責務を持つ
 */
interface MusicService {

    /**
     * MusicServiceが発生させる可能性のあるエラーを表す
     */
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

    /**
     * リソースタイプ
     */
    enum class ResourceType {
        Video, Playlist, Unknown
    }

    /**
     * MusicServiceのID(一意
     */
    val id: String

    /**
     * このMusicServiceで使用できる認証タイプ
     * プライベートなデータにアクセスするときにはこのタイプでのログインが必要
     */
    val authType: AuthType

    /**
     * 与えられたurlがハンドルできるかを返す
     */
    fun canHandle(url: String): Boolean

    /**
     * 与えられたurlのリソースタイプを返す
     */
    fun resourceType(url: String): ResourceType

    /**
     * 与えられたワードで検索した結果を返す
     */
    suspend fun search(q: String): Outcome<List<VideoInfo>, ErrorReason>

    /**
     * 与えられたurl単体リソースの詳細を返す
     */
    suspend fun detail(url: String): Outcome<VideoInfo, ErrorReason>

    /**
     * 実際の音源を取得するためのRemoteAudioProviderを生成する
     */
    suspend fun source(url: String): Outcome<RemoteAudioProvider, ErrorReason>

    /**
     * 与えられたプレイリストの動画一覧を返す
     */
    suspend fun playlist(url: String, accessToken: String? = null): Outcome<List<VideoInfo>, ErrorReason>

    /**
     * 与えられたリソースをお気に入りに登録する
     */
    suspend fun like(url: String, userID: String): Outcome<Unit, ErrorReason>
}