package space.siy.dj.yakamochi.music

import net.dv8tion.jda.api.audio.AudioSendHandler

/**
 * @author SIY1121
 */
interface Player : AudioSendHandler {
    val nowPlaying: String
    fun addTrack(url: String)
}