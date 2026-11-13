package com.aimsync.pro.system

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast

object SystemOptimizer {

    private fun launch(context: Context, action: String) {
        val intent = Intent(action)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun openPointerSpeedSettings(context: Context) {
        try {
            launch(context, Settings.ACTION_INPUT_METHOD_SETTINGS)
            Toast.makeText(
                context,
                "Look for 'Pointer speed' in this menu (location varies by phone)",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            try {
                launch(context, Settings.ACTION_SETTINGS)
            } catch (e2: Exception) {
                Toast.makeText(context, "Could not open Settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openTouchDelaySettings(context: Context) {
        try {
            launch(context, Settings.ACTION_ACCESSIBILITY_SETTINGS)
            Toast.makeText(context, "Set Touch & Hold Delay to SHORT", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Open Accessibility > Touch & Hold Delay", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleGamingDnd(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) {
            Toast.makeText(context, "Grant DND access first", Toast.LENGTH_LONG).show()
            try {
                launch(context, Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open DND settings", Toast.LENGTH_SHORT).show()
            }
            return
        }
        if (nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL) {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            Toast.makeText(context, "Gaming DND ON", Toast.LENGTH_SHORT).show()
        } else {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            Toast.makeText(context, "Gaming DND OFF", Toast.LENGTH_SHORT).show()
        }
    }
}
