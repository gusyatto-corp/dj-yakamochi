package space.siy.dj.yakamochi.music2.effect

import kotlin.math.max
import kotlin.math.min

/**
 * @author SIY1121
 */
class Effector {
    interface Effect {
        fun exec(l: Short, r: Short, t: Float): Pair<Short, Short>
    }

    data class EffectData(val effect: Effect, val start: Int, val end: Int)

    private val effects = ArrayList<EffectData>()

    fun scheduleEffect(effect: Effect, start: Int, end: Int) {
        effects.add(EffectData(effect, start, end))
    }

    fun clearEffects() {
        effects.clear()
    }

    fun exec(data: ShortArray, pos: Int): ShortArray {
        val targetEffects = effects.filter { it.start < pos + data.size && pos < it.end }
        if (targetEffects.isEmpty()) return data
        var res = data.map { it }.toShortArray()
        targetEffects.forEach { e ->
            for(i in res.indices step 2) {
                val t = (e.end - (pos + i)) / (e.end - e.start).toFloat()
                val r = e.effect.exec(res[i], res[i + 1], t)
                res[i] = r.first
                res[i + 1] = r.second
            }
        }
        return res
    }
}