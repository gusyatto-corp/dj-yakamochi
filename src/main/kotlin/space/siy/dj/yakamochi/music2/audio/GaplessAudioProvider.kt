package space.siy.dj.yakamochi.music2.audio

import space.siy.dj.yakamochi.music2.remote.RemoteAudioProvider
import space.siy.dj.yakamochi.music2.sampleCountToSec
import space.siy.dj.yakamochi.music2.secToSampleCount
import space.siy.dj.yakamochi.music2.toArray
import java.nio.ShortBuffer
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread
import kotlin.math.abs

/**
 * @author SIY1121
 * 音楽の前後無音部分をカットして
 */
@ExperimentalStdlibApi
class GaplessAudioProvider(remote: RemoteAudioProvider) : SimpleAudioProvider(remote) {

    var startGap = 0
    var endGap = 0
    var startGapDetected = false
    val gapThrethold = 300

    override fun start() {
        thread {
            startSync()
            synchronized(buffer) {
                for (scanPos in duration - 1 downTo 0) {
                    if (abs(buffer.get(scanPos).toInt()) > gapThrethold) {
                        endGap = (buffer.limit() - scanPos)
                        break
                    }
                }
                println("end gap ${endGap.sampleCountToSec()}")
                duration -= startGap + endGap
            }
            notifyFullLoaded()
        }
    }

    override fun startSync() {
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
                startGapDetected = true
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

                if (!startGapDetected) {
                    data.position(0)
                    val i = data.toArray().indexOfFirst { it > gapThrethold }
                    if (i >= 0) {
                        startGap = putPosition + i
                        startGapDetected = true
                        println("start gap: ${startGap.sampleCountToSec()}")
                    }
                }

                putPosition = buffer.position()
            }
        }
        println("done $putPosition")
        duration = putPosition
        duration = putPosition
    }

    override fun canRead(size: Int) =
            if (status != Status.Working || !startGapDetected) false
            else if
            // 最後の端数のときは読みとりOKを出す
                         (!isLive && (duration + startGap - position) < size) true
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
                buffer.position(position + startGap)
                val req = size.coerceAtMost(duration - position)
                val arr = ShortArray(req)
                buffer.get(arr)
                res.put(arr)
                position = buffer.position() - startGap
                if (position >= duration)
                    status = Status.End
            }
        }
        res.position(0)
        return res
    }
}