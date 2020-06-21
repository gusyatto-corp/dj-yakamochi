package space.siy.dj.yakamochi.music2.track

import org.koin.core.KoinComponent
import org.koin.core.inject
import space.siy.dj.yakamochi.database.TrackHistoryRepository

/**
 * @author SIY1121
 */
@ExperimentalStdlibApi
class TrackQueue : KoinComponent {
    val queue = ArrayDeque<Track>()
    val listeners: MutableList<() -> Unit> = mutableListOf()
    val trackHistoryRepository by inject<TrackHistoryRepository>()

    init {
        trackHistoryRepository.listAll(false).forEach { queue.add(Track.newYoutubeDLTrack(it.url, it.author.id, it.guild.id)) }
    }

    fun addTrack(url: String, author: String, guild: String) {
        queue.add(Track.newYoutubeDLTrack(url, author, guild))
        notifyQueueChanged()
    }

    fun removeTrack(trackID: Int) {
        val targetTrack = queue.find { it.trackID == trackID } ?: return
        targetTrack.remove()
        queue.remove(targetTrack)
        notifyQueueChanged()
    }

    fun list(): List<Track> = queue

    operator fun get(index: Int) = if (queue.size <= index) null else queue[index]

    fun done() {
        val targetTrack = queue.removeFirst()
        targetTrack.done()
        notifyQueueChanged()
    }

    fun addQueueChangedListener(block: () -> Unit) {
        listeners.add(block)
    }

    fun removeQueueChangedListener(block: () -> Unit) {
        listeners.remove { block }
    }

    private fun notifyQueueChanged() = listeners.forEach { it() }
}