package space.siy.dj.yakamochi.music2.track

/**
 * @author SIY1121
 */
interface TrackMetadataProvider {
    val url: String
    val title: String
    val duration: Int
    val thumbnail: String
}