package space.siy.dj.yakamochi.music2.track

import com.sapher.youtubedl.mapper.VideoFormat
import com.sapher.youtubedl.mapper.VideoInfo
import space.siy.dj.yakamochi.database.TrackHistory
import space.siy.dj.yakamochi.music2.audio.AudioProvider
import space.siy.dj.yakamochi.music2.remote.YoutubeDLFFmpegRemoteAudioProvider

/**
 * @author SIY1121
 */
class YoutubeDLTrack<T : AudioProvider>(trackHistory: TrackHistory, private val formats: List<VideoFormat>) : Track<T>(trackHistory) {
    override fun prepareRemoteAudio() = YoutubeDLFFmpegRemoteAudioProvider(trackHistory, formats)
}