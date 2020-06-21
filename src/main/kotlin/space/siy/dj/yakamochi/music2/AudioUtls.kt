package space.siy.dj.yakamochi.music2

import java.nio.ShortBuffer

/**
 * @author SIY1121
 */

const val SAMPLE_RATE = 48000
const val CHANNEL_COUNT = 2

fun Number.secToSampleCount() = (this.toFloat() * SAMPLE_RATE * CHANNEL_COUNT).toInt()

fun Number.sampleCountToSec() = (this.toFloat() / SAMPLE_RATE / CHANNEL_COUNT)

fun ShortBuffer.toArray(): ShortArray {
    val arr = ShortArray(limit())
    get(arr)
    return arr
}