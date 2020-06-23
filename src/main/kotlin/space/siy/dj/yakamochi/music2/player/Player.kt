package space.siy.dj.yakamochi.music2.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.dv8tion.jda.api.audio.AudioSendHandler
import space.siy.dj.yakamochi.music2.audio.AudioProvider
import space.siy.dj.yakamochi.music2.secToSampleCount

/**
 * @author SIY1121
 */
interface Player : AudioSendHandler {
    fun play()
    fun pause()
    suspend fun skip()
}

fun AudioProvider.read20Ms() = read(0.02f.secToSampleCount())

fun AudioProvider.canRead20Ms() = canRead(0.02f.secToSampleCount())