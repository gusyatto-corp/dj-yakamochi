package space.siy.dj.yakamochi

import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.util.KtorExperimentalAPI
import net.dv8tion.jda.api.JDABuilder
import org.bytedeco.javacv.FFmpegLogCallback
import org.jetbrains.exposed.sql.Database
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import space.siy.dj.yakamochi.auth.AuthHandler
import space.siy.dj.yakamochi.database.*
import space.siy.dj.yakamochi.http.startHttpServer

val repositoryModule = module {
    single { ExposedTrackHistoryRepository() as TrackHistoryRepository }
    single { ExposedUserRepository() as UserRepository }
    single { ExposedGuildRepository() as GuildRepository }
    single { ExposedAuthRepository() as AuthRepository }
}

@KtorExperimentalLocationsAPI
@KtorExperimentalAPI
@ExperimentalStdlibApi
fun main(args: Array<String>) {
    System.setProperty("org.bytedeco.javacpp.logger", "slf4j")
    FFmpegLogCallback.set()
    startHttpServer()
    startKoin {
        printLogger()
        modules(repositoryModule)
        modules(module {
            single { AuthHandler as space.siy.dj.yakamochi.auth.AuthProvider }
        })
    }
    Database.connect("jdbc:postgresql://${System.getenv("DB_HOST")}:${System.getenv("DB_PORT")}/${System.getenv("DB_NAME")}", driver = "org.postgresql.Driver",
            user = System.getenv("DB_USER"), password = System.getenv("DB_PASSWORD"))
    val jda = JDABuilder.createDefault(System.getenv("token")).build()
    jda.addEventListener(space.siy.dj.yakamochi.music2.EventHandler())
//    jda.addEventListener(DMHandler())
}


fun Any.logger(): Logger = LoggerFactory.getLogger(this::class.java)