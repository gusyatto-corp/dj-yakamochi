package space.siy.dj.yakamochi

import net.dv8tion.jda.api.JDABuilder
import org.bytedeco.javacv.FFmpegLogCallback
import org.jetbrains.exposed.sql.Database
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import space.siy.dj.yakamochi.database.*

val repositoryModule = module {
    single { ExposedTrackHistoryRepository() as TrackHistoryRepository }
    single { ExposedUserRepository() as UserRepository }
    single { ExposedGuildRepository() as GuildRepository }
}

@ExperimentalStdlibApi
fun main(args: Array<String>) {
    System.setProperty("org.bytedeco.javacpp.logger", "slf4j")
    FFmpegLogCallback.set()
    startKoin {
        printLogger()
        modules(repositoryModule)
    }
    Database.connect("jdbc:postgresql://${System.getenv("DB_HOST")}:${System.getenv("DB_PORT")}/${System.getenv("DB_NAME")}", driver = "org.postgresql.Driver",
            user = System.getenv("DB_USER"), password = System.getenv("DB_PASSWORD"))
    val jda = JDABuilder.createDefault(System.getenv("token")).build()
    jda.addEventListener(space.siy.dj.yakamochi.music2.EventHandler())
}


fun Any.logger(): Logger = LoggerFactory.getLogger(this::class.java)