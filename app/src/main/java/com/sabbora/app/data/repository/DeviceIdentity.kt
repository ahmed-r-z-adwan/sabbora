package com.sabbora.app.data.repository

import android.content.Context
import java.util.UUID

/**
 * A stable id for this installation, written once on first launch.
 *
 * Every record stores this as `updatedBy`, which is what lets a conflict later be
 * described as "this device against that one" instead of just "something changed".
 * It is a random UUID on purpose: no device name, no account, nothing identifying.
 */
object DeviceIdentity {
    private const val PREFS = "sabbora_identity"
    private const val KEY = "deviceId"

    fun get(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY, null)?.let { return it }
        val fresh = UUID.randomUUID().toString()
        prefs.edit().putString(KEY, fresh).apply()
        return fresh
    }
}
