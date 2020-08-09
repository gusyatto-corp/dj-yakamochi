package space.siy.dj.yakamochi.music.audio

import kotlinx.coroutines.launch
import space.siy.dj.yakamochi.music.remote.RemoteAudioProvider
import space.siy.dj.yakamochi.music.sampleCountToSec
import space.siy.dj.yakamochi.music.secToSampleCount
import space.siy.dj.yakamochi.music.toArray
import java.nio.ShortBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * @author SIY1121
 */

/**
 * ライブ配信の音源のストリーミングに特化したProvider
 * リアルタイム性を優先するため、キューがいっぱいになると古いものを破棄する
 * そのためまれに音が飛ぶ可能性がある
 */
class LiveQueueAudioProvider(remote: RemoteAudioProvider) : QueueAudioProvider(remote) {

    override fun start() = scope.launch {
        logInfo("starting")
        queue = ArrayBlockingQueue(10.secToSampleCount())
        remote.start()
        duration = Int.MAX_VALUE
        status = Status.Active
        logInfo("queue size: ${queue.remainingCapacity().sampleCountToSec()}s")
        logInfo("estimate audio duration: ${duration.sampleCountToSec()}s")
        while (status == Status.Active) {
            val data = remote.read() ?: break
            var dispose = 0
            data.toArray().forEach {
                // キューがいっぱいの場合は古いものを破棄
                if(!queue.offer(it, 5000, TimeUnit.MILLISECONDS)) {
                    queue.poll()
                    queue.offer(it)
                    dispose++
                }
            }
            if(dispose > 0)
                logInfo("${dispose.sampleCountToSec()}s audio were disposed")
        }
    }

    override fun read(size: Int): ShortBuffer {
        val buf = ShortBuffer.allocate(size)
        for (i in 0 until size) {
            buf.put(queue.poll() ?: break)
        }
        buf.position(0)
        return buf
    }
}