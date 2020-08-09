package space.siy.dj.yakamochi.music2.effect

import space.siy.dj.yakamochi.music2.SAMPLE_RATE
import uk.me.berndporr.iirj.Butterworth
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * @author SIY1121
 */

object FadeIn : Effector.Effect {
    override fun exec(l: Short, r: Short, t: Float) = Pair((l * (sqrt(0.5f * t))).toShort(), (r * (sqrt(0.5f * t))).toShort())
}

object FadeOut : Effector.Effect {
    override fun exec(l: Short, r: Short, t: Float) = Pair((l * (sqrt(0.5f * (1 - t)))).toShort(), (r * (sqrt(0.5f * (1 - t)))).toShort())
}

object LowPass : Effector.Effect {
    private val filterL = Butterworth().apply { lowPass(2, SAMPLE_RATE.toDouble(), 1000.0) }
    private val filterR = Butterworth().apply { lowPass(2, SAMPLE_RATE.toDouble(), 1000.0) }
    override fun exec(l: Short, r: Short, _t: Float): Pair<Short, Short> {
        val t = if (_t > 0.5f) (0.5f - _t) * 2 else _t * 2
        return Pair(filterL.filter(l.toDouble()).toShort(), filterR.filter(r.toDouble()).toShort())
    }
}

object HighPass : Effector.Effect {
    private val filterL = Butterworth().apply { highPass(2, SAMPLE_RATE.toDouble(), 1000.0) }
    private val filterR = Butterworth().apply { highPass(2, SAMPLE_RATE.toDouble(), 1000.0) }
    override fun exec(l: Short, r: Short, _t: Float): Pair<Short, Short> {
        val t = if (_t > 0.5f) (0.5f - _t) * 2 else _t * 2
        return Pair(filterL.filter(l.toDouble()).toShort(), filterR.filter(r.toDouble()).toShort())
    }
}

class Gain(val scale: Float) : Effector.Effect {
    override fun exec(l: Short, r: Short, t: Float) = Pair(floor(l * scale).toShort(), floor(r * scale).toShort())
}