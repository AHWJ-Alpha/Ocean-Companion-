package com.projectocean.oceancompanion

import android.app.Application
import com.projectocean.oceancompanion.memory.OceanDatabase

class OceanApplication : Application() {
    val database: OceanDatabase by lazy { OceanDatabase.create(this) }
}
