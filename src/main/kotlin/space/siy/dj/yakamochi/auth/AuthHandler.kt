package space.siy.dj.yakamochi.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.Application
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
import io.ktor.thymeleaf.Thymeleaf
import io.ktor.thymeleaf.ThymeleafContent
import io.ktor.util.KtorExperimentalAPI
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import space.siy.dj.yakamochi.database.Auth
import space.siy.dj.yakamochi.database.AuthRepository
import java.util.*
import kotlin.collections.HashMap

/**
 * @author SIY1121
 */

@KtorExperimentalLocationsAPI
@KtorExperimentalAPI
fun Application.authModule() {
    AuthHandler.configure(this)
}

@KtorExperimentalLocationsAPI
@KtorExperimentalAPI
object AuthHandler : AuthProvider, KoinComponent {

    val authRepository: AuthRepository by inject()

    val googleOauthProvider = OAuthServerSettings.OAuth2ServerSettings(
            name = "google",
            authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
            accessTokenUrl = "https://www.googleapis.com/oauth2/v3/token",
            requestMethod = HttpMethod.Post,
            authorizeUrlInterceptor = {
                parameters.append("access_type", "offline")
            },
            clientId = System.getenv("GOOGLE_CLIENT_ID"),
            clientSecret = System.getenv("GOOGLE_CLIENT_SECRET"),
            defaultScopes = listOf("profile", "https://www.googleapis.com/auth/youtube")
    )

    @Location("/auth/login")
    data class Login(val token: String, val next: String)

    val onetimeToken = HashMap<String, String>()
    val doneCallback = HashMap<String, () -> Unit>()

    val baseUrl = System.getenv("HTTP_BASE_URL")

    fun configure(app: Application) = app.run {
        install(Authentication) {
            jwt("jwt") {
                realm = "dj-yakamochi"
                verifier(jwtVerifier)
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
                    "$baseUrl/auth/google"
                }
            }
        }
        install(Locations)
        install(Thymeleaf) {
            setTemplateResolver(ClassLoaderTemplateResolver().apply {
                prefix = "templates/"
                suffix = ".html"
                characterEncoding = "utf-8"
            })
        }
        routing {
            get<Login> { p ->
                val userID = onetimeToken.entries.find { it.value == p.token }?.key
                if (userID == null) {
                    call.respond(HttpStatusCode.BadRequest, "このUrlは１回クリックしたら使えなくなるわよ！")
                    return@get
                }
                onetimeToken.remove(userID)
                call.response.cookies.append(Cookie("jwt", createJWT(userID), path = "/"))
                when (AuthType.valueOf(p.next)) {
                    AuthType.Self -> {
                        call.respondRedirect("/secret")
                        doneCallback.remove(userID)?.invoke()
                    }
                    else -> call.respondRedirect("/auth/${p.next.toLowerCase()}")
                }
            }
            authenticate("jwt") {
                get("/secret") {
                    call.respondText { call.principal<JWTPrincipal>()?.payload?.subject ?: "" }
                }
            }
            authenticate("google-oauth") {
                route("/auth/google") {
                    handle {
                        val oauthPrincipal = call.principal<OAuthAccessTokenResponse.OAuth2>()
                                ?: error("No principal")
                        val jwt = jwtVerifier.verify(call.request.cookies["jwt"]
                                ?: throw Exception("JWT not found"))
                        authRepository.upsert(Auth(jwt.subject, space.siy.dj.yakamochi.database.AuthProvider.Google, oauthPrincipal.accessToken, oauthPrincipal.refreshToken!!))
                        call.respond(ThymeleafContent("simple-message", mapOf("msg" to "ログイン完了よ！")))
                        doneCallback.remove(jwt.subject)?.invoke()
                    }
                }
            }
        }
    }

    private val algorithm = Algorithm.HMAC256(System.getenv("JWT_SECRET"))

    private val jwtVerifier = JWT
            .require(algorithm)
            .withAudience("audience")
            .withIssuer("issuer")
            .build()

    private fun createJWT(userID: String) =
            JWT.create()
                    .withIssuer("issuer")
                    .withAudience("audience")
                    .withSubject(userID)
                    .sign(algorithm)

    override fun requestAuth(userID: String, type: AuthType, done: (() -> Unit)?): String {
        onetimeToken[userID] = UUID.randomUUID().toString()
        if (done != null)
            doneCallback[userID] = done
        return """
        ログインするには次のリンクをクリックしてちょうだい！
        $baseUrl/auth/login?token=${onetimeToken[userID]}&next=$type
        """.trimIndent()
    }
}