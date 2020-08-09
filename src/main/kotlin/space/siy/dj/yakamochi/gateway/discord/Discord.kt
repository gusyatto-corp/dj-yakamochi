package space.siy.dj.yakamochi.gateway.discord

import net.dv8tion.jda.api.JDABuilder
import space.siy.dj.yakamochi.gateway.Gateway

/**
 * @author SIY1121
 */
@ExperimentalStdlibApi
object Discord: Gateway {
    override fun start() {
        val jda = JDABuilder.createDefault(System.getenv("token")).build()
        jda.addEventListener(EventHandler())
    }
}