package space.siy.dj.yakamochi.music

import kotlinx.coroutines.Job

/**
 * @author SIY1121
 */
abstract class TrackDataProvider(val sourceUrl: String) {
    val sampleRate = 48000
    val channelCount = 2

    abstract val isLive: Boolean
    abstract val loadProgress: Float
    abstract val trackInfo: Info
    abstract val status: Status
    abstract val providedPosition: Float
    abstract fun canRead(size: Int): Boolean
    abstract fun canRead(range: IntRange): Boolean
    abstract fun read(size: Int): ShortArray
    abstract fun read(range: IntRange): ShortArray
    abstract fun load(): Job

    data class Info(val url: String, val title: String, val duration: Float)
    enum class Status {
        UnInitialized, Ready ,End
    }
}