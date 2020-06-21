package space.siy.dj.yakamochi

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.jetbrains.exposed.sql.Database
import org.koin.core.context.startKoin
import org.koin.dsl.module
import space.siy.dj.yakamochi.music2.player.CrossFadePlayer
import space.siy.dj.yakamochi.music2.player.GaplessPlayer
import space.siy.dj.yakamochi.music2.player.SimplePlayer
import space.siy.dj.yakamochi.music2.track.TrackQueue
import space.siy.dj.yakamochi.music2.track.database.ExposedTrackHistoryRepository
import space.siy.dj.yakamochi.music2.track.database.TrackHistoryRepository

@ExperimentalStdlibApi
class Main : ListenerAdapter() {
    val trackQueue = TrackQueue()
    val player = GaplessPlayer(trackQueue)
    override fun onMessageReceived(event: MessageReceivedEvent) {
        GlobalScope.launch {
            if (!event.message.contentRaw.contains(event.jda.selfUser.id)) return@launch
            when {
                event.message.contentRaw.contains("おいで") -> {
                    val channel = event.member?.voiceState?.channel ?: return@launch
                    event.guild.audioManager.run {
                        sendingHandler = player
                        openAudioConnection(channel)
                    }
                }
                event.message.contentRaw.contains("skip") -> {
                    player.skip()
                }
                event.message.contentRaw.contains("バイバイ") -> {
                    event.guild.audioManager.closeAudioConnection()
                }
                else -> {
                    val url = event.message.contentRaw.matchUrl() ?: return@launch
                    trackQueue.addTrack(url, event.author.id)
                    event.channel.sendMessage("<@${event.author.id}> 追加したわよ").queue()
                }
            }
        }
    }
}

fun String.matchUrl(): String? {
    val regex = Regex("http(s)?:\\/\\/([\\w-]+\\.)+[\\w-]+(\\/[\\w- .\\/?%&=]*)?")
    return regex.find(this)?.value
}

@ExperimentalStdlibApi
fun main(args: Array<String>) {
    startKoin {
        printLogger()
        module {
            single { ExposedTrackHistoryRepository() as TrackHistoryRepository }
        }
    }
    Database.connect("jdbc:postgresql://${System.getenv("DB_HOST")}:${System.getenv("DB_PORT")}/${System.getenv("DB_NAME")}", driver = "org.postgresql.Driver",
            user = System.getenv("DB_USER"), password = System.getenv("DB_PASSWORD"))
    val jda = JDABuilder.createDefault(System.getenv("token")).build()
    jda.addEventListener(Main())
}
