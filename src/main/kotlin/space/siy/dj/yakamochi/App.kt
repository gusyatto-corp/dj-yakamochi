package space.siy.dj.yakamochi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.bytedeco.javacv.FFmpegLogCallback
import org.jetbrains.exposed.sql.Database
import org.koin.core.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.inject
import org.koin.dsl.module
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import space.siy.dj.yakamochi.database.*
import space.siy.dj.yakamochi.music2.player.CrossFadePlayer
import space.siy.dj.yakamochi.music2.player.GaplessPlayer
import space.siy.dj.yakamochi.music2.player.Player
import space.siy.dj.yakamochi.music2.track.TrackQueue

@ExperimentalStdlibApi
class Main : ListenerAdapter(), KoinComponent {

    val receiverScope = CoroutineScope(Dispatchers.IO)

    val trackQueues = HashMap<String, TrackQueue>()
    val players = HashMap<String, Player>()

    val userRepository: UserRepository by inject()
    val guildRepository: GuildRepository by inject()

    override fun onMessageReceived(event: MessageReceivedEvent) {
        receiverScope.launch {
            if (guildRepository.find(event.guild.id) == null)
                guildRepository.new(event.guild.id, event.guild.name, event.guild.iconUrl ?: "")
            if (userRepository.find(event.author.id) == null)
                userRepository.new(event.author.id, event.author.name, event.author.avatarUrl ?: "")

            if (!event.message.contentDisplay.contains("@DJ家持")) return@launch
            if (trackQueues[event.guild.id] == null) {
                event.channel.sendMessage("<@${event.author.id}> 準備するからちょっと待ってちょうだい").queue()
                trackQueues[event.guild.id] = TrackQueue(event.guild.id)
                players[event.guild.id] = GaplessPlayer(trackQueues[event.guild.id]!!)
            }
            when {
                event.message.contentRaw.contains("おいで") -> {
                    val channel = event.member?.voiceState?.channel ?: return@launch
                    event.guild.audioManager.run {
                        sendingHandler = players[event.guild.id]
                        openAudioConnection(channel)
                    }
                }
                event.message.contentRaw.contains("skip") -> {
                    players[event.guild.id]?.skip()
                }
                event.message.contentRaw.contains("バイバイ") -> {
                    event.guild.audioManager.closeAudioConnection()
                }
                else -> {
                    val url = event.message.contentRaw.matchUrl() ?: return@launch
                    trackQueues[event.guild.id]?.addTrack(url, event.author.id, event.guild.id)
                    event.message.addReaction("🎵").queue()
                }
            }
        }
    }
}

fun String.matchUrl(): String? {
    val regex = Regex("http(s)?:\\/\\/([\\w-]+\\.)+[\\w-]+(\\/[\\w- .\\/?%&=]*)?")
    return regex.find(this)?.value
}

val repositoryModule = module {
    single { ExposedTrackHistoryRepository() as TrackHistoryRepository }
    single { ExposedUserRepository() as UserRepository }
    single { ExposedGuildRepository() as GuildRepository }
}

@ExperimentalStdlibApi
fun main(args: Array<String>) {
    System.setProperty("org.bytedeco.javacpp.logger", "slf4j")
    FFmpegLogCallback.set()
    startKoin {
        printLogger()
        modules(repositoryModule)
    }
    Database.connect("jdbc:postgresql://${System.getenv("DB_HOST")}:${System.getenv("DB_PORT")}/${System.getenv("DB_NAME")}", driver = "org.postgresql.Driver",
            user = System.getenv("DB_USER"), password = System.getenv("DB_PASSWORD"))
    val jda = JDABuilder.createDefault(System.getenv("token")).build()
    jda.addEventListener(Main())
}


fun Any.logger(): Logger = LoggerFactory.getLogger(this::class.java)