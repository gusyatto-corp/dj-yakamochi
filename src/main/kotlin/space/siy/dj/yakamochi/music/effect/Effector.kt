package space.siy.dj.yakamochi.music.effect

/**
 * @author SIY1121
 */

/**
 * 音源にエフェクトを掛ける
 */
class Effector {
    interface Effect {
        fun exec(l: Short, r: Short, t: Float): Pair<Short, Short>
    }

    data class EffectData(val effect: Effect, val start: Int, val end: Int)

    private val effects = ArrayList<EffectData>()

    /**
     * 指定された位置にエフェクトをスケジュールする
     */
    fun scheduleEffect(effect: Effect, start: Int, end: Int) {
        effects.add(EffectData(effect, start, end))
    }

    fun clearEffects() {
        effects.clear()
    }

    fun exec(data: ShortArray, pos: Int): ShortArray {
        // 現在の範囲で掛ける必要のあるエフェクトを検出する
        val targetEffects = effects.filter { it.start < pos + data.size && pos < it.end }
        if (targetEffects.isEmpty()) return data // 掛ける必要がなければそのまま返す
        var res = data.map { it }.toShortArray()
        // エフェクトを掛ける（同時に複数の場合もあり）
        targetEffects.forEach { e ->
            for(i in res.indices step 2) {
                val t = 1 - (e.end - (pos + i)) / (e.end - e.start).toFloat()
                val r = e.effect.exec(res[i], res[i + 1], t)
                res[i] = r.first
                res[i + 1] = r.second
            }
        }
        return res
    }
}