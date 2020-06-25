package space.siy.dj.yakamochi.music2.remote

import com.sapher.youtubedl.YoutubeDL
import com.sapher.youtubedl.mapper.VideoFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FrameGrabber
import space.siy.dj.yakamochi.music2.VideoInfo
import java.nio.ShortBuffer

/**
 * FFMpegがサポートしてないMPD等の読み込みをYoutubeDLに肩代わりさせるProvider
 * @author SIY1121
 */
class YoutubeDLFFmpegRemoteAudioProvider(private val videoInfo: VideoInfo, private val _formatsInfo: List<VideoFormat>? = null) : RemoteAudioProvider {
    private var grabber: FFmpegFrameGrabber? = null
    private var ytdlProcess: Process? = null
    private var targetFormat: VideoFormat? = null

    override val format: String
        get() = if (targetFormat != null) targetFormat!!.format else ""
    override val estimateDuration: Float
        get() = videoInfo.duration
    override val source: String
        get() = videoInfo.title

    override suspend fun start() = withContext(Dispatchers.IO) {
        val info = this@YoutubeDLFFmpegRemoteAudioProvider._formatsInfo ?: YoutubeDL.getFormats(videoInfo.url)
        val targetFormat = info.filter { it.acodec == "opus" }.maxBy { it.abr } ?: info.last()
        this@YoutubeDLFFmpegRemoteAudioProvider.targetFormat = targetFormat

        if (targetFormat.format.contains("DASH")) {
            ytdlProcess = ProcessBuilder("youtube-dl", videoInfo.url, "-f ${targetFormat.formatId}", "-q", "-o-").start().apply {
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
                setOption("reconnect", "1")
                start()
            }
        }
    }

    override suspend fun read(): ShortBuffer? = withContext(Dispatchers.IO) {
        if (grabber == null) return@withContext null
        return@withContext grabber?.run {
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