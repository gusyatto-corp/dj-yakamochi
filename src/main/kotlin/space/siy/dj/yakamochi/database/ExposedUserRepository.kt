package space.siy.dj.yakamochi.database

import org.jetbrains.exposed.sql.transactions.transaction

/**
 * @author SIY1121
 */
class ExposedUserRepository : UserRepository {
    override fun new(id: String, name: String, icon: String) {
        transaction {
            UserDAO.new(id) {
                this.name = name
                this.icon = icon
            }
        }
    }

    override fun find(id: String): User? = transaction {
        UserDAO.findById(id)?.transform()
    }

    override fun update(id: String, name: String, icon: String) {
        transaction {
            UserDAO.findById(id)?.apply {
                this.name = name
                this.icon = icon
            }
        }
    }


    private fun UserDAO.transform() = User(this.id.value, this.name, this.icon)
}