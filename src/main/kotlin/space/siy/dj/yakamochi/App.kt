package space.siy.dj.yakamochi

import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.hooks.ListenerAdapter
import space.siy.dj.yakamochi.music.DefaultPlayer
import java.nio.ByteBuffer
import java.nio.IntBuffer

class Main : ListenerAdapter() {
    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.message.contentRaw.contains(event.jda.selfUser.id)) {
            val channel = event.member?.voiceState?.channel ?: return
            event.guild.audioManager.run {
                sendingHandler = DefaultPlayer().apply {
                    openAudioConnection(channel)
                }
            }
        }
    }
}

fun main(args: Array<String>) {
    val jda = JDABuilder.createDefault(System.getenv("token")).build()
    jda.addEventListener(Main())
}
