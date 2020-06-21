package space.siy.dj.yakamochi.music2.track

/**
 * @author SIY1121
 */
data class TrackHistory(
        val id: Int,
        val title: String,
        val thumbnail: String,
        val url: String,
        val duration: Int,
        val author: String,
        val done: Boolean
)