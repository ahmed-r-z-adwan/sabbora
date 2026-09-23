package com.sabbora.app.data.repository

import com.sabbora.app.data.local.AssessmentEntity
import com.sabbora.app.data.local.AttendanceEntity
import com.sabbora.app.data.local.ClassEntity
import com.sabbora.app.data.local.SabboraDatabase
import com.sabbora.app.data.local.ScoreEntity
import com.sabbora.app.data.local.StudentEntity
import com.sabbora.app.data.local.SyncMeta
import com.sabbora.app.domain.model.AttendanceStatus
import com.sabbora.app.domain.model.SyncState
import com.sabbora.app.domain.report.AttendanceRate
import com.sabbora.app.domain.report.ScoreAverage
import com.sabbora.app.domain.report.StudentReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.util.UUID

/**
 * The only way the app writes to the database.
 *
 * Two invariants live here rather than in each caller, because forgetting either one is
 * what breaks sync later:
 *
 *  1. Every write stamps [SyncMeta] with the current time, this device, and
 *     [SyncState.PENDING]. A row that changed but still says `SYNCED` is invisible to the
 *     sync worker, which is silent data loss.
 *  2. Ids are UUIDs minted on the device, so two teachers working offline never collide.
 *
 * [now] is injected so the sync and conflict tests can control time instead of sleeping.
 */
class SabboraRepository(
    private val db: SabboraDatabase,
    private val deviceId: String,
    private val now: () -> Long = System::currentTimeMillis,
) {

    private fun meta() = SyncMeta(
        updatedAt = now(),
        updatedBy = deviceId,
        syncState = SyncState.PENDING,
        isDeleted = false,
    )

    // ---- classes ---------------------------------------------------------------

    fun observeClasses(): Flow<List<ClassEntity>> = db.classDao().observeAll()

    suspend fun addClass(name: String): String {
        val id = UUID.randomUUID().toString()
        db.classDao().upsert(ClassEntity(id = id, name = name.trim(), sync = meta()))
        return id
    }

    suspend fun renameClass(existing: ClassEntity, name: String) {
        db.classDao().upsert(existing.copy(name = name.trim(), sync = meta()))
    }

    suspend fun deleteClass(id: String) =
        db.classDao().softDelete(id = id, now = now(), by = deviceId)

    // ---- students --------------------------------------------------------------

    fun observeStudents(classId: String): Flow<List<StudentEntity>> =
        db.studentDao().observeByClass(classId)

    suspend fun addStudent(classId: String, name: String): String {
        val id = UUID.randomUUID().toString()
        db.studentDao().upsert(
            StudentEntity(id = id, classId = classId, name = name.trim(), sync = meta())
        )
        return id
    }

    suspend fun deleteStudent(id: String) =
        db.studentDao().softDelete(id = id, now = now(), by = deviceId)

    // ---- attendance ------------------------------------------------------------

    fun observeAttendance(classId: String, day: LocalDate): Flow<List<AttendanceEntity>> =
        db.attendanceDao().observeForClassOnDay(classId, day.toEpochDay())

    /**
     * Records or changes one student's status for one day.
     *
     * Reuses the existing row's id when there is one, so correcting a mark updates that
     * day instead of creating a second row for it. The unique index on
     * `(studentId, dateEpochDay)` would reject the duplicate anyway; doing it here means
     * the correction also carries a fresh `updatedAt` and travels as an update.
     */
    suspend fun mark(studentId: String, day: LocalDate, status: AttendanceStatus) {
        val epochDay = day.toEpochDay()
        val existing = db.attendanceDao().find(studentId, epochDay)
        db.attendanceDao().upsert(
            AttendanceEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                studentId = studentId,
                dateEpochDay = epochDay,
                status = status,
                sync = meta(),
            )
        )
    }

    // ---- assessments and scores ------------------------------------------------

    fun observeAssessments(classId: String): Flow<List<AssessmentEntity>> =
        db.assessmentDao().observeByClass(classId)

    suspend fun addAssessment(
        classId: String,
        title: String,
        maxScore: Double,
        date: LocalDate,
    ): String {
        require(maxScore > 0) { "an assessment worth $maxScore marks cannot be scored" }
        val id = UUID.randomUUID().toString()
        db.assessmentDao().upsert(
            AssessmentEntity(
                id = id,
                classId = classId,
                title = title.trim(),
                maxScore = maxScore,
                dateEpochDay = date.toEpochDay(),
                sync = meta(),
            )
        )
        return id
    }

    fun observeScores(assessmentId: String): Flow<List<ScoreEntity>> =
        db.scoreDao().observeForAssessment(assessmentId)

    /**
     * Sets a student's mark on one assessment, reusing the existing row so an edited mark
     * syncs as an update instead of a duplicate.
     */
    suspend fun setScore(assessmentId: String, studentId: String, value: Double) {
        val existing = db.scoreDao().find(assessmentId, studentId)
        db.scoreDao().upsert(
            ScoreEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                assessmentId = assessmentId,
                studentId = studentId,
                value = value,
                sync = meta(),
            )
        )
    }

    // ---- reports ---------------------------------------------------------------

    /**
     * The class report, assembled from three aggregate queries rather than from the raw
     * attendance and score rows.
     */
    fun observeReport(classId: String): Flow<List<StudentReport>> = combine(
        db.studentDao().observeByClass(classId),
        db.attendanceDao().observeTallies(classId),
        db.scoreDao().observeAverages(classId),
    ) { students, tallies, averages ->
        val tallyByStudent = tallies.associateBy { it.studentId }
        val scoreByStudent = averages.associateBy { it.studentId }
        students.map { student ->
            val tally = tallyByStudent[student.id]
            val score = scoreByStudent[student.id]
            StudentReport(
                studentId = student.id,
                studentName = student.name,
                attendanceRate = tally?.let {
                    AttendanceRate.of(it.present, it.late, it.absent)
                },
                scoreAverage = score?.let { ScoreAverage.of(it.achieved, it.available) },
                daysMarked = tally?.let {
                    it.present + it.late + it.absent + it.excused
                } ?: 0,
            )
        }
    }

    // ---- sync status -----------------------------------------------------------

    /** How many local changes are still waiting for a server. Shown in the app bar. */
    fun observePendingCount(): Flow<Int> = db.syncDao().observePendingCount()
}
