package space.siy.dj.yakamochi.music.remote

import java.nio.ShortBuffer

/**
 * リモートにあるオーディオデータを取得、デコードする
 * @author SIY1121
 */
interface RemoteAudioProvider {
    val source: String
    val format: String
    val estimateDuration: Float
    suspend fun start()
    suspend fun read(): ShortBuffer?
    fun release()
}