package com.broadcast.m0verify

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/** V9 Device Owner 验证用接收器（空实现即可，setDeviceOwner 只要求 ComponentName 有效） */
class AdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Log.i("M0Verify", "DeviceAdmin enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Log.i("M0Verify", "DeviceAdmin disabled")
    }
}
