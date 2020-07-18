package space.siy.dj.yakamochi.music2

import com.sapher.youtubedl.YoutubeDL
import com.sapher.youtubedl.YoutubeDLRequest
import kotlinx.coroutines.delay
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
        val rawMsg = event.message.contentRaw

        when {
            rawMsg.matches(Regex("[\\s\\S]*?お[\\s\\S]*?い[\\s\\S]*?で[\\s\\S]*?")) -> {
                val channel = event.member?.voiceState?.channel ?: return
                event.guild.audioManager.run {
                    sendingHandler = player.apply {
                        init()
                        play()
                    }
                    openAudioConnection(channel)
                }
            }
            rawMsg.contains("skip") -> {
                player.skip()
            }
            rawMsg.matches(Regex("[\\s\\S]*?([ばバ][\\s\\S]*?[いイ][\\s\\S]*?){2}[\\s\\S]*?")) -> {
                event.guild.audioManager.closeAudioConnection()
            }
            else -> {
                val url = rawMsg.matchUrl() ?: return
                val playlistID = url.matchYoutubePlaylistID()
                if (playlistID != null) {
                    val isRandom = event.message.contentRaw.contains("random")
                    val urls = YoutubeDL.execute(YoutubeDLRequest(url).apply {
                        setOption("get-id")
                    }).out.split("\n")
                            .map { "https://www.youtube.com/watch?v=$it" }

                    (if (isRandom) urls.shuffled() else urls).forEach {
                        player.queue(it, event.author.id, guildID)
                        delay(2000)
                    }
                } else
                    player.queue(url, event.author.id, guildID)
                event.message.addReaction("🎵").queue()
            }
        }
    }
}