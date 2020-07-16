package space.siy.dj.yakamochi.music2

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import space.siy.dj.yakamochi.music2.player.DJPlayer
import space.siy.dj.yakamochi.music2.player.SimplePlayer

/**
 * @author SIY1121
 */
@ExperimentalStdlibApi
class GuildHandler(private val guildID: String, private val djID: String) {
    private val player = DJPlayer(guildID)


    suspend fun onMessageReceived(event: MessageReceivedEvent) {
        when {
            event.message.contentRaw.matches(Regex("[\\s\\S]*?お[\\s\\S]*?い[\\s\\S]*?で[\\s\\S]*?")) -> {
                val channel = event.member?.voiceState?.channel ?: return
                event.guild.audioManager.run {
                    sendingHandler = player.apply {
                        init()
                        play()
                    }
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
                player.queue(url, event.author.id, guildID)
                event.message.addReaction("🎵").queue()
            }
        }
    }
}