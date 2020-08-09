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

/**
 * 曲の頭と最後の無音を取り除いた音源を提供する
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
        status = Status.Active
        logInfo("queue size: ${queue.remainingCapacity().sampleCountToSec()}s")
        logInfo("estimate audio duration: ${duration.sampleCountToSec()}s")
        var startGapDetected = false
        while (status == Status.Active) {
            val data = remote.read()?.toArray() ?: break
            // 頭の無音が検出されていない場合
            if (!startGapDetected) {
                val i = data.indexOfFirst { it > threthold }
                if (i < 0) {
                    startGap += data.size
                    continue
                } else { // 初めてしきい値を超えるサンプルが出現した場合、頭の無音区間は検出終了
                    data.sliceArray(i until data.size).run {
                        forEach { queue.put(it) }
                        putPosition += size
                    }
                    startGapDetected = true
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
        // 曲の最後までサンプルを読み込んだ際、後ろから無音区間を検出する
        val i = queue.toList().reversed().indexOfFirst { it > threthold }
        duration = putPosition - i
        logInfo("silent section has been detected at the end: ${i.sampleCountToSec()}s")
        logInfo("audio duration updated: ${duration.sampleCountToSec()}s")
    }
}