package com.sabbora.app.domain.report

/**
 * How an attendance rate is defined in this app.
 *
 * Two decisions are encoded here, and both are policy rather than arithmetic:
 *
 *  - **A late student attended.** Arriving late is a punctuality problem, not an
 *    absence, so [late] is counted in the numerator.
 *  - **An excused absence does not exist.** It is removed from the denominator instead
 *    of counted against the student, so a pupil with a medical note is not punished for
 *    having one. This is why the excused count is not a parameter.
 *
 * Returns `null`, not zero, when there is nothing to divide by: a student with no marked
 * days has no rate, and showing them 0% would read as perfect absence.
 */
object AttendanceRate {

    /** @return the fraction attended in `0.0..1.0`, or `null` when no day counts. */
    fun of(present: Int, late: Int, absent: Int): Double? {
        require(present >= 0 && late >= 0 && absent >= 0) {
            "attendance counts cannot be negative: present=$present late=$late absent=$absent"
        }
        val considered = present + late + absent
        if (considered == 0) return null
        return (present + late).toDouble() / considered
    }
}

/**
 * A student's score as a fraction of the marks that were available to them.
 *
 * Summing marks rather than averaging percentages is deliberate: a 20-mark final and a
 * 5-mark quiz should not carry the same weight.
 */
object ScoreAverage {

    /** @return the fraction achieved in `0.0..1.0`, or `null` when nothing was marked. */
    fun of(achieved: Double, available: Double): Double? {
        if (available <= 0.0) return null
        return achieved / available
    }
}

/** One row of the class report. A `null` figure means "not enough data", never zero. */
data class StudentReport(
    val studentId: String,
    val studentName: String,
    val attendanceRate: Double?,
    val scoreAverage: Double?,
    val daysMarked: Int,
)
