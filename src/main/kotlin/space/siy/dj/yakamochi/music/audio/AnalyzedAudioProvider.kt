package space.siy.dj.yakamochi.music.audio

import kotlinx.coroutines.launch
import space.siy.dj.yakamochi.music.*
import space.siy.dj.yakamochi.music.effect.*
import space.siy.dj.yakamochi.music.remote.RemoteAudioProvider
import space.siy.music_analyzer_kt.Event
import space.siy.music_analyzer_kt.MusicAnalyzer
import java.nio.ShortBuffer

/**
 * @author SIY1121
 */
class AnalyzedAudioProvider(remote: RemoteAudioProvider) : AudioProvider(remote) {
    override var duration: Int = 0
    override val available: Int
        get() = TODO("Not yet implemented")
    override var position: Int = 0
    override var status: Status = Status.Uninitialized

    lateinit var buffer: ShortBuffer
    val events = ArrayList<Event>()
    var tempo = 0
    var startFadePos = 0
    var startPos = 0
    var endPos = 0
    var endFadePos = 0

    val effector = Effector()

    override fun start() = scope.launch {
        logInfo("starting")
        remote.start()
        buffer = ShortBuffer.allocate((remote.estimateDuration + 1).secToSampleCount())
        duration = remote.estimateDuration.secToSampleCount()
        logInfo("downloading")
        while (true) {
            buffer.put(remote.read() ?: break)
        }
        logInfo("analyzing")
        duration = buffer.position()
        val arr = ShortArray(duration / CHANNEL_COUNT)
        buffer.toArray(duration).forEachIndexed { index, sh ->
            arr[index / 2] = ((sh / 2) + arr[index / 2]).toShort()
        }
        val res = MusicAnalyzer(arr, SAMPLE_RATE).analyze()
        events.addAll(res.events)
        tempo = res.tempo

        val sorted = events.sortedBy { it.positionInSec }

        startFadePos = events.find { it is Event.StartPos }?.positionInSec?.secToSampleCount() ?: 0
        startPos = sorted.find { it is Event.VolumeUp && it.beatOffset == 0 }?.positionInSec?.secToSampleCount()
                ?: 0
        endPos = sorted.findLast { it is Event.VolumeDown && it.beatOffset == 0 }?.positionInSec?.secToSampleCount()
                ?: 0
        endFadePos = events.find { it is Event.EndPos }?.positionInSec?.secToSampleCount() ?: 0
        effector.scheduleEffect(Gain(1 / res.maxVolume - 0.05f), startFadePos, endFadePos)
        logInfo("tempo: ${res.tempo}")
        logInfo("adjust volume ${1 / res.maxVolume - 0.05f}")
        effector.scheduleEffect(FadeIn, startFadePos, startPos)
        effector.scheduleEffect(FadeOut, endPos, endFadePos)

        sorted.filter { it is Event.VolumeUp && it.beatOffset == 0 }.forEach { e ->
            val startEvent = events.find { it is Event.VolumeDown && it.beatOffset == 0 && e.positionInSec - it.positionInSec > 0 && e.positionInSec - it.positionInSec < 5 }
                    ?: return@forEach
            val end = e.positionInSec.secToSampleCount()
            val start = startEvent.positionInSec.secToSampleCount()
            effector.scheduleEffect(HighPass, start, end)
            logInfo("HighPass filter scheduled at ${start.sampleCountToSec()}s")
        }

        seek(startFadePos)
        System.gc()
        logInfo("ready")
        status = Status.Active
    }

    override fun canRead(size: Int) = status == Status.Active

    override fun read(size: Int): ShortBuffer {
        buffer.position(position)
        var arr = ShortArray(size)
        buffer.get(arr, 0, size.coerceAtMost(endFadePos - position))
        arr = effector.exec(arr, position)
        position = buffer.position()
        if (endFadePos - position == 0)
            status = Status.End
        return ShortBuffer.wrap(arr)
    }

    override fun seek(position: Int) {
        this.position = position
    }

    override fun release() {
        remote.release()
    }

}