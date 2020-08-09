package space.siy.dj.yakamochi.music.track

import space.siy.dj.yakamochi.Outcome
import space.siy.dj.yakamochi.music.VideoInfo
import space.siy.dj.yakamochi.music.audio.AudioProvider
import space.siy.dj.yakamochi.music.remote.RemoteAudioProvider
import space.siy.dj.yakamochi.music.service.MusicService
import space.siy.dj.yakamochi.music.service.MusicServiceManager

/**
 * @author SIY1121
 */

/**
 * 与えられたプレイリストリソースからトラックを供給する
 */
class PlaylistTrackProvider<T : AudioProvider>(val guildID: String) : TrackProvider<T> {

    /**
     * PlaylistTrackProviderが発生させる可能性のあるエラーを表す
     */
    sealed class ErrorReason {
        data class MusicServiceError(val reason: MusicService.ErrorReason) : ErrorReason()
        object Unhandled : ErrorReason()
    }

    override var audioProviderCreator: ((remoteAudioProvider: RemoteAudioProvider) -> T)? = null

    private var playlistItems: List<VideoInfo>? = null
    private var author: String? = null

    private var pos = 0

    var random = false
    var repeat = false

    /**
     * 各トラックが再生済みかどうかのフラグ
     */
    var playMap: BooleanArray? = null

    private var doneCallback: (() -> Unit)? = null

    /**
     * プレイリストをクリアする
     */
    fun clearPlaylist() {
        playlistItems = null
        author = null
        random = false
        repeat = false
        doneCallback?.invoke()
        doneCallback = null
    }

    /**
     * プレイリストを設定する
     * 以降、このプレイリスト内からトラックが供給される
     */
    suspend fun setPlaylist(url: String, author: String, doneCallback: (() -> Unit)? = null): Outcome<Unit, ErrorReason> {
        if (playlistItems != null) clearPlaylist()
        playlistItems = when (val r = MusicServiceManager.playlist(url)) {
            is Outcome.Success -> r.result
            is Outcome.Error -> return Outcome.Error(ErrorReason.MusicServiceError(r.reason), r.cause)
        }

        playMap = BooleanArray(playlistItems!!.size) { false }
        this.author = author
        this.doneCallback = doneCallback

        return Outcome.Success(Unit)
    }

    override fun canProvide() = playlistItems != null

    override suspend fun requestTrack() = runCatching<Outcome<Track<T>, TrackProvider.ErrorReason>> {
        val playlistItems = this.playlistItems
                ?: return@runCatching Outcome.Error(TrackProvider.ErrorReason.NoTrack, null)

        //まだ再生していない曲を取得
        var targets = playlistItems.filterIndexed { index, _ -> !(playMap?.get(index) ?: true) }
        when {
            // すべての曲を再生し終えて、かつリピート有効時はすべての曲を未再生状態に戻す
            targets.isEmpty() && repeat -> {
                playMap = playMap?.map { false }?.toBooleanArray()
                targets = playlistItems
            }
            // すべての曲を再生し終えて、かつリピート無効時はプレイリストをクリア
            targets.isEmpty() && !repeat -> {
                clearPlaylist()
                return@runCatching Outcome.Error(TrackProvider.ErrorReason.NoTrack, null)
            }
        }

        // 再生するトラックを決定する
        val track = if (random)
            targets.random()
        else
            targets.first()

        // 該当トラックの再生済みフラグを立てる
        playMap?.set(playlistItems.indexOf(track), true)

        val res = when (val r = Track.newTrack<T>(track.url, author!!, guildID)) {
            is Outcome.Success -> r.result
            // トラック生成時のエラー処理
            is Outcome.Error -> return@runCatching when (r.reason) {
                is Track.ErrorReason.MusicServiceError -> Outcome.Error(TrackProvider.ErrorReason.MusicServiceError(r.reason.reason), r.cause)
                is Track.ErrorReason.Unhandled -> Outcome.Error(TrackProvider.ErrorReason.Unhandled, r.cause)
            }
        }.apply {
            //AudioProvider初期化
            val r = prepareAudio { audioProviderCreator!!(it) }
            // 初期化に失敗した場合はデータベースから消去する
            if (r is Outcome.Error) {
                remove()
                return@runCatching when (r.reason) {
                    is Track.ErrorReason.MusicServiceError -> Outcome.Error(TrackProvider.ErrorReason.MusicServiceError(r.reason.reason), r.cause)
                    is Track.ErrorReason.Unhandled -> Outcome.Error(TrackProvider.ErrorReason.Unhandled, r.cause)
                }
            }
        }

        Outcome.Success(res)
    }.recover {
        Outcome.Error(TrackProvider.ErrorReason.Unhandled, it)
    }.getOrThrow()
}