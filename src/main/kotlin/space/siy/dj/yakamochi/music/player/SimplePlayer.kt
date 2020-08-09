package space.siy.dj.yakamochi.music.player

import kotlinx.coroutines.runBlocking
import space.siy.dj.yakamochi.music.VideoInfo
import space.siy.dj.yakamochi.music.audio.AudioProvider
import space.siy.dj.yakamochi.music.audio.QueueAudioProvider
import space.siy.dj.yakamochi.music.secToSampleCount
import java.nio.ByteBuffer

/**
 * @author SIY1121
 */

/**
 * シンプルなプレイヤー実装
 */
@ExperimentalStdlibApi
class SimplePlayer(guildID: String) : Player<AudioProvider>(guildID) {
    override var status: Status = Status.Stop
    override val queue: List<VideoInfo>
        get() = emptyList()

    init {
        trackProviders.audioProviderCreator = {
            QueueAudioProvider(it)
        }
    }

    override suspend fun play() {
        if (nowPlayingTrack == null) nowPlayingTrack = requestTrack() ?: return
        status = Player.Status.Play
    }

    override suspend fun pause() {
        status = Player.Status.Pause
    }

    override suspend fun skip() {
        nowPlayingTrack = requestTrack()
    }

    override fun provide20MsAudio(): ByteBuffer? = runBlocking {
        val buf = ByteBuffer.allocate(0.02f.secToSampleCount() * Short.SIZE_BYTES)
        buf.asShortBuffer().put(nowPlayingTrack?.audioProvider?.read20Ms())

        // 曲が終了したら次の曲をリクエストする
        if (nowPlayingTrack?.audioProvider?.status == AudioProvider.Status.End) {
            doneTrack(nowPlayingTrack!!)
            nowPlayingTrack = requestTrack()
        }
        return@runBlocking buf
    }

    override fun canProvide() = status == Status.Play && nowPlayingTrack?.audioProvider?.canRead20Ms() ?: false

}