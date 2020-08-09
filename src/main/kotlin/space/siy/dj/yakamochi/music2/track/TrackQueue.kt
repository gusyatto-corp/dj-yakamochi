package space.siy.dj.yakamochi.music2.track

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import space.siy.dj.yakamochi.Outcome
import space.siy.dj.yakamochi.database.TrackHistoryRepository
import space.siy.dj.yakamochi.music2.audio.AudioProvider
import space.siy.dj.yakamochi.music2.remote.RemoteAudioProvider
import space.siy.dj.yakamochi.music_service.MusicService

/**
 * @author SIY1121
 */

/**
 * TrackProviderのQueue実装
 * 一般にユーザーのリクエスをキューに格納しておいて
 * リクエストに応じてpopするのに使用する
 */
@ExperimentalStdlibApi
class TrackQueue<T : AudioProvider>(val guildID: String) : TrackProvider<T>, KoinComponent {

    /**
     * TrackQueueが発生させる可能性のあるエラーを表す
     */
    sealed class ErrorReason {
        data class MusicServiceError(val reason: MusicService.ErrorReason) : ErrorReason()
        object Unhandled : ErrorReason()
    }

    val queue = ArrayDeque<Track<T>>()
    val trackHistoryRepository by inject<TrackHistoryRepository>()

    /**
     * データベースからまだ再生されていないトラックをキューにpushする
     * 強制終了等で過去にリクエストされたがキューから消失しているときに有効
     * 初期化時に呼び出す
     */
    suspend fun loadFromHistory() {
        trackHistoryRepository.listAll(guildID, false).map { history ->
            val track = Track.fromHistory<T>(history)
            queue.add(track)
            track
        }
    }

    /**
     * 与えられた曲をpushする
     */
    suspend fun queue(url: String, author: String, guild: String) = withContext(Dispatchers.IO) {
        return@withContext runCatching<Outcome<Track<T>, ErrorReason>> {
            if (audioProviderCreator == null) throw Exception("AudioProviderCreator is not set")

            val track = when (val r = Track.newTrack<T>(url, author, guild)) {
                is Outcome.Success -> r.result
                is Outcome.Error -> when (r.reason) {
                    is Track.ErrorReason.MusicServiceError -> return@runCatching Outcome.Error(ErrorReason.MusicServiceError(r.reason.reason), r.cause)
                    is Track.ErrorReason.Unhandled -> return@runCatching Outcome.Error(ErrorReason.Unhandled, r.cause)
                }
            }.apply {
                queue.add(this)
            }

            Outcome.Success(track)
        }.recover { Outcome.Error(ErrorReason.Unhandled, it) }
                .getOrThrow()
    }

    override fun canProvide() = queue.size > 0

    override var audioProviderCreator: ((remoteAudioProvider: RemoteAudioProvider) -> T)? = null

    override suspend fun requestTrack() = runCatching {
        // 曲を提供できない場合はNoTrackを返却
        if (queue.isEmpty()) return@runCatching Outcome.Error(TrackProvider.ErrorReason.NoTrack, null)

        val res = queue.removeFirst().apply {

            // AudioProviderが初期化されてなければする
            if (!audioInitialized) {
                val r = prepareAudio { audioProviderCreator!!(it) }
                if (r is Outcome.Error) {
                    remove() // 再生できなかったトラックはなかったことにする
                    when (r.reason) {
                        is Track.ErrorReason.MusicServiceError -> return@runCatching Outcome.Error(TrackProvider.ErrorReason.MusicServiceError(r.reason.reason), r.cause)
                        else -> return@runCatching Outcome.Error(TrackProvider.ErrorReason.Unhandled, r.cause)
                    }
                }
            }
        }
        return@runCatching Outcome.Success(res)
    }.recover { Outcome.Error(TrackProvider.ErrorReason.Unhandled, it) }
            .getOrThrow()
}