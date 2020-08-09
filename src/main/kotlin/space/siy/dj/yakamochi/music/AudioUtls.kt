package space.siy.dj.yakamochi.music

import java.nio.ShortBuffer

/**
 * @author SIY1121
 */

const val SAMPLE_RATE = 48000
const val CHANNEL_COUNT = 2

/**
 * 秒数をサンプル数に変換する
 */
fun Number.secToSampleCount() = (this.toFloat() * SAMPLE_RATE * CHANNEL_COUNT).toInt()

/**
 * サンプル数から秒数に変換する
 */
fun Number.sampleCountToSec() = (this.toFloat() / SAMPLE_RATE / CHANNEL_COUNT)

fun ShortBuffer.toArray(size: Int = -1): ShortArray {
    position(0)
    val arr = ShortArray(if (size < 0) limit() else size)
    get(arr)
    return arr
}