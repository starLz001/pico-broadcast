package com.broadcast.m0verify

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.pvr.tobservice.ToBServiceHelper
import com.pvr.tobservice.enums.PBS_DeviceControlEnum
import com.pvr.tobservice.enums.PBS_PackageControlEnum
import com.pvr.tobservice.enums.PBS_SwitchEnum
import com.pvr.tobservice.enums.PBS_SystemInfoEnum
import com.pvr.tobservice.impl.ToBServiceProxyImpl
import com.pvr.tobservice.interfaces.IIntCallback
import com.pvr.tobservice.interfaces.IToBServiceProxy
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.function.Consumer

// ============================================================
// PICO TobService import 已按 tobservicelib-4.3.24.aar 实际检查核实（任务卡 M0-FIX-01）：
//   真实包名 = com.pvr.tobservice（com.picoxr.tobservice 只是 Maven 坐标 groupId，不是类包名）
//   枚举在 .enums 子包；AIDL 回调在 .interfaces 子包；代理实现在 .impl 子包
//   证据：docs/m0/tobservicelib-4.3.24-classes.txt + docs/m0/M0-FIX-01-修复报告.md
// ============================================================

/**
 * M0 技术验证 Demo（对应 M0-技术验证清单.md 的 V1–V11）
 * 每按一个按钮 = 跑一项验证，日志同步写屏幕 + /sdcard/Android/data/<pkg>/files/m0.log
 */
class MainActivity : Activity(), SurfaceHolder.Callback {

    companion object {
        const val TAG = "M0Verify"
        const val LOG_TAIL_LINES = 200   // M0-FIX-02 §2.2：启动时回放的历史日志行数
        fun writeExternalLog(ctx: android.content.Context, msg: String) {
            try {
                val f = File(ctx.getExternalFilesDir(null), "m0.log")
                FileWriter(f, true).use { it.append(msg).append("\n") }
            } catch (t: Throwable) {
                Log.e(TAG, "writeExternalLog fail", t)
            }
        }
    }

    private lateinit var logView: TextView
    private lateinit var etPackage: EditText
    private lateinit var etApkPath: EditText
    private lateinit var surfaceView: SurfaceView
    // M0-FIX-02 §2.1：surfaceCreated 后缓存 holder.surface，供 pbsStartRecordScreenBySurface 使用
    private var previewSurface: Surface? = null
    private val handler = Handler(Looper.getMainLooper())

    private var bound = false
    private var recording = false

    // ---------- 生命周期 ----------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        replayLogTail()   // M0-FIX-02 §2.2：先回放历史日志（含开机自启写入的行），再开始追加本次日志
        log("M0 验证 demo 启动。请按 V1→V11 顺序点测，每步日志会自动落盘。")
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    // ---------- UI（纯代码，无布局文件） ----------

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val scroll = ScrollView(this)
        val btnPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(btnPanel)

        etPackage = EditText(this).apply {
            hint = "目标包名（V3启动/V4前台/V6杀进程/V8保活/V10 Home）"
            setText("com.example.targetgame")
        }
        etApkPath = EditText(this).apply {
            hint = "APK 路径（V7 静默安装验证）"
            setText("/sdcard/test.apk")
        }
        btnPanel.addView(etPackage)
        btnPanel.addView(etApkPath)

        fun btn(label: String, onClick: () -> Unit) {
            btnPanel.addView(Button(this).apply { text = label; setOnClickListener { runSafe(label, onClick) } })
        }

        btn("V1 绑定 TobService") { v1Bind() }
        btn("V2 读取系统信息(PUI版本)+枚举全量") { v2SystemInfo() }
        btn("V3 拉起目标App(带extra启动参数)") { v3StartActivity() }
        btn("V4 轮询前台App(2s×15次)") { v4FocusedApp() }
        btn("V5 开始录屏(Surface 1024×1024)") { v5StartRecord() }
        btn("V5 停止录屏") { v5StopRecord() }
        btn("V6 杀掉目标App进程") { v6Kill() }
        btn("V6 息屏 pbsScreenOff") { v6ScreenOff() }
        btn("V6 重启设备(会弹确认)") { v6Reboot() }
        btn("V7 枚举包管理枚举值+静默安装") { v7SilentInstall() }
        btn("V8 设置保活+自启(重启后看日志)") { v8KeepAlive() }
        btn("V9 设置DeviceOwner") { v9DeviceOwner() }
        btn("V10 设为目标App为Home(S_ON)") { v10Home(true) }
        btn("V10 恢复系统Home(S_OFF)") { v10Home(false) }
        btn("V11 内容版权校验 appCopyrightVerify") { v11Copyright() }
        btn("清空日志文件") { clearLog() }

        // M0-FIX-02 §2.1：TextureView → SurfaceView（官方示例为 surfaceHolder.getSurface() 形态，
        // TextureView 的 Surface(SurfaceTexture) 在真机上被受理但不渲染）
        surfaceView = SurfaceView(this).apply {
            holder.addCallback(this@MainActivity)
            layoutParams = LinearLayout.LayoutParams(480, 480)
        }
        btnPanel.addView(TextView(this).apply { text = "↓ V5 录屏预览窗口（SurfaceView）" })
        btnPanel.addView(surfaceView)

        logView = TextView(this).apply { setTextIsSelectable(true); textSize = 12f }

        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(logView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.2f))
        setContentView(root)
    }

    private fun runSafe(label: String, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            log("✗ $label 异常: ${t.javaClass.simpleName}: ${t.message}")
            Log.e(TAG, label, t)
        }
    }

    // ---------- 日志 ----------

    private fun log(msg: String) {
        val line = "[${SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())}] $msg"
        Log.i(TAG, line)
        writeExternalLog(this, line)
        runOnUiThread {
            logView.append(line + "\n")
        }
    }

    private fun clearLog() {
        try {
            File(getExternalFilesDir(null), "m0.log").delete()
            logView.text = ""
            log("日志文件已清空")
        } catch (t: Throwable) { log("清空失败: ${t.message}") }
    }

    /**
     * M0-FIX-02 §2.2：启动时把 m0.log 尾部（最近 LOG_TAIL_LINES 行）读进屏幕日志区，再追加分隔行。
     * 解决：开机自启的 AutoBootService 在 demo 界面打开前就写了日志，
     * 之前界面看不到历史行，V8 验证被迫用 adb cat 确认。
     * 注意：回放行直接进屏幕、不重复写文件（它们本来就在文件里）。
     */
    private fun replayLogTail() {
        try {
            val f = File(getExternalFilesDir(null), "m0.log")
            if (!f.exists() || f.length() == 0L) {
                logView.append("（无历史日志文件）\n—— 以下为本次启动 ——\n")
                return
            }
            val lines = f.readLines()
            val tail = if (lines.size > LOG_TAIL_LINES) lines.subList(lines.size - LOG_TAIL_LINES, lines.size) else lines
            logView.append(tail.joinToString(separator = "\n", postfix = "\n"))
            logView.append("（历史回放：最近 ${tail.size} 行 / 文件共 ${lines.size} 行）\n")
            logView.append("—— 以下为本次启动 ——\n")
        } catch (t: Throwable) {
            logView.append("（历史日志读取失败: ${t.javaClass.simpleName}: ${t.message}）\n—— 以下为本次启动 ——\n")
        }
    }

    // ---------- V1 绑定 TobService ----------

    private fun v1Bind() {
        log("V1 开始绑定 TobService…")
        // AAR 实锤（M0-FIX-01）：回调接口为 ToBServiceHelper.BindCallBack（CallBack 的 C 大写），
        // 唯一方法 bindCallBack(java.lang.Boolean)，true=绑定成功；不存在 onBindSuccess/onBindFailed
        ToBServiceHelper.getInstance().bindTobService(this, object : ToBServiceHelper.BindCallBack {
            override fun bindCallBack(result: Boolean?) {
                if (result == true) {
                    bound = true
                    log("✓ V1 绑定成功 bindCallBack(true)")
                } else {
                    bound = false
                    log("✗ V1 绑定失败 bindCallBack(false) —— 检查是否企业版系统、TobService 是否正常")
                }
            }
        })
    }

    /**
     * 取服务并包装为 IToBServiceProxy，未绑定/失败时返回 null 并提示。
     * AAR 实锤（M0-FIX-01）：getServiceBinder() 返回的是原始 AIDL 接口 IToBService（不是 IToBServiceProxy），
     * 须经 ToBServiceProxyImpl(IToBService) 包装 —— 官方类结构即为此设计（impl 包存在的意义）。
     * 返回类型保持 Any?，下游各 V 函数的 `as? IToBServiceProxy` 判定逻辑不变。
     */
    private fun binder(): Any? {
        if (!bound) { log("⚠ 尚未绑定成功，请先点 V1"); return null }
        return try {
            val service = ToBServiceHelper.getInstance().serviceBinder
            if (service == null) {
                log("✗ getServiceBinder 返回 null —— 服务可能已断开，请重新点 V1 绑定")
                null
            } else {
                ToBServiceProxyImpl(service)
            }
        } catch (t: Throwable) {
            log("✗ getServiceBinder 异常: ${t.message}"); null
        }
    }

    // ---------- V2 系统信息 ----------

    private fun v2SystemInfo() {
        val b = binder() ?: return
        // 先把枚举全量打出来（验证清单要求记录枚举可用性）
        try {
            PBS_SystemInfoEnum.values().forEach { log("V2 枚举 PBS_SystemInfoEnum: ${it.name}") }
        } catch (t: Throwable) { log("V2 枚举反射失败: ${t.message}") }

        val proxy = b as? IToBServiceProxy
        if (proxy == null) { log("✗ V2 无法转为 IToBServiceProxy"); return }
        val pui = proxy.pbsStateGetDeviceInfo(PBS_SystemInfoEnum.PUI_VERSION, 0)
        log("✓ V2 PUI 版本 = $pui  ← 回填矩阵「PUI版本」列")
    }

    // ---------- V3 拉起应用 ----------

    private fun v3StartActivity() {
        val b = binder() ?: return
        val pkg = etPackage.text.toString().trim()
        val extra = """{"sceneId":"level_3","launchId":"m0_test_001"}"""
        val proxy = b as? IToBServiceProxy ?: run { log("✗ V3 无法转为 IToBServiceProxy"); return }
        val ret = proxy.pbsStartActivity(pkg, "", "", extra, null, null, 0)
        log("V3 pbsStartActivity($pkg) 返回=$ret extra=$extra")
        log("  → 观察：目标App是否被拉起？extra 参数游戏侧能否收到？回填矩阵")
    }

    // ---------- V4 前台 App 轮询（计费的"目击通道"验证） ----------

    private fun v4FocusedApp() {
        val b = binder() ?: return
        val proxy = b as? IToBServiceProxy ?: run { log("✗ V4 无法转为 IToBServiceProxy"); return }
        log("V4 开始轮询前台App（每2s一次，共15次）。请在这期间手动切换几个App")
        var count = 0
        val task = object : Runnable {
            override fun run() {
                count++
                try {
                    val cn: ComponentName? = proxy.focusedApp
                    log("V4 第${count}次 前台=${cn?.packageName}/${cn?.className}")
                } catch (t: Throwable) {
                    log("V4 第${count}次 异常: ${t.message}")
                }
                if (count < 15) handler.postDelayed(this, 2000) else log("✓ V4 轮询结束")
            }
        }
        handler.post(task)
    }

    // ---------- V5 录屏（投屏/监控画面回传验证） ----------

    private fun v5StartRecord() {
        // M0-FIX-02 §2.1：改用 SurfaceView 的 holder.surface（与官方示例 surfaceHolder.getSurface() 一致）
        val surface = previewSurface
        if (surface == null || !surface.isValid) {
            log("✗ V5 预览 Surface 未就绪 —— SurfaceView 尚未完成创建，请等下方预览窗口出现后再点（不是静默失败）")
            return
        }
        val b = binder() ?: return
        val proxy = b as? IToBServiceProxy ?: run { log("✗ V5 无法转为 IToBServiceProxy"); return }
        log("V5 → pbsStartRecordScreenBySurface(holder.surface, 1024, 1024, 0) 调用中…")
        try {
            proxy.pbsStartRecordScreenBySurface(surface, 1024, 1024, 0)
            recording = true
            log("✓ V5 调用已受理（无异常返回）→ 观察下方预览窗口是否出现设备实时画面")
        } catch (e: RemoteException) {
            recording = false
            log("✗ V5 RemoteException（跨进程调用被拒/服务断开）: ${e.message} → 服务侧问题候选，请抓 logcat 附报告")
        }
    }

    private fun v5StopRecord() {
        val b = binder() ?: return
        val proxy = b as? IToBServiceProxy ?: run { log("✗ V5 无法转为 IToBServiceProxy"); return }
        try {
            proxy.pbsStopRecordScreenBySurface(0)
            recording = false
            log("✓ V5 已停止录屏（调用受理，无异常）")
        } catch (e: RemoteException) {
            log("✗ V5 停止录屏 RemoteException: ${e.message}")
        }
    }

    // ---------- SurfaceHolder.Callback（M0-FIX-02：缓存 holder.surface） ----------

    override fun surfaceCreated(holder: SurfaceHolder) {
        previewSurface = holder.surface
        log("V5 预览 Surface 已创建（holder.surface 就绪，可以点开始录屏）")
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        previewSurface = holder.surface
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        previewSurface = null
        log("V5 预览 Surface 已销毁（重新进入界面后需等 Surface 再创建）")
    }

    // ---------- V6 设备控制 ----------

    private fun v6Kill() {
        val b = binder() ?: return
        val pkg = etPackage.text.toString().trim()
        val proxy = b as? IToBServiceProxy ?: run { log("✗ V6 无法转为 IToBServiceProxy"); return }
        proxy.pbsKillAppsByPidOrPackageName(null, arrayOf(pkg), 0)
        log("V6 已请求杀掉 $pkg → 观察目标App是否退出")
    }

    private fun v6ScreenOff() {
        val b = binder() ?: return
        val proxy = b as? IToBServiceProxy ?: run { log("✗ V6 无法转为 IToBServiceProxy"); return }
        proxy.pbsScreenOff()
        log("V6 已请求息屏 → 观察设备屏幕")
    }

    private fun v6Reboot() {
        val b = binder() ?: return
        AlertDialog.Builder(this)
            .setTitle("确认重启设备？")
            .setMessage("将调用 pbsControlSetDeviceAction(DEVICE_CONTROL_REBOOT)")
            .setPositiveButton("重启") { _, _ ->
                try {
                    val proxy = b as? IToBServiceProxy ?: run { log("✗ V6 无法转为 IToBServiceProxy"); return@setPositiveButton }
                    proxy.pbsControlSetDeviceAction(PBS_DeviceControlEnum.DEVICE_CONTROL_REBOOT, object : IIntCallback.Stub() {
                        override fun callback(result: Int) { log("V6 重启回调 result=$result") }
                    })
                    log("V6 重启指令已下发")
                } catch (t: Throwable) { log("✗ V6 重启异常: ${t.message}") }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ---------- V7 静默安装（枚举值文档未列，运行时反射+名称匹配） ----------

    private fun v7SilentInstall() {
        val b = binder() ?: return
        val path = etApkPath.text.toString().trim()
        // 1) 先全量打印枚举值 —— 这正是验证清单要的"枚举可用性"证据
        val enums = try {
            PBS_PackageControlEnum.values().also {
                it.forEach { e -> log("V7 枚举 PBS_PackageControlEnum: ${e.name}") }
            }
        } catch (t: Throwable) { log("✗ V7 枚举反射失败: ${t.message}"); return }

        // 2) 名称匹配找 INSTALL（不臆造枚举值，按名字找）
        val installEnum = enums.firstOrNull { it.name.contains("INSTALL", ignoreCase = true) && !it.name.contains("UNINSTALL", ignoreCase = true) }
        if (installEnum == null) { log("✗ V7 未找到名称含 INSTALL 的枚举，请把上方枚举全量回填矩阵"); return }

        val f = File(path)
        if (!f.exists()) { log("✗ V7 文件不存在: $path —— 先 adb push 一个测试APK到 /sdcard/"); return }

        val proxy = b as? IToBServiceProxy ?: run { log("✗ V7 无法转为 IToBServiceProxy"); return }
        proxy.pbsControlAPPManger(installEnum, path, 0, object : IIntCallback.Stub() {
            override fun callback(result: Int) { log("V7 静默安装回调 result=$result（0通常=成功）→ 检查App列表") }
        })
        log("V7 已请求安装 $path 使用枚举 ${installEnum.name}")
    }

    // ---------- V8 保活 + 开机自启 ----------

    private fun v8KeepAlive() {
        val b = binder() ?: return
        val proxy = b as? IToBServiceProxy ?: run { log("✗ V8 无法转为 IToBServiceProxy"); return }
        proxy.pbsAppKeepAlive(packageName, true, 0)
        log("V8 已对 demo 自身($packageName)开启保活")
        log("V8 开机自启验证：重启设备后看日志里是否出现 [V8] AutoBootService onCreate —— 出现即生效")
    }

    // ---------- V9 Device Owner ----------

    private fun v9DeviceOwner() {
        val b = binder() ?: return
        val proxy = b as? IToBServiceProxy ?: run { log("✗ V9 无法转为 IToBServiceProxy"); return }
        val cn = ComponentName(this, AdminReceiver::class.java)
        val ret = proxy.setDeviceOwner(cn)
        log("V9 setDeviceOwner 返回=$ret（0通常=成功）→ 成功后可再验证 禁用设置/锁定任务 等Owner能力")
    }

    // ---------- V10 自定义 Home ----------

    private fun v10Home(on: Boolean) {
        val b = binder() ?: return
        val pkg = etPackage.text.toString().trim()
        val proxy = b as? IToBServiceProxy ?: run { log("✗ V10 无法转为 IToBServiceProxy"); return }
        val sw = if (on) PBS_SwitchEnum.S_ON else PBS_SwitchEnum.S_OFF
        proxy.pbsAppSetAPPAsHomeTwo(sw, pkg)
        log("V10 pbsAppSetAPPAsHomeTwo(${sw.name}, $pkg) → ${if (on) "按Home键看是否进入目标App" else "按Home键看是否恢复系统桌面"}")
    }

    // ---------- V11 内容版权校验 ----------

    private fun v11Copyright() {
        val b = binder() ?: return
        val pkg = etPackage.text.toString().trim()
        val proxy = b as? IToBServiceProxy ?: run { log("✗ V11 无法转为 IToBServiceProxy"); return }
        try {
            // AAR 实锤（M0-FIX-01）：真实签名为 appCopyrightVerify(String, java.util.function.Consumer<Integer>)，
            // 不是文档记载的 IIntCallback.Stub 形态 —— 文档歧义已实锤，需回写研读纪要并向官方对接人确认口径
            proxy.appCopyrightVerify(pkg, Consumer { result ->
                log("✓ V11 appCopyrightVerify 回调 result=$result")
            })
            log("V11 已请求版权校验 $pkg（Consumer<Integer> 形态，AAR 实锤）")
        } catch (t: NoSuchMethodError) {
            log("✗ V11 NoSuchMethodError：设备端服务与 AAR 4.3.24 签名不一致 → 回填矩阵标注差异，问官方对接人")
            throw t
        }
    }
}
