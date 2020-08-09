package space.siy.dj.yakamochi.gateway

import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.util.KtorExperimentalAPI
import space.siy.dj.yakamochi.gateway.discord.Discord
import space.siy.dj.yakamochi.gateway.http.HttpServer

/**
 * @author SIY1121
 */

/**
 * コアを制御するためのGateway
 */
interface Gateway {
    fun start()
}

@ExperimentalStdlibApi
@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
object Gateways: Gateway{
    private val gateways = listOf(Discord, HttpServer)
    override fun start() {
        gateways.forEach { it.start() }
    }
}