package space.siy.dj.yakamochi.music2

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import space.siy.dj.yakamochi.music2.player.CrossFadePlayer
import space.siy.dj.yakamochi.music2.track.TrackQueue

/**
 * @author SIY1121
 */
@ExperimentalStdlibApi
class GuildHandler(private val guildID: String) {
    private val trackQueue = TrackQueue(guildID)
    private val player = CrossFadePlayer(trackQueue)

    suspend fun onMessageReceived(event: MessageReceivedEvent) {
        when {
            event.message.contentRaw.matches(Regex("[\\s\\S]*?お[\\s\\S]*?い[\\s\\S]*?で[\\s\\S]*?")) -> {
                val channel = event.member?.voiceState?.channel ?: return
                event.guild.audioManager.run {
                    sendingHandler = player.apply { play() }
                    openAudioConnection(channel)
                }
            }
            event.message.contentRaw.contains("skip") -> {
                player.skip()
            }
            event.message.contentRaw.matches(Regex("[\\s\\S]*?([ばバ][\\s\\S]*?[いイ][\\s\\S]*?){2}[\\s\\S]*?")) -> {
                event.guild.audioManager.closeAudioConnection()
            }
            else -> {
                val url = event.message.contentRaw.matchUrl() ?: return
                trackQueue.addTrack(url, event.author.id, event.guild.id)
                event.message.addReaction("🎵").queue()
            }
        }
    }
}