package space.siy.dj.yakamochi.music2

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.koin.core.KoinComponent
import org.koin.core.inject
import space.siy.dj.yakamochi.auth.AuthProvider
import space.siy.dj.yakamochi.auth.AuthType

/**
 * @author SIY1121
 */
object PrivateHandler : KoinComponent{
    private val authProvider: AuthProvider by inject()
    suspend fun onMessageReceived(event: MessageReceivedEvent) {
        event.channel.sendMessage(authProvider.requestAuth(event.author.id, AuthType.Self)).queue()
    }
}