package space.siy.dj.yakamochi.music2.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FrameGrabber
import java.io.InputStream
import java.nio.ShortBuffer

/**
 * リモートからの読み取りもすべてFFmpegに任せるProvider
 * @author SIY1121
 */
class SimpleFFmpegRemoteAudioProvider(private val inputStream: InputStream, override val estimateDuration: Float = -1f, override val title: String, override val source: String) : RemoteAudioProvider {
    private var grabber: FFmpegFrameGrabber? = null

    override val format: String
        get() = grabber?.format ?: ""

    override suspend fun start() = withContext(Dispatchers.IO) {
        grabber = FFmpegFrameGrabber(inputStream).apply {
            sampleMode = FrameGrabber.SampleMode.SHORT
            sampleRate = 48000
            audioChannels = 2
            start()
        }
    }

    override suspend fun read(): ShortBuffer? = withContext(Dispatchers.IO) {
        if (grabber == null) throw IllegalStateException("startが呼び出されていません")
        return@withContext grabber?.grabSamples()?.samples?.get(0) as? ShortBuffer
    }

    override fun release() {
        grabber?.release()
    }

}