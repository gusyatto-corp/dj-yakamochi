package space.siy.dj.yakamochi.music2.audio

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import space.siy.dj.yakamochi.logger
import space.siy.dj.yakamochi.music2.remote.RemoteAudioProvider
import space.siy.dj.yakamochi.music2.sampleCountToSec
import space.siy.dj.yakamochi.music2.secToSampleCount
import space.siy.dj.yakamochi.music2.toArray
import java.util.concurrent.ArrayBlockingQueue
import kotlin.math.abs

/**
 * @author SIY1121
 */
class QueueGaplessAudioProvider(remote: RemoteAudioProvider) : QueueAudioProvider(remote) {
    val threthold = 500
    var retryCount = 0
    var startGap = 0

    override fun start() = scope.launch {
        logInfo("starting")
        remote.start()
        queue = ArrayBlockingQueue(15.secToSampleCount())
        duration = remote.estimateDuration.secToSampleCount()

        logInfo("queue size: ${queue.remainingCapacity().sampleCountToSec()}s")
        logInfo("estimate audio duration: ${duration.sampleCountToSec()}s")

        while (true) {
            val data = remote.read()?.toArray() ?: break
            if (status == Status.Uninitialized) {
                val i = data.indexOfFirst { it > threthold }
                if (i < 0) {
                    startGap += data.size
                    continue
                } else {
                    data.sliceArray(i until data.size).run {
                        forEach { queue.put(it) }
                        putPosition += size
                    }
                    status = Status.Active
                    startGap += i
                    logInfo("silent section has been detected at the start: ${startGap.sampleCountToSec()}s")
                }
            } else {
                data.run {
                    forEach { queue.put(it) }
                    putPosition += size
                }
            }
        }
        logInfo("loading completed at the end of audio")
        val i = queue.toList().reversed().indexOfFirst { it > threthold }
        duration = putPosition - i
        logInfo("silent section has been detected at the end: ${i.sampleCountToSec()}s")
        logInfo("audio duration updated: ${duration.sampleCountToSec()}s")
    }
}