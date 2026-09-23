package com.sabbora.app.data.local

import androidx.room.ColumnInfo
import com.sabbora.app.domain.model.SyncState

/**
 * Sync bookkeeping carried by every table in the database.
 *
 * Embedded rather than inherited so the columns are real columns and can be indexed
 * and queried directly — `WHERE isDeleted = 0 AND syncState = 'PENDING'` is the query
 * the sync worker will live on.
 *
 * @param updatedAt epoch millis of the last local edit; the ordering used to resolve
 *   conflicts under last-write-wins.
 * @param updatedBy identifier of the device or teacher that made the edit, so a
 *   conflict can be explained after the fact rather than silently discarded.
 * @param isDeleted soft delete. A row is never removed, because a deletion has to
 *   travel to the other devices like any other change.
 */
data class SyncMeta(
    @ColumnInfo(name = "updatedAt") val updatedAt: Long,
    @ColumnInfo(name = "updatedBy") val updatedBy: String,
    @ColumnInfo(name = "syncState") val syncState: SyncState = SyncState.PENDING,
    @ColumnInfo(name = "isDeleted") val isDeleted: Boolean = false,
)
