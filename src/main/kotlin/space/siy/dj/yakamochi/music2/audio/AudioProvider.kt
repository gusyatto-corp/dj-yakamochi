package space.siy.dj.yakamochi.music2.audio

import space.siy.dj.yakamochi.music2.remote.RemoteAudioProvider
import java.nio.ShortBuffer

/**
 * オーディオサンプルを供給する
 * サンプル単位
 * @author SIY1121
 */
abstract class AudioProvider(protected val remote: RemoteAudioProvider) {
    abstract val duration: Int
    abstract val available: Int
    abstract var position: Int
    abstract val status: Status
    abstract fun start()
    abstract fun canRead(size: Int): Boolean
    abstract fun read(size: Int): ShortBuffer
    abstract fun release()

    private val fullLoadedListeners: MutableList<() -> Unit> = mutableListOf()

    fun addFullLoadedListener(block: () -> Unit) {
        fullLoadedListeners.add(block)
    }

    fun removeFullLoadedListener(block: () -> Unit) {
        fullLoadedListeners.remove { block }
    }

    protected fun notifyFullLoaded() = fullLoadedListeners.forEach { it() }

    enum class Status {
        Uninitialized, Working, End
    }
}