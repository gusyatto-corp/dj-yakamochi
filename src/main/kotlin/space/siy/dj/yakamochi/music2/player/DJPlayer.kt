package space.siy.dj.yakamochi.music2.player

import kotlinx.coroutines.runBlocking
import space.siy.dj.yakamochi.music2.VideoInfo
import space.siy.dj.yakamochi.music2.audio.AnalyzedAudioProvider
import space.siy.dj.yakamochi.music2.audio.AudioProvider
import space.siy.dj.yakamochi.music2.audio.QueueAudioProvider
import space.siy.dj.yakamochi.music2.secToSampleCount
import space.siy.dj.yakamochi.music2.toArray
import space.siy.dj.yakamochi.music2.track.Track
import java.nio.ByteBuffer

/**
 * @author SIY1121
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

    override suspend fun play() {
        if (nowPlayingTrack == null) nowPlayingTrack = trackProviders.requestTrack()
        status = Player.Status.Play
    }

    override suspend fun pause() {
        TODO("Not yet implemented")
    }

    override suspend fun skip() {
        TODO("Not yet implemented")
    }

    override fun provide20MsAudio(): ByteBuffer = runBlocking {
        val buf = ByteBuffer.allocate(0.02f.secToSampleCount() * Short.SIZE_BYTES)
        val nowProvider = nowPlayingTrack?.audioProvider ?: return@runBlocking buf
        if (nowProvider.endPos - nowProvider.position < 20.secToSampleCount() && nextTrack == null) {
            nextTrack = trackProviders.requestTrack()
        }
        val pos = nowProvider.position
        var arr = nowProvider.read20Ms().toArray().map { (it * 0.5f).toShort() }.toShortArray()
        if (nextTrack != null) {
            val nextProvider = nextTrack!!.audioProvider!!
            val startCrossPos = nowProvider.endPos - (nextProvider.startPos - nextProvider.startFadePos)
            val requiredNextSample = (arr.size - (startCrossPos - pos)).coerceAtMost(0.02.secToSampleCount())
            if (requiredNextSample > 0) {
                val nextarr = nextProvider.read(requiredNextSample).toArray().map { (it * 0.5f).toShort() }.toShortArray()
                if (startCrossPos <= pos + arr.size) {
                    for (i in (startCrossPos - pos).coerceAtLeast(0) until arr.size) {
                        arr[i] = (arr[i] + nextarr[i - (startCrossPos - pos).coerceAtLeast(0)]).toShort()
                    }
                }
            }
        }
//         test
//        if (pos > 15.secToSampleCount() + nowProvider.startPos && pos < 20.secToSampleCount() + nowProvider.startPos) {
//            nowProvider.seek(nowProvider.endPos - 25.secToSampleCount())
//        }

        if (nowProvider.status == AudioProvider.Status.End) {
            nowPlayingTrack?.done()
            nowPlayingTrack = nextTrack
            nextTrack = null
        }
//        val requiredSample = 0.02f.secToSampleCount().coerceAtMost(nowProvider.endPos - nowProvider.position)
//        var arr = nowProvider.read(requiredSample).toArray()
//        if (requiredSample < 0.02f.secToSampleCount()) {
//            val requiredNextSample = 0.02f.secToSampleCount() - (nowProvider.endPos - nowProvider.position)
//            nextTrack?.audioProvider?.seek(nextTrack?.audioProvider?.startPos ?: 0)
//            arr += nextTrack?.audioProvider?.read(requiredNextSample)?.toArray() ?: ShortArray(requiredNextSample)
//            nowPlayingTrack = nextTrack
//            nextTrack = null
//        }
        buf.asShortBuffer().put(arr)
        return@runBlocking buf
    }

    override fun canProvide() = status == Status.Play && nowPlayingTrack?.audioProvider?.canRead20Ms() ?: false
}