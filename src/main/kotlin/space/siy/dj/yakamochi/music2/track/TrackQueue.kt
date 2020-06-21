package space.siy.dj.yakamochi.music2.track

/**
 * @author SIY1121
 */
@ExperimentalStdlibApi
class TrackQueue {
    val queue = ArrayDeque<Track>()
    val listeners: MutableList<() -> Unit> = mutableListOf()

    fun addTrack(url: String, author: String) {
        queue.add(Track.newYoutubeDLTrack(url, author))
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