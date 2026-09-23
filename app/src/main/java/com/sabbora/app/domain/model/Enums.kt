package com.sabbora.app.domain.model

/**
 * Where a record stands relative to the server.
 *
 * Every record is created as [PENDING] on the device that made it and only becomes
 * [SYNCED] once the server has acknowledged it. The app never waits for the network
 * to let a teacher save something, so [PENDING] is the normal state, not an error.
 */
enum class SyncState { PENDING, SYNCED }

/**
 * A student's attendance for one day.
 *
 * [EXCUSED] is deliberately separate from [ABSENT]: an excused absence is removed from
 * the attendance rate entirely rather than counted against the student. See
 * [com.sabbora.app.domain.report.AttendanceRate].
 */
enum class AttendanceStatus { PRESENT, ABSENT, LATE, EXCUSED }
