package space.siy.dj.yakamochi.gateway.discord

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.koin.core.KoinComponent
import org.koin.core.inject
import space.siy.dj.yakamochi.database.GuildRepository
import space.siy.dj.yakamochi.database.UserRepository

/**
 * @author SIY1121
 */

/**
 * Discord内すべてのイベントを各GuildHandlerに振り分ける
 */
@ExperimentalStdlibApi
class EventHandler : ListenerAdapter(), KoinComponent {

    private val receiverScope = CoroutineScope(Dispatchers.IO)

    private val userRepository: UserRepository by inject()
    private val guildRepository: GuildRepository by inject()

    private val guildHandlers = HashMap<String, GuildHandler>()

    override fun onMessageReceived(event: MessageReceivedEvent) {
        receiverScope.launch {
            if (event.author.id == event.jda.selfUser.id) return@launch
            event.checkDB()
            if (!event.channelType.isGuild) return@launch

            if (!event.message.isMentioned(event.jda.selfUser)) return@launch
            if (guildHandlers[event.guild.id] == null) {
                guildHandlers[event.guild.id] = GuildHandler(event.guild.id, event.jda.selfUser.id)
            }
            guildHandlers[event.guild.id]?.onMessageReceived(event)
        }
    }

    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        receiverScope.launch {
            if (guildHandlers[event.guild.id] == null) {
                guildHandlers[event.guild.id] = GuildHandler(event.guild.id, event.jda.selfUser.id)
            }
            guildHandlers[event.guild.id]?.onMessageReactionAdd(event)
        }
    }

    override fun onMessageReactionRemove(event: MessageReactionRemoveEvent) {
        receiverScope.launch {
            if (guildHandlers[event.guild.id] == null) {
                guildHandlers[event.guild.id] = GuildHandler(event.guild.id, event.jda.selfUser.id)
            }
            guildHandlers[event.guild.id]?.onMessageReactionRemove(event)
        }
    }

    private fun MessageReceivedEvent.checkDB() {
        if (channelType.isGuild && guildRepository.find(guild.id) == null)
            guildRepository.new(guild.id, guild.name, guild.iconUrl ?: "")
        if (userRepository.find(author.id) == null)
            userRepository.new(author.id, author.name, author.avatarUrl ?: "")
    }
}

fun String.matchUrl(): String? {
    val regex = Regex("http(s)?:\\/\\/([\\w-]+\\.)+[\\w-]+(\\/[\\w- .\\/?%&=]*)?")
    return regex.find(this)?.value
}
