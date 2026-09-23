package com.sabbora.app

import com.sabbora.app.domain.report.AttendanceRate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AttendanceRateTest {

    @Test
    fun `every day present is a full rate`() {
        assertEquals(1.0, AttendanceRate.of(present = 10, late = 0, absent = 0)!!, 1e-9)
    }

    @Test
    fun `every day absent is a zero rate, not null`() {
        assertEquals(0.0, AttendanceRate.of(present = 0, late = 0, absent = 7)!!, 1e-9)
    }

    @Test
    fun `a late student counts as having attended`() {
        // 8 present + 2 late out of 10 marked days is still full attendance.
        assertEquals(1.0, AttendanceRate.of(present = 8, late = 2, absent = 0)!!, 1e-9)
    }

    @Test
    fun `absences drag the rate down proportionally`() {
        assertEquals(0.75, AttendanceRate.of(present = 6, late = 0, absent = 2)!!, 1e-9)
        assertEquals(0.75, AttendanceRate.of(present = 5, late = 1, absent = 2)!!, 1e-9)
    }

    @Test
    fun `a student with no marked days has no rate`() {
        assertNull(AttendanceRate.of(present = 0, late = 0, absent = 0))
    }

    @Test
    fun `excused days are excluded, so an excused-only student still has no rate`() {
        // Excused is not a parameter: a student whose only records are excused absences
        // reaches this function as all zeros, and must read as "no data" rather than 0%.
        assertNull(AttendanceRate.of(present = 0, late = 0, absent = 0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative counts are rejected rather than silently averaged`() {
        AttendanceRate.of(present = -1, late = 0, absent = 5)
    }
}
