package space.siy.dj.yakamochi.music2

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @author SIY1121
 */
interface VideoSourceInfo {
    val asr: Int
    val tbr: Int
    val abr: Int
    val format: String
    val formatId: String
    val formatNote: String?
    val ext: String
    val vcodec: String?
    val acodec: String?
    val width: Int
    val height: Int
    val filesize: Int
    val fps: Int
    val url: String
}

data class VideoSourceInfoImpl(
        override val asr: Int,
        override val tbr: Int,
        override val abr: Int,
        override val format: String,
        override val formatId: String,
        override val formatNote: String?,
        override val ext: String,
        override val vcodec: String?,
        override val acodec: String?,
        override val width: Int,
        override val height: Int,
        override val filesize: Int,
        override val fps: Int,
        override val url: String
) : VideoSourceInfo