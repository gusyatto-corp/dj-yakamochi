package space.siy.dj.yakamochi.music2.track

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import space.siy.dj.yakamochi.database.TrackHistoryRepository
import java.util.concurrent.LinkedBlockingQueue

/**
 * @author SIY1121
 */
@ExperimentalStdlibApi
class TrackQueue(val guild: String) : KoinComponent {
    val queue = ArrayDeque<Track>()
    val listeners: MutableList<() -> Unit> = mutableListOf()
    val trackHistoryRepository by inject<TrackHistoryRepository>()

    init {
        trackHistoryRepository.listAll(guild, false).forEach { queue.add(Track.fromHistory(it)) }
        notifyQueueChanged()
    }

    suspend fun addTrack(url: String, author: String, guild: String) = withContext(Dispatchers.IO) {
        queue.add(Track.newYoutubeDLTrack(url, author, guild))
        notifyQueueChanged()
    }

    suspend fun removeTrack(trackID: Int) = withContext(Dispatchers.IO) {
        val targetTrack = queue.find { it.trackID == trackID } ?: return@withContext
        targetTrack.remove()
        queue.remove(targetTrack)
        notifyQueueChanged()
    }

    fun list(): List<Track> = queue

    operator fun get(index: Int) = if (queue.size <= index) null else queue[index]

    suspend fun done() = withContext(Dispatchers.IO) {
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