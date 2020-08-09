package space.siy.dj.yakamochi.music2

import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent
import org.koin.core.KoinComponent
import org.koin.core.inject
import space.siy.dj.yakamochi.Outcome
import space.siy.dj.yakamochi.auth.AuthProvider
import space.siy.dj.yakamochi.music2.player.DJPlayer
import space.siy.dj.yakamochi.music2.player.Player
import space.siy.dj.yakamochi.music2.track.PlaylistTrackProvider
import space.siy.dj.yakamochi.music_service.MusicService
import space.siy.dj.yakamochi.music_service.MusicServiceManager
import space.siy.dj.yakamochi.stackTraceString

/**
 * @author SIY1121
 */
@ExperimentalStdlibApi
class GuildHandler(private val guildID: String, private val djID: String) : KoinComponent {
    private val authProvider: AuthProvider by inject()
    private val player = DJPlayer(guildID).apply {
        onErrorHandler = {
            if (activeChannel != null)
                it.handle(activeChannel!!)
        }
    }
    private var playlistMessageID: String? = null

    private var activeChannel: MessageChannel? = null

    suspend fun onMessageReceived(event: MessageReceivedEvent) = runCatching {
        val rawMsg = event.message.contentRaw

        when {
            rawMsg.matches(Regex("[\\s\\S]*?お[\\s\\S]*?い[\\s\\S]*?で[\\s\\S]*?")) -> {
                val channel = event.member?.voiceState?.channel ?: return@runCatching
                event.guild.audioManager.run {
                    sendingHandler = player.apply {
                        if (!player.initialized)
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
            rawMsg.contains("おまかせ") -> player.setHistoryFallback(true)
            rawMsg.contains("おまかせおわり") -> player.setHistoryFallback(false)
            rawMsg.contains(Regex("(好き|すき|すこ)")) -> {
                val videoInfo = player.videoInfo ?: return@runCatching
                when (val result = MusicServiceManager.like(videoInfo.url, event.author.id)) {
                    is Outcome.Success -> event.message.addReaction("👍").queue()
                    is Outcome.Error -> when (result.reason) {
                        is MusicService.ErrorReason.UnsupportedOperation -> event.channel.sendMessage(MessageBuilder()
                                .append("<@${event.author.id}>")
                                .append("このサービスはまだ連携できないから自分でお気に入り登録してちょうだい\n")
                                .append(videoInfo.url)
                                .build()
                        ).queue()
                        is MusicService.ErrorReason.Unauthorized -> {
                            event.channel.sendMessage(MessageBuilder()
                                    .append("<@${event.author.id}>")
                                    .append("あんたの代わりにお気に入りに登録するには${result.reason.type}のログインが必要よ！\nDMを見てちょうだい")
                                    .build()
                            ).queue()
                            val privateChannel = event.author.openPrivateChannel().complete()
                            privateChannel.sendMessage(authProvider.requestAuth(event.author.id, result.reason.type) {
                                runBlocking { MusicServiceManager.like(videoInfo.url, event.author.id) }
                                privateChannel.sendMessage("ログイン完了よ！").queue()
                                event.message.addReaction("👍").queue()
                            }).queue()
                        }
                        else -> event.message.addReaction("❌").queue()
                    }
                }
            }
            else -> {
                val url = rawMsg.matchUrl() ?: return@runCatching
                when (MusicServiceManager.resourceType(url)) {
                    MusicService.ResourceType.Video -> {
                        val result = player.queue(url, event.author.id, guildID) {
                            event.message.clearReactions().complete()
                            event.message.addReaction("✅").queue()
                        }
                        when (result) {
                            is Outcome.Success -> event.message.addReaction("🎵").queue()
                            is Outcome.Error -> result.handle(event.channel)
                        }

                    }
                    MusicService.ResourceType.Playlist -> {
                        val result = player.setPlaylist(url, event.author.id) {
                            event.message.clearReactions().complete()
                            event.message.addReaction("✅").queue()
                        }
                        when (result) {
                            is Outcome.Success -> {
                                event.message.addReaction("🎵").queue()
                                event.message.addReaction("🔁").queue()
                                event.message.addReaction("🔀").queue()
                                event.message.addReaction("❌").queue()
                                playlistMessageID = event.messageId
                            }
                            is Outcome.Error -> result.handle(event.channel)
                        }

                    }
                    MusicService.ResourceType.Unknown -> {
                        event.channel.sendMessage("これの再生の仕方がわからないわ").queue()
                    }
                }
            }
        }
        activeChannel = event.channel
    }.onFailure {
        activeChannel?.sendMessage("""なんか機材が煙上げてるんだけど！！！！
            ```======家持ちゃんが操作した機材ログ(Unmanaged)======
            
            ${it.stackTraceString()}
        ```""".trimIndent())?.queue()
        it.printStackTrace()
    }.getOrNull()

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

    private fun Outcome.Error<Player.ErrorReason>.handle(channel: MessageChannel) {
        fun String.attachLog() = this + """
```======家持ちゃんが操作した機材ログ======

======Reason======
$reason

======StackTrace======
${cause?.stackTraceString()?.ensureUnderNChars(1000) ?: "This error has no StackTrace"}
```
""".trimIndent()

        when (this.reason) {
            is Player.ErrorReason.MusicServiceError -> when (reason.reason) {
                is MusicService.ErrorReason.Unavailable -> when (reason.reason.reason) {
                    MusicService.ErrorReason.Unavailable.Reason.NotFound -> channel.sendMessage("存在しないか、非公開になってるから再生できないわ！出直しなさい！").queue()
                    MusicService.ErrorReason.Unavailable.Reason.Forbidden -> channel.sendMessage("非公開になってるから再生できないわ、公開してくれないと見れないじゃない！").queue()
                    MusicService.ErrorReason.Unavailable.Reason.Unknown -> channel.sendMessage("よくわからないけど再生できなかったわ！ポンコツ機材ね！".attachLog()).queue()
                }
                is MusicService.ErrorReason.UnsupportedResource -> channel.sendMessage("再生の仕方がわからなかったわ".attachLog()).queue()
                else -> {
                    channel.sendMessage("よくわからないけど再生できなかったわ！ポンコツ機材ね！".attachLog()).queue()
                    println(reason)
                    cause?.printStackTrace()
                }
            }
            is Player.ErrorReason.Unhandled -> {
                channel.sendMessage("よくわからないけど再生できなかったわ！ポンコツ機材ね！".attachLog()).queue()
                println(reason)
                cause?.printStackTrace()
            }
        }
    }

    fun String.ensureUnderNChars(n: Int) = if (length > n) substring(0, n - 20) + "\n...${substring(n - 20).lines().size} lines follow" else this
}