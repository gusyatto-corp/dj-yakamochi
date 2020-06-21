package space.siy.dj.yakamochi.music2.remote

import com.sapher.youtubedl.YoutubeDL
import com.sapher.youtubedl.mapper.VideoFormat
import com.sapher.youtubedl.mapper.VideoInfo
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FrameGrabber
import java.nio.ShortBuffer

/**
 * FFMpegがサポートしてないMPD等の読み込みをYoutubeDLに肩代わりさせるProvider
 * @author SIY1121
 */
class YoutubeDLFFmpegRemoteAudioProvider(val url: String, private val info: VideoInfo? = null) : RemoteAudioProvider {
    private var grabber: FFmpegFrameGrabber? = null
    private var ytdlProcess: Process? = null
    private var targetFormat: VideoFormat? = null
    private var videoInfo: VideoInfo? = null

    override val format: String
        get() = if (targetFormat != null) targetFormat!!.format else ""
    override val estimateDuration: Float
        get() = if (videoInfo != null)
            if (videoInfo!!.duration == 0)
                0f
            else
                videoInfo!!.duration.toFloat() + 1
        else -1f
    override val source: String
        get() = url

    override fun start() {
        val info = this.info ?: YoutubeDL.getVideoInfo(url)
        videoInfo = info
        val targetFormat = info.formats.filter { it.acodec == "opus" }.maxBy { it.abr } ?: info.formats.last()

        if (targetFormat.format.contains("DASH")) {
            ytdlProcess = ProcessBuilder("youtube-dl", url, "-f ${targetFormat.formatId}", "-q", "-o-").start().apply {
                grabber = FFmpegFrameGrabber(inputStream).apply {
                    sampleMode = FrameGrabber.SampleMode.SHORT
                    sampleRate = 48000
                    audioChannels = 2
                    start()
                }
            }
        } else {
            grabber = FFmpegFrameGrabber(targetFormat.url).apply {
                sampleMode = FrameGrabber.SampleMode.SHORT
                sampleRate = 48000
                audioChannels = 2
                start()
            }
        }
    }

    override fun read(): ShortBuffer? {
        if (grabber == null) return null
        return grabber?.run {
            synchronized(this) {
                grabSamples()?.samples?.get(0) as? ShortBuffer
            }
        }
    }

    override fun release() {
        synchronized(grabber ?: return) {
            grabber?.stop()
        }
        grabber = null
    }
}