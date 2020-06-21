package space.siy.dj.yakamochi.music

import com.sapher.youtubedl.YoutubeDL
import com.sapher.youtubedl.mapper.VideoInfo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.bytedeco.javacv.*
import java.nio.ShortBuffer
import kotlin.math.abs
import org.bytedeco.ffmpeg.global.avformat.*


/**
 * @author SIY1121
 */

class FFmpegTrackDataProvider(sourceUrl: String, val gapless: Boolean = true, val normalizeVolume: Boolean = true) : TrackDataProvider(sourceUrl) {
    lateinit var rawBuffer: ShortBuffer

    private var putPosition: Int = 0
    private var readPosition: Int = 0

    var startGap = 0f
    var endGap = 0f
    var maxVolume = 1f

    override val isLive: Boolean
        get() = rawTrackInfo.duration > 0

    override var loadProgress: Float = 0f
        set(value) = synchronized(field) {
            field = value
        }
        get() = synchronized(field) { field }

    lateinit var rawTrackInfo: VideoInfo

    var detectedDuration = 0f

    override val trackInfo: Info
        get() = Info(sourceUrl, rawTrackInfo.title, detectedDuration - endGap, rawTrackInfo.thumbnail)

    override var status: Status = Status.UnInitialized
        private set
    override val providedPosition: Float
        get() = readPosition.toFloat() / sampleRate / channelCount

    override var historyID: Int = -1

    override fun canRead(size: Int) = readPosition + size < putPosition

    override fun canRead(range: IntRange): Boolean {
        TODO("Not yet implemented")
    }

    override fun read(size: Int): ShortArray {
        val arr = ShortArray(size)
        synchronized(rawBuffer) {
            rawBuffer.position(readPosition)
            rawBuffer.get(arr, 0, (putPosition - readPosition).coerceAtMost(size))
            readPosition = rawBuffer.position()
        }
        return arr
    }

    override fun read(range: IntRange): ShortArray {
        TODO("Not yet implemented")
    }

    override fun loadMetadata() = GlobalScope.launch {
        rawTrackInfo = YoutubeDL.getVideoInfo(sourceUrl)
        detectedDuration = rawTrackInfo.duration.toFloat()
    }

    override fun loadMedia() = GlobalScope.launch {
        val targetFormat = rawTrackInfo.formats.find { it.acodec == "opus" && it.abr > 150 }
                ?: rawTrackInfo.formats.last()

        val grabber = if (targetFormat.format.contains("DASH")) {
            val pb = ProcessBuilder("youtube-dl", sourceUrl, "-f ${targetFormat.formatId}", "-q", "-o-")
            val p = pb.start()
            FFmpegFrameGrabber(p.inputStream)
        } else FFmpegFrameGrabber(targetFormat.url)

        grabber.sampleMode = FrameGrabber.SampleMode.SHORT
        grabber.sampleRate = 48000
        grabber.audioChannels = 2
        rawBuffer = ShortBuffer.allocate((rawTrackInfo.duration + 1) * sampleRate * channelCount)
        grabber.start()

        status = Status.Ready

        while (true) {
            val frame = grabber.grabSamples() ?: break
            val buf = frame.samples[0] as ShortBuffer
            synchronized(rawBuffer) {
                rawBuffer.position(putPosition)
                rawBuffer.put(buf)
                putPosition = rawBuffer.position()
            }
            loadProgress = putPosition.toFloat() / rawBuffer.limit()
        }
        loadProgress = 1f
        detectedDuration = putPosition / sampleRate / channelCount.toFloat()
        grabber.release()

        if (!gapless) return@launch
        synchronized(rawBuffer) {
            for (scanPos in 0 until rawBuffer.limit()) {
                if (abs(rawBuffer.get(scanPos).toInt()) > 1000) {
                    startGap = scanPos / 48000f / 2
                    break
                }
            }
            println("start gap: $startGap")
            for (scanPos in rawBuffer.limit() - 1 downTo 0) {
                if (abs(rawBuffer.get(scanPos).toInt()) > 1000) {
                    endGap = (rawBuffer.limit() - scanPos) / 48000f / 2
                    break
                }
            }
            println("end gap $endGap")
        }
    }

}