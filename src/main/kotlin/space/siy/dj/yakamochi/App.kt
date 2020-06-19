package space.siy.dj.yakamochi

import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import space.siy.dj.yakamochi.music.DefaultPlayer
import space.siy.dj.yakamochi.music.database.TrackHistories
import space.siy.dj.yakamochi.music.database.TrackHistory
import java.nio.ByteBuffer
import java.nio.IntBuffer

class Main : ListenerAdapter() {
    val player = DefaultPlayer()
    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (!event.message.contentRaw.contains(event.jda.selfUser.id)) return
        if (event.message.contentRaw.contains("おいで")) {
            val channel = event.member?.voiceState?.channel ?: return
            event.guild.audioManager.run {
                sendingHandler = player
                openAudioConnection(channel)
            }
        } else if (event.message.contentRaw.contains("skip")) {
            player.skip()
        } else if (event.message.contentRaw.contains("バイバイ")) {
            event.guild.audioManager.closeAudioConnection()
        } else {
            val url = event.message.contentRaw.matchUrl() ?: return
            player.addTrack(url)
            event.channel.sendMessage("<@${event.author.id}> 追加したわよ")
        }
    }
}

fun String.matchUrl(): String? {
    val regex = Regex("http(s)?:\\/\\/([\\w-]+\\.)+[\\w-]+(\\/[\\w- .\\/?%&=]*)?")
    return regex.find(this)?.value
}

fun main(args: Array<String>) {
    val jda = JDABuilder.createDefault(System.getenv("token")).build()
    jda.addEventListener(Main())
}
