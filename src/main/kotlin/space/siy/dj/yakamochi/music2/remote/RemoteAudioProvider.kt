package space.siy.dj.yakamochi.music2.remote

import java.io.InputStream
import java.nio.ShortBuffer

/**
 * リモートにあるオーディオデータを取得、デコードする
 * @author SIY1121
 */
interface RemoteAudioProvider {
    companion object {
        fun create(url: String) = YoutubeDLFFmpegRemoteAudioProvider(url)
        fun create(inputStream: InputStream) = SimpleFFmpegRemoteAudioProvider(inputStream)
    }

    val source: String
    val format: String
    val estimateDuration: Float
    fun start()
    fun read(): ShortBuffer?
    fun release()
}