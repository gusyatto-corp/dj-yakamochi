package space.siy.dj.yakamochi.music2.player

import com.sapher.youtubedl.YoutubeDL
import space.siy.dj.yakamochi.music2.VideoInfo
import space.siy.dj.yakamochi.music2.track.Track
import space.siy.dj.yakamochi.music2.track.TrackQueue

/**
 * @author SIY1121
 */
interface PlayerAgent {
    @ExperimentalStdlibApi
    suspend fun requestNewTrack(trackQueue: TrackQueue)
}