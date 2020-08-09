package space.siy.dj.yakamochi.music2.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.siy.dj.yakamochi.music2.remote.RemoteAudioProvider
import space.siy.dj.yakamochi.music2.sampleCountToSec
import space.siy.dj.yakamochi.music2.secToSampleCount
import space.siy.dj.yakamochi.music2.toArray
import java.nio.ShortBuffer
import java.util.concurrent.ArrayBlockingQueue

/**
 * @author SIY1121
 */

/**
 * リモートの音源をキューしながら供給する
 * いわゆるストリーミング再生状態を実現する
 */
open class QueueAudioProvider(remote: RemoteAudioProvider) : AudioProvider(remote) {

    override var duration = 0
    override val available: Int
        get() = queue.size
    override var position: Int = 0
    override var status = Status.Uninitialized

    lateinit var queue: ArrayBlockingQueue<Short>

    protected var putPosition = 0

    override fun start() = scope.launch {
        logInfo("starting")
        queue = ArrayBlockingQueue(10.secToSampleCount())
        remote.start()
        duration = remote.estimateDuration.secToSampleCount()
        status = Status.Active
        logInfo("queue size: ${queue.remainingCapacity().sampleCountToSec()}s")
        logInfo("estimate audio duration: ${duration.sampleCountToSec()}s")
        while (status == Status.Active) {
            val data = remote.read() ?: break
            data.toArray().run {
                forEach { queue.put(it) }
                putPosition += size
            }
        }
        duration = putPosition
    }

    override fun canRead(size: Int) = when {
        status != Status.Active -> false
        duration - position < size -> true
        else -> queue.size >= size
    }

    override fun read(size: Int): ShortBuffer {
        val buf = ShortBuffer.allocate(size)
        for (i in 0 until size) {
            buf.put(queue.poll() ?: break)
        }
        position += size
        if (position >= duration)
            status = Status.End
        buf.position(0)
        return buf
    }

    override fun seek(position: Int) {
        TODO("Not yet implemented")
    }

    override fun release() {
        status = Status.Uninitialized
        remote.release()
        queue.clear()
    }
}