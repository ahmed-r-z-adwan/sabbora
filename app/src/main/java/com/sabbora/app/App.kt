package com.sabbora.app

import android.app.Application
import com.sabbora.app.data.local.SabboraDatabase
import com.sabbora.app.data.repository.DeviceIdentity
import com.sabbora.app.data.repository.SabboraRepository

/**
 * Holds the one repository the app uses.
 *
 * Hand-wired rather than injected: there is a single dependency graph, it is three lines
 * long, and a DI framework here would be more moving parts than the thing it wires. If the
 * sync layer grows its own set of collaborators this is the place that changes.
 */
class SabboraApp : Application() {
    val repository: SabboraRepository by lazy {
        SabboraRepository(
            db = SabboraDatabase.get(this),
            deviceId = DeviceIdentity.get(this),
        )
    }
}
