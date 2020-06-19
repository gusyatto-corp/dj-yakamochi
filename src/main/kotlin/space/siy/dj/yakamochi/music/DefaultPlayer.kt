package space.siy.dj.yakamochi.music

import java.nio.ByteBuffer
import javax.sound.midi.Track

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


    override fun addTrack(url: String) {
        tracksData[url] = FFmpegTrackDataProvider(url, true).apply {
            load()
        }
        trackList.add(url)
        if (nowPlayingIndex == -1) {
            nowPlayingIndex++
        }
    }

    override fun provide20MsAudio(): ByteBuffer {
        val requiredInBytes = (sampleCountIn20Ms * Short.SIZE_BYTES).toInt()

        val buf = ByteBuffer.allocate(requiredInBytes)

        val nowPlayingTrackData = getTrackDataNext(0) ?: return buf

        var nowPlayingAudioData = nowPlayingTrackData.read20MsAudio()
        val tmp = nowPlayingTrackData.trackInfo.duration - nowPlayingTrackData.providedPosition
        if (tmp <= crossFadeDuration) {
            println(tmp)
            val t = tmp / crossFadeDuration
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

        if (tmp <= 0.03f) {
            nowPlayingIndex++
        }


        buf.asShortBuffer().put(nowPlayingAudioData)
        return buf
    }

    fun TrackDataProvider.read20MsAudio() = read(sampleCountIn20Ms)

    fun TrackDataProvider.canRead20MsAudio() = canRead(sampleCountIn20Ms)

    fun getTrackDataNext(offset: Int): TrackDataProvider? {
        if (trackList.size <= nowPlayingIndex + offset) return null
        return tracksData[trackList[nowPlayingIndex + offset]]
    }

    private fun Short.fade(v: Float) = (this * (v)).toShort()

    override fun canProvide() = (getTrackDataNext(0)?.canRead20MsAudio()
            ?: false) || (getTrackDataNext(1)?.canRead20MsAudio() ?: false)
}