package com.sabbora.app

import com.sabbora.app.domain.report.ScoreAverage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScoreAverageTest {

    @Test
    fun `full marks is a full average`() {
        assertEquals(1.0, ScoreAverage.of(achieved = 25.0, available = 25.0)!!, 1e-9)
    }

    @Test
    fun `marks are weighted by how much each assessment was worth`() {
        // 20/20 on the final and 0/5 on the quiz is 80 percent, not the 50 percent that
        // averaging the two percentages would give.
        assertEquals(0.8, ScoreAverage.of(achieved = 20.0, available = 25.0)!!, 1e-9)
    }

    @Test
    fun `a student with nothing marked has no average`() {
        assertNull(ScoreAverage.of(achieved = 0.0, available = 0.0))
    }

    @Test
    fun `zero achieved out of real marks is zero, not null`() {
        assertEquals(0.0, ScoreAverage.of(achieved = 0.0, available = 30.0)!!, 1e-9)
    }
}
