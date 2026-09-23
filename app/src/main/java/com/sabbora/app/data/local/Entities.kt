package com.sabbora.app.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sabbora.app.domain.model.AttendanceStatus

/**
 * Primary keys are UUID strings generated on the device, not auto-increment integers.
 * Two teachers working offline on the same class must be able to add students without
 * colliding, which rules out a server-assigned or sequential id.
 */
@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey val id: String,
    val name: String,
    @Embedded val sync: SyncMeta,
)

@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("classId")],
)
data class StudentEntity(
    @PrimaryKey val id: String,
    val classId: String,
    val name: String,
    /** Free-text teacher note. Deliberately the only extra field: students are minors. */
    val note: String? = null,
    @Embedded val sync: SyncMeta,
)

/**
 * One row per student per day. The unique index is what makes a repeated tap idempotent:
 * marking the same student twice updates the row instead of creating a second one, which
 * matters when the same day is edited on two devices and then merged.
 */
@Entity(
    tableName = "attendance",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["studentId", "dateEpochDay"], unique = true),
        Index("dateEpochDay"),
    ],
)
data class AttendanceEntity(
    @PrimaryKey val id: String,
    val studentId: String,
    /** Days since 1970-01-01. Stored as a day, not an instant, so timezone cannot shift it. */
    val dateEpochDay: Long,
    val status: AttendanceStatus,
    @Embedded val sync: SyncMeta,
)

@Entity(
    tableName = "assessments",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("classId")],
)
data class AssessmentEntity(
    @PrimaryKey val id: String,
    val classId: String,
    val title: String,
    val maxScore: Double,
    val dateEpochDay: Long,
    @Embedded val sync: SyncMeta,
)

@Entity(
    tableName = "scores",
    foreignKeys = [
        ForeignKey(
            entity = AssessmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["assessmentId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["assessmentId", "studentId"], unique = true),
        Index("studentId"),
    ],
)
data class ScoreEntity(
    @PrimaryKey val id: String,
    val assessmentId: String,
    val studentId: String,
    val value: Double,
    @Embedded val sync: SyncMeta,
)
