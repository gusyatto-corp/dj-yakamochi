package space.siy.dj.yakamochi.gateway.http

import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.util.KtorExperimentalAPI
import space.siy.dj.yakamochi.gateway.Gateway

/**
 * @author SIY1121
 */

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
object HttpServer: Gateway {
    override fun start() {
        embeddedServer(CIO, System.getenv("HTTP_PORT").toInt()) {
            authModule()
            routing {
                static("static/img") {
                    resources("img")
                }
            }
        }.start()
    }
}
