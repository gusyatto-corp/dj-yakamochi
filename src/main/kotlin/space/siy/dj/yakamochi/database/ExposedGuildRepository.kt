package space.siy.dj.yakamochi.database

import org.jetbrains.exposed.sql.transactions.transaction

/**
 * @author SIY1121
 */
class ExposedGuildRepository : GuildRepository {
    override fun new(id: String, name: String, icon: String) {
        transaction {
            GuildDAO.new(id) {
                this.name = name
                this.icon = icon
            }
        }
    }

    override fun find(id: String): Guild? = transaction {
        GuildDAO.findById(id)?.transform()
    }

    override fun update(id: String, name: String, icon: String) {
        transaction {
            GuildDAO.findById(id)?.apply {
                this.name = name
                this.icon = icon
            }
        }
    }

    private fun GuildDAO.transform() = Guild(this.id.value, this.name, this.icon)
}