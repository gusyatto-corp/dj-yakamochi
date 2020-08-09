package space.siy.dj.yakamochi.database

import org.joda.time.DateTime
import space.siy.dj.yakamochi.music.VideoInfo

/**
 * @author SIY1121
 */
data class TrackHistory(
        val id: Int,
        override val title: String,
        override val thumbnail: String,
        override val url: String,
        override val duration: Float,
        val author: User,
        val guild: Guild,
        val createdAt: DateTime,
        val done: Boolean
) : VideoInfo