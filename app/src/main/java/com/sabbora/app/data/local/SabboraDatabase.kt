package com.sabbora.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ClassEntity::class,
        StudentEntity::class,
        AttendanceEntity::class,
        AssessmentEntity::class,
        ScoreEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SabboraDatabase : RoomDatabase() {
    abstract fun classDao(): ClassDao
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun assessmentDao(): AssessmentDao
    abstract fun scoreDao(): ScoreDao
    abstract fun syncDao(): SyncDao

    companion object {
        @Volatile
        private var instance: SabboraDatabase? = null

        fun get(context: Context): SabboraDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SabboraDatabase::class.java,
                    "sabbora.db",
                )
                    // Write-ahead logging: a teacher taking the roll writes on the main
                    // flow while the sync worker reads in the background, and WAL lets those
                    // two happen without blocking each other.
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                    .also { instance = it }
            }
    }
}
