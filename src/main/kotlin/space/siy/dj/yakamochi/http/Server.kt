package space.siy.dj.yakamochi.http

import io.ktor.http.content.files
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.util.KtorExperimentalAPI
import space.siy.dj.yakamochi.auth.authModule

/**
 * @author SIY1121
 */

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
fun startHttpServer() {
    embeddedServer(CIO, System.getenv("HTTP_PORT").toInt()) {
        authModule()
        routing {
            static("static/img") {
                resources("img")
            }
        }
    }.start()
}