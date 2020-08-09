package space.siy.dj.yakamochi.music2

/**
 * @author SIY1121
 */

/**
 * リソースのIDを表す
 */
interface VideoInfo {
    /**
     * リソースのUrl
     */
    val url: String

    /**
     * リソースのタイトル
     */
    val title: String

    /**
     * サムネイルUrl
     */
    val thumbnail: String

    /**
     * リソースの長さ（秒）
     * ライブ配信の場合は0
     */
    val duration: Float
}