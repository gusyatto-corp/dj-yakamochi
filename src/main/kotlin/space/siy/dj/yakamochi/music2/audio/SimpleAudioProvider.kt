package space.siy.dj.yakamochi.music2.audio

import space.siy.dj.yakamochi.music2.remote.RemoteAudioProvider
import space.siy.dj.yakamochi.music2.sampleCountToSec
import space.siy.dj.yakamochi.music2.secToSampleCount
import java.nio.ShortBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * @author SIY1121
 */
@ExperimentalStdlibApi
open class SimpleAudioProvider(remote: RemoteAudioProvider) : AudioProvider(remote) {
    override var duration = -1
    override val available
        get() = if (isLive) queue.size
        else putPosition - position
    override var position: Int = 0

    protected var isLive = false

    protected var putPosition = 0
    protected lateinit var buffer: ShortBuffer

    protected lateinit var queue: ArrayBlockingQueue<Short>

    override var status = Status.Uninitialized

    override fun start() {
        thread {
            startSync()
            notifyFullLoaded()
        }
    }

    protected open fun startSync() {
        remote.start()
        duration = remote.estimateDuration.secToSampleCount()
        duration = remote.estimateDuration.secToSampleCount()
        isLive = remote.estimateDuration == 0f
        if (isLive) queue = ArrayBlockingQueue(10.secToSampleCount())
        else buffer = ShortBuffer.allocate(remote.estimateDuration.secToSampleCount())
        status = Status.Working
        while (true) {
            val data = remote.read() ?: break
            if (isLive) synchronized(queue) {
                val arr = ShortArray(data.limit())
                data.get(arr)
                arr.forEach {
                    if (!queue.offer(it)) {
                        queue.poll()
                        queue.offer(it)
                    }
                }
            }
            else synchronized(buffer) {
                buffer.position(putPosition)
                buffer.put(data)
                putPosition = buffer.position()
            }
        }
        println("done $putPosition")
        duration = putPosition
    }

    override fun canRead(size: Int) =
            if (status != Status.Working) false
            else if
            // 最後の端数のときは読みとりOKを出す
                         (!isLive && (duration - position) < size) true
            else if (isLive)
                size <= queue.size
            else size <= available

    override fun read(size: Int): ShortBuffer {
        val res = ShortBuffer.allocate(size)
        if (isLive) {
            synchronized(queue) {
                for (i in 0 until size)
                    res.put(queue.poll())
            }
        } else {
            synchronized(buffer) {
                buffer.position(position)
                val req = size.coerceAtMost(duration - position)
                val arr = ShortArray(req)
                buffer.get(arr)
                res.put(arr)
                position = buffer.position()

                if (position == duration)
                    status = Status.End
            }
        }
        res.position(0)
        return res
    }

    override fun release() {
        status = Status.Uninitialized
        remote.release()
    }
}