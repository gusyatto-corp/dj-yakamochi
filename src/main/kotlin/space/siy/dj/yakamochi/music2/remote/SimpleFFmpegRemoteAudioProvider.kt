package space.siy.dj.yakamochi.music2.remote

import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FrameGrabber
import java.io.InputStream
import java.nio.ShortBuffer

/**
 * リモートからの読み取りもすべてFFmpegに任せるProvider
 * @author SIY1121
 */
class SimpleFFmpegRemoteAudioProvider(private val inputStream: InputStream, override val estimateDuration: Float = -1f) : RemoteAudioProvider {
    private var grabber: FFmpegFrameGrabber? = null

    override val format: String
        get() = grabber?.format ?: ""
    override val source: String
        get() = "InputStream"

    override fun start() {
        grabber = FFmpegFrameGrabber(inputStream).apply {
            sampleMode = FrameGrabber.SampleMode.SHORT
            sampleRate = 48000
            audioChannels = 2
            start()
        }
    }

    override fun read(): ShortBuffer? {
        if (grabber == null) throw IllegalStateException("startが呼び出されていません")
        return grabber?.grabSamples()?.samples?.get(0) as? ShortBuffer
    }

    override fun release() {
        grabber?.release()
    }

}