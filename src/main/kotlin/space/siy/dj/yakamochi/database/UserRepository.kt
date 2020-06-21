package space.siy.dj.yakamochi.database

/**
 * @author SIY1121
 */
interface UserRepository {
    fun new(id: String, name: String, icon: String)
    fun find(id:String): User?
    fun update(id: String, name: String, icon: String)
}