package space.siy.dj.yakamochi.music2.audio

import space.siy.dj.yakamochi.music2.remote.RemoteAudioProvider
import space.siy.dj.yakamochi.music2.sampleCountToSec
import space.siy.dj.yakamochi.music2.secToSampleCount
import kotlin.concurrent.thread
import kotlin.math.abs

/**
 * @author SIY1121
 */
@ExperimentalStdlibApi
class GaplessAudioProvider(remote: RemoteAudioProvider) : SimpleAudioProvider(remote) {

    var startGap = 0
    var endGap = 0

    override var position: Int
        get() = super.position - startGap
        set(value) {
            super.position = value + startGap
        }

    override fun start() {
        thread {
            startSync()
            synchronized(buffer) {
                for (scanPos in 0 until buffer.limit()) {
                    if (abs(buffer.get(scanPos).toInt()) > 1000) {
                        startGap = scanPos
                        break
                    }
                }
                println("start gap: ${startGap.sampleCountToSec()}")
                for (scanPos in duration - 1 downTo 0) {
                    if (abs(buffer.get(scanPos).toInt()) > 1000) {
                        endGap = (buffer.limit() - scanPos)
                        break
                    }
                }
                println("end gap ${endGap.sampleCountToSec()}")
                duration -= startGap + endGap
                position = 0
            }
            notifyFullLoaded()
        }
    }
}