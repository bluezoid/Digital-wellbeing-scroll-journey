package com.achyutpal.scrolljourney.domain.calculator

/** Well-known heights used to make a distance feel real. Deliberately a short list (spec §21). */
enum class Landmark(val heightCentimetres: Long) {
    QUTUB_MINAR(7_300),
    GREAT_PYRAMID(13_900),
    EIFFEL_TOWER(33_000),
    BURJ_KHALIFA(82_800),
    MOUNT_EVEREST(884_900),
}

/** "[times] × [landmark]": e.g. 1.7 × Eiffel Tower, or 0.45 × Qutub Minar for short distances. */
data class Comparison(val landmark: Landmark, val times: Double)

object DistanceComparisons {

    /** Uses the tallest landmark the distance has passed, or the smallest one if none yet. */
    fun compare(distanceCentimetres: Long): Comparison? {
        if (distanceCentimetres <= 0) return null
        val landmark = Landmark.entries.lastOrNull { it.heightCentimetres <= distanceCentimetres }
            ?: Landmark.entries.first()
        return Comparison(landmark, distanceCentimetres.toDouble() / landmark.heightCentimetres)
    }
}
