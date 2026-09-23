package com.sabbora.app.data.local

import androidx.room.TypeConverter
import com.sabbora.app.domain.model.AttendanceStatus
import com.sabbora.app.domain.model.SyncState

/** Enums are stored by name, not ordinal, so reordering the enum cannot corrupt saved rows. */
class Converters {
    @TypeConverter
    fun syncStateToString(value: SyncState): String = value.name

    @TypeConverter
    fun stringToSyncState(value: String): SyncState = SyncState.valueOf(value)

    @TypeConverter
    fun attendanceStatusToString(value: AttendanceStatus): String = value.name

    @TypeConverter
    fun stringToAttendanceStatus(value: String): AttendanceStatus = AttendanceStatus.valueOf(value)
}
