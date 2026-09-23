package com.sabbora.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sabbora.app.domain.model.AttendanceStatus
import kotlinx.coroutines.flow.Flow

/**
 * Per-student counts for one class, produced by SQLite rather than by loading every
 * attendance row into memory. A class of 40 over a school year is ~7,000 rows; the
 * report only ever needs these four numbers per student.
 */
data class AttendanceTally(
    val studentId: String,
    val present: Int,
    val late: Int,
    val absent: Int,
    val excused: Int,
)

/** Average of a student's scores as a fraction of the marks available. */
data class ScoreAverage(
    val studentId: String,
    val achieved: Double,
    val available: Double,
)

@Dao
interface ClassDao {
    @Upsert
    suspend fun upsert(item: ClassEntity)

    @Query("SELECT * FROM classes WHERE isDeleted = 0 ORDER BY name")
    fun observeAll(): Flow<List<ClassEntity>>

    @Query("SELECT * FROM classes WHERE id = :id AND isDeleted = 0")
    fun observe(id: String): Flow<ClassEntity?>

    @Query(
        "UPDATE classes SET isDeleted = 1, syncState = 'PENDING', " +
            "updatedAt = :now, updatedBy = :by WHERE id = :id"
    )
    suspend fun softDelete(id: String, now: Long, by: String)
}

@Dao
interface StudentDao {
    @Upsert
    suspend fun upsert(item: StudentEntity)

    @Query("SELECT * FROM students WHERE classId = :classId AND isDeleted = 0 ORDER BY name")
    fun observeByClass(classId: String): Flow<List<StudentEntity>>

    @Query("SELECT COUNT(*) FROM students WHERE classId = :classId AND isDeleted = 0")
    fun countInClass(classId: String): Flow<Int>

    @Query(
        "UPDATE students SET isDeleted = 1, syncState = 'PENDING', " +
            "updatedAt = :now, updatedBy = :by WHERE id = :id"
    )
    suspend fun softDelete(id: String, now: Long, by: String)
}

@Dao
interface AttendanceDao {
    @Upsert
    suspend fun upsert(item: AttendanceEntity)

    /** The row for one student on one day, if the teacher has already marked it. */
    @Query(
        "SELECT * FROM attendance WHERE studentId = :studentId " +
            "AND dateEpochDay = :day AND isDeleted = 0 LIMIT 1"
    )
    suspend fun find(studentId: String, day: Long): AttendanceEntity?

    /** Today's marks for a whole class, so the roll-call screen is one query. */
    @Query(
        "SELECT a.* FROM attendance a INNER JOIN students s ON s.id = a.studentId " +
            "WHERE s.classId = :classId AND a.dateEpochDay = :day " +
            "AND a.isDeleted = 0 AND s.isDeleted = 0"
    )
    fun observeForClassOnDay(classId: String, day: Long): Flow<List<AttendanceEntity>>

    @Query(
        """
        SELECT s.id AS studentId,
               SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS present,
               SUM(CASE WHEN a.status = 'LATE'    THEN 1 ELSE 0 END) AS late,
               SUM(CASE WHEN a.status = 'ABSENT'  THEN 1 ELSE 0 END) AS absent,
               SUM(CASE WHEN a.status = 'EXCUSED' THEN 1 ELSE 0 END) AS excused
        FROM students s
        LEFT JOIN attendance a ON a.studentId = s.id AND a.isDeleted = 0
        WHERE s.classId = :classId AND s.isDeleted = 0
        GROUP BY s.id
        """
    )
    fun observeTallies(classId: String): Flow<List<AttendanceTally>>
}

@Dao
interface AssessmentDao {
    @Upsert
    suspend fun upsert(item: AssessmentEntity)

    @Query(
        "SELECT * FROM assessments WHERE classId = :classId AND isDeleted = 0 " +
            "ORDER BY dateEpochDay DESC"
    )
    fun observeByClass(classId: String): Flow<List<AssessmentEntity>>

    @Query(
        "UPDATE assessments SET isDeleted = 1, syncState = 'PENDING', " +
            "updatedAt = :now, updatedBy = :by WHERE id = :id"
    )
    suspend fun softDelete(id: String, now: Long, by: String)
}

@Dao
interface ScoreDao {
    @Upsert
    suspend fun upsert(item: ScoreEntity)

    @Query("SELECT * FROM scores WHERE assessmentId = :assessmentId AND isDeleted = 0")
    fun observeForAssessment(assessmentId: String): Flow<List<ScoreEntity>>

    /** The one row for a student on an assessment, so an edited mark reuses its id. */
    @Query(
        "SELECT * FROM scores WHERE assessmentId = :assessmentId " +
            "AND studentId = :studentId AND isDeleted = 0 LIMIT 1"
    )
    suspend fun find(assessmentId: String, studentId: String): ScoreEntity?

    @Query(
        """
        SELECT s.id AS studentId,
               COALESCE(SUM(sc.value), 0)      AS achieved,
               COALESCE(SUM(a.maxScore), 0)    AS available
        FROM students s
        LEFT JOIN scores sc     ON sc.studentId = s.id AND sc.isDeleted = 0
        LEFT JOIN assessments a ON a.id = sc.assessmentId AND a.isDeleted = 0
        WHERE s.classId = :classId AND s.isDeleted = 0
        GROUP BY s.id
        """
    )
    fun observeAverages(classId: String): Flow<List<ScoreAverage>>
}

/**
 * Cross-table view of what has not reached the server yet. The sync worker drains this,
 * and the UI shows its size so a teacher can tell at a glance whether their work is safe.
 */
@Dao
interface SyncDao {
    @Query(
        """
        SELECT (SELECT COUNT(*) FROM classes     WHERE syncState = 'PENDING')
             + (SELECT COUNT(*) FROM students    WHERE syncState = 'PENDING')
             + (SELECT COUNT(*) FROM attendance  WHERE syncState = 'PENDING')
             + (SELECT COUNT(*) FROM assessments WHERE syncState = 'PENDING')
             + (SELECT COUNT(*) FROM scores      WHERE syncState = 'PENDING')
        """
    )
    fun observePendingCount(): Flow<Int>
}
