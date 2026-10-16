package com.broadcast.m0verify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * V8 开机自启验证服务。
 * 官方文档：声明 action 为 com.pvr.tobservice.SERVICE_AUTO_BOOT 的 Service，
 * 设备开机后由系统拉起。这里只做一件事：写日志证明"被拉起了"。
 */
class AutoBootService : Service() {

    companion object {
        const val TAG = "M0Verify-AutoBoot"
        const val CHANNEL_ID = "m0_autoboot"
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "M0自启验证", NotificationManager.IMPORTANCE_LOW)
        )
        val n: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("M0验证：AutoBootService 已启动")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        startForeground(1001, n)
        Log.i(TAG, "AutoBootService onCreate —— 开机自启生效")
        // 同步写一份到 m0.log，方便导出
        MainActivity.writeExternalLog(this, "[V8] AutoBootService onCreate at ${System.currentTimeMillis()} —— 开机自启生效")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
