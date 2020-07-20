package space.siy.dj.yakamochi.music2

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent
import space.siy.dj.yakamochi.music2.player.DJPlayer
import space.siy.dj.yakamochi.music_service.MusicService
import space.siy.dj.yakamochi.music_service.MusicServiceManager

/**
 * @author SIY1121
 */
@ExperimentalStdlibApi
class GuildHandler(private val guildID: String, private val djID: String) {
    private val player = DJPlayer(guildID)
    private var playlistMessageID: String? = null

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
            rawMsg.contains("enable history fallback") -> player.setHistoryFallback(true)
            rawMsg.contains("disable history fallback") -> player.setHistoryFallback(false)
            else -> {
                val url = rawMsg.matchUrl() ?: return
                when (MusicServiceManager.resourceType(url)) {
                    MusicService.ResourceType.Video -> {
                        player.queue(url, event.author.id, guildID) {
                            event.message.removeReaction("🎵").queue()
                            event.message.addReaction("✅").queue()
                        }
                        event.message.addReaction("🎵").queue()
                    }
                    MusicService.ResourceType.Playlist -> {
                        player.setPlaylist(url, event.author.id) {
                            event.message.removeReaction("🎵").queue()
                            event.message.removeReaction("🔁").queue()
                            event.message.removeReaction("🔀").queue()
                            event.message.removeReaction("❌").queue()
                            event.message.addReaction("✅").queue()
                        }
                        event.message.addReaction("🎵").queue()
                        event.message.addReaction("🔁").queue()
                        event.message.addReaction("🔀").queue()
                        event.message.addReaction("❌").queue()
                        playlistMessageID = event.messageId
                    }
                    MusicService.ResourceType.Unknown -> {
                        event.channel.sendMessage("これの再生の仕方がわからないわ...").queue()
                    }
                }
            }
        }
    }

    suspend fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        if (event.user?.id == event.jda.selfUser.id || event.messageId != playlistMessageID) return

        when (event.reactionEmote.name) {
            "❌" -> {
                player.clearPlaylist()
                playlistMessageID = null
            }
            "🔁" -> player.setPlaylistRepeat(true)
            "🔀" -> player.setPlaylistRandom(true)
        }
    }

    suspend fun onMessageReactionRemove(event: MessageReactionRemoveEvent) {
        when (event.reactionEmote.name) {
            "🔁" -> player.setPlaylistRepeat(false)
            "🔀" -> player.setPlaylistRandom(false)
        }
    }
}