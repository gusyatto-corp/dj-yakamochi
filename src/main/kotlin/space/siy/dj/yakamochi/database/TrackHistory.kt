package space.siy.dj.yakamochi.database

import org.joda.time.DateTime

/**
 * @author SIY1121
 */
data class TrackHistory(
        val id: Int,
        val title: String,
        val thumbnail: String,
        val url: String,
        val duration: Int,
        val author: User,
        val guild: Guild,
        val createdAt: DateTime,
        val done: Boolean
)