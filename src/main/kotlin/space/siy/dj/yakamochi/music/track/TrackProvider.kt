package space.siy.dj.yakamochi.music.track

import space.siy.dj.yakamochi.Outcome
import space.siy.dj.yakamochi.music.audio.AudioProvider
import space.siy.dj.yakamochi.music.remote.RemoteAudioProvider
import space.siy.dj.yakamochi.music.service.MusicService

/**
 * @author SIY1121
 */

/**
 * 再生するトラックを供給する責務を持つ
 */
interface TrackProvider<T: AudioProvider> {

    /**
     * TrackProviderが発生させる可能性のあるエラーを表す
     */
    sealed class ErrorReason {
        object NoTrack: ErrorReason()
        data class MusicServiceError(val reason: MusicService.ErrorReason): ErrorReason()
        object Unhandled: ErrorReason()
        object NoAudioProviderCreatorFound: ErrorReason()
    }

    /**
     * AudioProviderを作成するための関数
     */
    var audioProviderCreator: ((remoteAudioProvider: RemoteAudioProvider) -> T)?

    /**
     * 供給できるトラックがあるかを返す
     */
    fun canProvide(): Boolean

    /**
     * トラックをリクエストする
     */
    suspend fun requestTrack(): Outcome<Track<T>, ErrorReason>
}