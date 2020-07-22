package space.siy.dj.yakamochi.auth

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.koin.core.KoinComponent
import org.koin.core.inject
import space.siy.dj.yakamochi.database.GuildRepository
import space.siy.dj.yakamochi.database.UserRepository

/**
 * @author SIY1121
 */
class DMHandler : ListenerAdapter(), KoinComponent {
    private val authProvider: AuthProvider by inject()
    private val userRepository: UserRepository by inject()

    override fun onPrivateMessageReceived(event: PrivateMessageReceivedEvent) {
        if (event.author.id == event.jda.selfUser.id) return
        event.checkDB()
        event.channel.sendMessage(authProvider.requestAuth(event.author.id, AuthType.Self)).queue()
    }

    private fun PrivateMessageReceivedEvent.checkDB() {
        if (userRepository.find(author.id) == null)
            userRepository.new(author.id, author.name, author.avatarUrl ?: "")
    }
}