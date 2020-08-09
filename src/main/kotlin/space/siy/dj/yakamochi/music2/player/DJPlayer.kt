package space.siy.dj.yakamochi.music2.player

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import space.siy.dj.yakamochi.Outcome
import space.siy.dj.yakamochi.music2.VideoInfo
import space.siy.dj.yakamochi.music2.audio.AnalyzedAudioProvider
import space.siy.dj.yakamochi.music2.audio.AudioProvider
import space.siy.dj.yakamochi.music2.audio.QueueAudioProvider
import space.siy.dj.yakamochi.music2.secToSampleCount
import space.siy.dj.yakamochi.music2.toArray
import space.siy.dj.yakamochi.music2.track.Track
import space.siy.dj.yakamochi.music2.track.TrackProvider
import java.nio.ByteBuffer

/**
 * @author SIY1121
 */

/**
 * DJのように再生してくれるプレイヤー
 */
@ExperimentalStdlibApi
class DJPlayer(guildID: String) : Player<AnalyzedAudioProvider>(guildID) {
    override var status: Status = Status.Stop
    override val queue: List<VideoInfo>
        get() = emptyList()

    init {
        trackProviders.audioProviderCreator = {
            AnalyzedAudioProvider(it)
        }
    }

    var nextTrack: Track<AnalyzedAudioProvider>? = null
    var requestedNext = false

    override suspend fun play() {
        if (nowPlayingTrack == null)
            nowPlayingTrack = requestTrack() ?: return
        status = Status.Play
    }

    override suspend fun pause() {
        TODO("Not yet implemented")
    }

    override suspend fun skip() {
        val tmp = nowPlayingTrack ?: return
        nowPlayingTrack = null
        doneTrack(tmp)
        nowPlayingTrack = requestTrack() ?: return
    }

    override fun provide20MsAudio(): ByteBuffer = runBlocking {
        val buf = ByteBuffer.allocate(0.02f.secToSampleCount() * Short.SIZE_BYTES)
        val nowProvider = nowPlayingTrack?.audioProvider ?: return@runBlocking buf

        // 現在の曲終了20秒前に次トラックのリクエストを開始する
        if (nowProvider.endPos - nowProvider.position < 20.secToSampleCount() && nextTrack == null && !requestedNext && trackProviders.canProvide()) {
            requestedNext = true
            GlobalScope.launch {
                nextTrack = requestTrack()
                requestedNext = false
            }
        }
        val pos = nowProvider.position
        var arr = nowProvider.read20Ms().toArray().map { (it * 0.5f).toShort() }.toShortArray()

        // 次のトラックの準備ができている場合
        if (nextTrack != null && nextTrack?.audioProvider?.canRead20Ms() == true) {
            val nextProvider = nextTrack!!.audioProvider!!
            // 次のトラックの再生を開始するタイミング（現在の曲の再生時間を基準に）
            val startCrossPos = nowProvider.endPos - (nextProvider.startPos - nextProvider.startFadePos)

            val requiredNextSample = (arr.size - (startCrossPos - pos)).coerceAtMost(0.02.secToSampleCount())
            // 次のトラックのサンプルをオーバーする必要がある場合
            if (requiredNextSample > 0) {
                val nextarr = nextProvider.read(requiredNextSample).toArray().map { (it * 0.5f).toShort() }.toShortArray()
                if (startCrossPos <= pos + arr.size) {
                    for (i in (startCrossPos - pos).coerceAtLeast(0) until arr.size) {
                        // 前の曲のサンプルに次の曲のサンプルを加算
                        arr[i] = (arr[i] + nextarr[i - (startCrossPos - pos).coerceAtLeast(0)]).toShort()
                    }
                }
            }
        }
//        test つなぎを確認するテストコード（最初と最後の20秒のみを再生）
//        if (pos > 15.secToSampleCount() + nowProvider.startPos && pos < 20.secToSampleCount() + nowProvider.startPos) {
//            nowProvider.seek(nowProvider.endPos - 25.secToSampleCount())
//        }

        // 再生中の曲が完了した場合次の曲をメインに切り替える
        if (nowProvider.status == AudioProvider.Status.End) {
            doneTrack(nowPlayingTrack!!)
            nowPlayingTrack = nextTrack
            nextTrack = null
        }
        buf.asShortBuffer().put(arr)
        return@runBlocking buf
    }

    override fun canProvide() = status == Status.Play && nowPlayingTrack?.audioProvider?.canRead20Ms() ?: false
}