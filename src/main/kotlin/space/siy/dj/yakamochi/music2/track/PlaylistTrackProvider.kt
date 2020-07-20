package space.siy.dj.yakamochi.music2.track

import kotlinx.coroutines.coroutineScope
import space.siy.dj.yakamochi.music2.VideoInfo
import space.siy.dj.yakamochi.music2.audio.AudioProvider
import space.siy.dj.yakamochi.music2.remote.RemoteAudioProvider
import space.siy.dj.yakamochi.music_service.MusicServiceManager

/**
 * @author SIY1121
 */
class PlaylistTrackProvider<T : AudioProvider>(val guildID: String) : TrackProvider<T> {
    override var audioProviderCreator: ((remoteAudioProvider: RemoteAudioProvider) -> T)? = null

    private var playlistItems: List<VideoInfo>? = null
    private var author: String? = null

    private var pos = 0

    var random = false
    var repeat = false

    var playMap: BooleanArray? = null

    private var doneCallback: (() -> Unit)? = null

    fun clearPlaylist() {
        playlistItems = null
        author = null
        random = false
        repeat = false
        doneCallback?.invoke()
        doneCallback = null
    }

    suspend fun setPlaylist(url: String, author: String, doneCallback: (() -> Unit)? = null) {
        if (playlistItems != null) clearPlaylist()
        playlistItems = MusicServiceManager.playlist(url)
        playMap = BooleanArray(playlistItems!!.size) { false }
        this.author = author
        this.doneCallback = doneCallback
    }

    override fun canProvide() = playlistItems != null

    override suspend fun requestTrack(): Track<T>? {
        return try {
            playlistItems?.let { playlistItems ->
                coroutineScope {
                    var targets = playlistItems.filterIndexed { index, _ -> !(playMap?.get(index) ?: true) }
                    when {
                        targets.isEmpty() && repeat -> {
                            playMap = playMap?.map { false }?.toBooleanArray()
                            targets = playlistItems
                        }
                        targets.isEmpty() && !repeat -> {
                            clearPlaylist()
                            return@coroutineScope null
                        }
                    }
                    val track = if (random)
                        targets.random()
                    else
                        targets.first()

                    playMap?.set(playlistItems.indexOf(track), true)
                    Track.newTrack<T>(track.url, author!!, guildID).apply {
                        prepareAudio { audioProviderCreator!!(it) }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            requestTrack()
        }
    }

}