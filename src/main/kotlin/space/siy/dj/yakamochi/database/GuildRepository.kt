package space.siy.dj.yakamochi.database

/**
 * @author SIY1121
 */
interface GuildRepository {
    fun new(id: String, name: String, icon: String)
    fun find(id: String): Guild?
    fun update(id: String, name: String, icon: String)
}