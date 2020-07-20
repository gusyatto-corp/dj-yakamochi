package space.siy.dj.yakamochi.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.*
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.client.HttpClient
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.Locations
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.util.KtorExperimentalAPI
import java.util.*
import kotlin.collections.HashMap

/**
 * @author SIY1121
 */

@KtorExperimentalLocationsAPI
@KtorExperimentalAPI
class Server: AuthProvider {
    val googleOauthProvider = OAuthServerSettings.OAuth2ServerSettings(
            name = "google",
            authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
            accessTokenUrl = "https://www.googleapis.com/oauth2/v3/token",
            requestMethod = HttpMethod.Post,

            clientId = System.getenv("GOOGLE_CLIENT_ID"),
            clientSecret = System.getenv("GOOGLE_CLIENT_SECRET"),
            defaultScopes = listOf("profile", "https://www.googleapis.com/auth/youtube")
    )

    @Location("/auth/login")
    data class Login(val token: String, val next: String)
    val onetimeToken = HashMap<String, String>()

    fun start() {
        embeddedServer(CIO, 8080) {
            install(Authentication) {
                jwt("jwt") {
                    realm = "dj-yakamochi"
                    verifier(makeJwtVerifier("issuer", "audience"))
                    authHeader {
                        val jwt = it.request.cookies["jwt"] ?: return@authHeader null
                        HttpAuthHeader.Single("bearer", jwt)
                    }
                    validate { credential ->
                        if (credential.payload.audience.contains("audience")) JWTPrincipal(credential.payload) else null
                    }
                }
                oauth("google-oauth") {
                    client = HttpClient(io.ktor.client.engine.cio.CIO)
                    providerLookup = { googleOauthProvider }
                    urlProvider = {
                        "http://localhost:8080/auth/google"
                    }
                }
            }
            install(Locations)
            routing {
                get<Login> { p ->
                    val userID = onetimeToken.entries.find { it.value ==  p.token}?.key
                    if(userID == null) {
                        call.respond(HttpStatusCode.BadRequest, "invalid token")
                        return@get
                    }
                    onetimeToken.remove(userID)
                    call.response.cookies.append(Cookie("jwt", createJWT(userID), path = "/"))
                    call.respondRedirect("/secret")
                }
                authenticate("jwt") {
                    get("/secret") {
                        call.respondText { call.principal<JWTPrincipal>()?.payload?.subject ?: "" }
                    }
                }
                authenticate("google-oauth") {
                    route("/auth/google") {
                        handle {
                            val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
                                    ?: error("No principal")
                            println(principal)
                            call.respondText { "OK" }
                        }
                    }
                }
            }
        }.start(wait = false)
    }

    private val algorithm = Algorithm.HMAC256(System.getenv("JWT_SECRET"))
    private fun makeJwtVerifier(issuer: String, audience: String): JWTVerifier = JWT
            .require(algorithm)
            .withAudience(audience)
            .withIssuer(issuer)
            .build()

    private fun createJWT(userID: String) =
            JWT.create()
                    .withIssuer("issuer")
                    .withAudience("audience")
                    .withSubject(userID)
                    .sign(algorithm)

    override fun requestAuth(userID: String, type: AuthType): String {
        onetimeToken[userID] = UUID.randomUUID().toString()
        return """
        下のUrlをクリックしてちょうだい
        http://localhost:8080/auth/login?token=${onetimeToken[userID]}&next=$type
        """.trimIndent()
    }
}