package space.siy.dj.yakamochi.music

import org.jetbrains.exposed.sql.transactions.transaction
import space.siy.dj.yakamochi.music.database.TrackHistory
import java.nio.ByteBuffer

/**
 * @author SIY1121
 */
class DefaultPlayer : Player {
    override val nowPlaying: String
        get() = trackList[nowPlayingIndex]

    val crossFadeDuration = 2f
    val sampleCountIn20Ms = (48000 * 0.02f * 2).toInt()

    val tracksData = HashMap<String, TrackDataProvider>()
    val trackList = ArrayList<String>()

    var nowPlayingIndex = -1
    var skiping = -1f

    override suspend fun addTrack(_url: String) {
        val newTrackData = FFmpegTrackDataProvider(_url, true).apply {
            loadMetadata()
        }
        tracksData[_url] = newTrackData
        trackList.add(_url)
        if (nowPlayingIndex == -1) {
            nowPlayingIndex++
        }

        transaction {
            val history = TrackHistory.new {
                title = newTrackData.trackInfo.title
                url = newTrackData.trackInfo.url
                duration = newTrackData.trackInfo.duration.toInt()
                thumbnail = newTrackData.trackInfo.thumbnail
                done = false
            }
            newTrackData.historyID = history.id.value
        }
    }

    private fun doneTrack(id: Int) = transaction {
        val h = TrackHistory.findById(id) ?: return@transaction
        h.done = true
    }

    override fun skip() {
        val track = getTrackDataNext(0) ?: return
        skiping = crossFadeDuration
    }

    override fun provide20MsAudio(): ByteBuffer {
        val requiredInBytes = (sampleCountIn20Ms * Short.SIZE_BYTES)

        val buf = ByteBuffer.allocate(requiredInBytes)

        val nowPlayingTrackData = getTrackDataNext(0) ?: return buf

        var nowPlayingAudioData = nowPlayingTrackData.read20MsAudio()
        val remainingSec = if (skiping > 0) skiping else nowPlayingTrackData.trackInfo.duration - nowPlayingTrackData.providedPosition
        if (remainingSec <= crossFadeDuration) {
            val t = remainingSec / crossFadeDuration
            val nextTrackData = getTrackDataNext(1)
            // 次のトラックの準備ができていたらフェードインする
            nowPlayingAudioData = if (nextTrackData != null && nextTrackData.canRead20MsAudio()) {
                val nextTrackAudioData = nextTrackData.read20MsAudio()
                nowPlayingAudioData.mapIndexed { index, s ->
                    val volume = t - index / 48000f / 2
                    (s.fade(volume).toInt() + nextTrackAudioData[index].fade(1 - volume).toInt()).toShort()
                }.toShortArray()
            } else // 準備ができていない、または次のトラックが存在しない場合は何もしない
                nowPlayingAudioData.mapIndexed { index, s -> s.fade(t - index / 48000f / 2) }.toShortArray()
        }

        if (skiping > 0) skiping -= 0.02f

        if (remainingSec <= 0.03f) {
            tracksData.remove(nowPlaying)
            doneTrack(nowPlayingTrackData.historyID)
            nowPlayingIndex++
            skiping = -1f
        }

        buf.asShortBuffer().put(nowPlayingAudioData)
        return buf
    }

    private fun TrackDataProvider.read20MsAudio() = read(sampleCountIn20Ms)

    private fun TrackDataProvider.canRead20MsAudio() = canRead(sampleCountIn20Ms)

    private fun getTrackDataNext(offset: Int): TrackDataProvider? {
        if (nowPlayingIndex + offset < 0 || trackList.size <= nowPlayingIndex + offset) return null
        return tracksData[trackList[nowPlayingIndex + offset]]
    }

    private fun Short.fade(v: Float) = (this * (v)).toShort()

    override fun canProvide() = (getTrackDataNext(0)?.canRead20MsAudio()
            ?: false) || (getTrackDataNext(1)?.canRead20MsAudio() ?: false)
}