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
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.post
import io.ktor.http.*
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.Locations
import io.ktor.locations.get
import io.ktor.request.userAgent
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.contentType
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.thymeleaf.Thymeleaf
import io.ktor.thymeleaf.ThymeleafContent
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import space.siy.dj.yakamochi.database.Auth
import space.siy.dj.yakamochi.database.AuthRepository
import space.siy.dj.yakamochi.logger
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.scheduleAtFixedRate

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
                parameters.append("prompt", "consent")
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

    // 認証システムを設定
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
                    if (credential.payload.audience.contains(audience) && credential.payload.subject == "access_token") JWTPrincipal(credential.payload) else null
                }
            }
            oauth("google-oauth") {
                client = HttpClient(CIO)
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
            // 発行されたログインリンククリック時
            get<Login> { p ->
                if (call.request.userAgent()?.contains("Discordbot") == true) {
                    call.respond(HttpStatusCode.Forbidden)
                    return@get
                }
                val userID = onetimeToken.entries.find { it.value == p.token }?.key
                if (userID == null) {
                    call.respond(HttpStatusCode.BadRequest, "このUrlは１回クリックしたら使えなくなるわよ！")
                    return@get
                }
                onetimeToken.remove(userID)
                call.response.cookies.append(Cookie("jwt", createJWT(userID), path = "/", httpOnly = true))
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
                    call.respondText { call.principal<JWTPrincipal>()?.payload?.getClaim("userID")?.asString() ?: "" }
                }
            }
            authenticate("google-oauth") {
                route("/auth/google") {
                    handle {
                        val oauthPrincipal = call.principal<OAuthAccessTokenResponse.OAuth2>()
                                ?: error("No principal")
                        val jwt = jwtVerifier.verify(call.request.cookies["jwt"]
                                ?: throw Exception("JWT not found"))
                        val userID = jwt.getClaim("userID").asString()
                        authRepository.upsert(Auth(userID, space.siy.dj.yakamochi.database.AuthProvider.Google, oauthPrincipal.accessToken, oauthPrincipal.refreshToken!!))
                        call.respond(ThymeleafContent("simple-message", mapOf("msg" to "ログイン完了よ！")))
                        doneCallback.remove(userID)?.invoke()
                    }
                }
            }

            // TODO 直書きやめる
            // Googleトークン更新処理
            Timer().scheduleAtFixedRate(0, 30 * 60 * 1000) {
                logger().info("Start Refresh Token Operation")
                val client = HttpClient(CIO) {
                    install(JsonFeature) {
                        serializer = GsonSerializer()
                    }
                    install(Logging) {
                        logger = io.ktor.client.features.logging.Logger.DEFAULT
                        level = LogLevel.HEADERS
                    }
                }
                GlobalScope.launch(Dispatchers.IO) {
                    authRepository.find(space.siy.dj.yakamochi.database.AuthProvider.Google).forEach {
                        val res = client.post<Map<String, String>>("https://www.googleapis.com/oauth2/v3/token") {
                            body = FormDataContent(Parameters.build {
                                append("refresh_token", it.refreshToken)
                                append("client_id", googleOauthProvider.clientId)
                                append("client_secret", googleOauthProvider.clientSecret)
                                append("redirect_uri", "$baseUrl/auth/google")
                                append("grant_type", "refresh_token")
                            })
                        }
                        val newAccessToken = res["access_token"] ?: throw Exception("AccessTokenが取得できませんでした")
                        authRepository.upsert(Auth(it.userID, it.provider, newAccessToken, it.refreshToken))
                        logger().info("Token Refreshed: ${it.userID}")
                    }
                }

            }
        }
    }

    private val issuer = System.getenv("JWT_ISSUER")
    private val audience = System.getenv("JWT_AUDIENCE")

    private val algorithm = Algorithm.HMAC256(System.getenv("JWT_SECRET"))

    private val jwtVerifier = JWT
            .require(algorithm)
            .withAudience(audience)
            .withIssuer(issuer)
            .build()

    private fun createJWT(userID: String) =
            JWT.create()
                    .withIssuer(audience)
                    .withAudience(issuer)
                    .withSubject("access_token")
                    .withClaim("userID", userID)
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