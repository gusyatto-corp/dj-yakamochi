package space.siy.dj.yakamochi.music2.track

import kotlinx.coroutines.coroutineScope
import org.koin.ext.scope
import space.siy.dj.yakamochi.Outcome
import space.siy.dj.yakamochi.music2.VideoInfo
import space.siy.dj.yakamochi.music2.audio.AudioProvider
import space.siy.dj.yakamochi.music2.remote.RemoteAudioProvider
import space.siy.dj.yakamochi.music_service.MusicService
import space.siy.dj.yakamochi.music_service.MusicServiceManager

/**
 * @author SIY1121
 */
class PlaylistTrackProvider<T : AudioProvider>(val guildID: String) : TrackProvider<T> {
    sealed class ErrorReason {
        data class MusicServiceError(val reason: MusicService.ErrorReason) : ErrorReason()
        object Unhandled : ErrorReason()
    }

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

    suspend fun setPlaylist(url: String, author: String, doneCallback: (() -> Unit)? = null): Outcome<Unit, ErrorReason> {
        if (playlistItems != null) clearPlaylist()
        playlistItems = when (val r = MusicServiceManager.playlist(url)) {
            is Outcome.Success -> r.result
            is Outcome.Error -> return Outcome.Error(ErrorReason.MusicServiceError(r.reason), r.cause)
        }

        playMap = BooleanArray(playlistItems!!.size) { false }
        this.author = author
        this.doneCallback = doneCallback

        return Outcome.Success(Unit)
    }

    override fun canProvide() = playlistItems != null

    override suspend fun requestTrack() = runCatching<Outcome<Track<T>, TrackProvider.ErrorReason>> {
        val playlistItems = this.playlistItems
                ?: return@runCatching Outcome.Error(TrackProvider.ErrorReason.NoTrack, null)
        var targets = playlistItems.filterIndexed { index, _ -> !(playMap?.get(index) ?: true) }
        when {
            targets.isEmpty() && repeat -> {
                playMap = playMap?.map { false }?.toBooleanArray()
                targets = playlistItems
            }
            targets.isEmpty() && !repeat -> {
                clearPlaylist()
                return@runCatching Outcome.Error(TrackProvider.ErrorReason.NoTrack, null)
            }
        }
        val track = if (random)
            targets.random()
        else
            targets.first()

        playMap?.set(playlistItems.indexOf(track), true)
        val res = when (val r = Track.newTrack<T>(track.url, author!!, guildID)) {
            is Outcome.Success -> r.result
            is Outcome.Error -> return@runCatching when (r.reason) {
                is Track.ErrorReason.MusicServiceError -> Outcome.Error(TrackProvider.ErrorReason.MusicServiceError(r.reason.reason), r.cause)
                is Track.ErrorReason.Unhandled -> Outcome.Error(TrackProvider.ErrorReason.Unhandled, r.cause)
            }
        }.apply {
            val r = prepareAudio { audioProviderCreator!!(it) }
            if (r is Outcome.Error) {
                remove()
                return@runCatching when (r.reason) {
                    is Track.ErrorReason.MusicServiceError -> Outcome.Error(TrackProvider.ErrorReason.MusicServiceError(r.reason.reason), r.cause)
                    is Track.ErrorReason.Unhandled -> Outcome.Error(TrackProvider.ErrorReason.Unhandled, r.cause)
                }
            }
        }

        Outcome.Success(res)
    }.recover {
        Outcome.Error(TrackProvider.ErrorReason.Unhandled, it)
    }.getOrThrow()

}