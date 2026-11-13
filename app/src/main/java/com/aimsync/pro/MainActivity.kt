package com.aimsync.pro

import android.app.Activity
import android.os.Bundle
import com.aimsync.pro.ui.AimSyncDashboard

class MainActivity : Activity() {

    private lateinit var dashboard: AimSyncDashboard

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dashboard = AimSyncDashboard(this)
        setContentView(dashboard.build())
    }

    override fun onPause() {
        super.onPause()
        dashboard.stop()
    }

    override fun onDestroy() {
        dashboard.stop()
        super.onDestroy()
    }
}
