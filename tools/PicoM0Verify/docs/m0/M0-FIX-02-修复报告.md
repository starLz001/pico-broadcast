# M0-FIX-02 修复报告：V5 录屏无画面修复 + 日志回放

> **执行方**：Claude Code　**日期**：2026-10-15　**状态**：✅ 编译通过，待真机复测 V5
> **前置**：基于 M0-FIX-01 修复后的可编译版本；未动 gradle 版本；未改其他 V 项逻辑；接口以 AAR 4.3.24 为准（`pbsStartRecordScreenBySurface(Surface,int,int,int)` / `pbsStopRecordScreenBySurface(int)` 签名未变，仅换 Surface 来源）

---

## 1. 改了哪几处（全部在 `MainActivity.kt`，共 7 处）

### §2.1 V5 改 SurfaceView（主任务）

| # | 位置 | 改动 |
|---|---|---|
| 1 | import 块 | 删 `android.graphics.SurfaceTexture`、`android.view.TextureView`；增 `android.view.SurfaceHolder`、`android.view.SurfaceView`、`android.os.RemoteException` |
| 2 | 类声明 | `Activity(), TextureView.SurfaceTextureListener` → `Activity(), SurfaceHolder.Callback`；删除 4 个 `onSurfaceTexture*` 空实现 |
| 3 | 字段 | `textureView: TextureView` → `surfaceView: SurfaceView` + `previewSurface: Surface?`（**surfaceCreated 后缓存 `holder.surface`**，surfaceDestroyed 置 null） |
| 4 | `buildUi()` | 预览窗口改为 `SurfaceView` + `holder.addCallback(this)`，尺寸 480×480 不变 |
| 5 | `v5StartRecord()` | ① 用缓存的 `holder.surface` 调 `pbsStartRecordScreenBySurface(surface, 1024, 1024, 0)`（官方示例形态）；② Surface 未就绪/失效时**明确日志提示**（"请等预览窗口出现后再点"），不静默；③ 调用前打 "调用中…"、成功打 "**✓ 调用已受理（无异常返回）**"、`RemoteException` 单独 catch 打 "**✗ RemoteException（跨进程被拒/服务断开）**" —— 受理/异常两种结局日志可辨 |
| 6 | `v5StopRecord()` | 对称处理：受理日志 + `RemoteException` 单独 catch |
| 7 | `SurfaceHolder.Callback` | 新增 `surfaceCreated`（缓存 surface + 日志"就绪，可以点开始录屏"）/ `surfaceChanged`（更新缓存）/ `surfaceDestroyed`（置 null + 日志） |

### §2.2 日志历史回放（顺带）

| # | 位置 | 改动 |
|---|---|---|
| 8 | `onCreate()` | `buildUi()` 后、首条新日志前调用新增的 `replayLogTail()` |
| 9 | 新增 `replayLogTail()` | 读 `m0.log` **尾部最近 200 行**（`LOG_TAIL_LINES=200`，companion 常量可调）直接显示到日志区（不重复写文件），随后追加统计行与分隔行 **`—— 以下为本次启动 ——`**；文件不存在/读取失败也有对应提示，分隔行始终存在 |
| 10 | `clearLog()` | 顺带一行：清空文件时同步清空屏幕日志区（否则回放机制下屏幕仍显示旧行，与"已清空"矛盾）——唯一的超范围小改动，如 QA 认为越界可单独回退 |

**效果**：重启设备后打开 demo，界面直接能看到开机时 AutoBootService 写入的 `[V8] AutoBootService onCreate` 历史行，不再需要 `adb cat`。

## 2. 自测报告

- `gradle :app:assembleDebug` → **BUILD SUCCESSFUL in 11s**（Gradle 8.5 + AGP 8.1.4 + Kotlin 1.9.20，版本未动），`compileDebugKotlin` 零错误
- 产物：`app/build/outputs/apk/debug/app-debug.apk`
- 真机行为（验收第 2/3/4 条）本环境无设备，**未执行**，待复测

## 3. 真机复测指引（验收 2–4 条）

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

1. **V5 复测**：打开 demo → V1 绑定 → 等日志出现"V5 预览 Surface 已创建" → 点"V5 开始录屏"。
   - 预期日志：`调用中…` → `✓ 调用已受理（无异常返回）`，且预览窗出现设备实时画面。
2. **若仍无画面**（受理但不渲染）：按验收第 3 条抓 logcat，**标"服务侧问题候选"，不硬绕**：
   ```bash
   adb logcat -c && adb logcat -v time > v5-logcat.txt
   # 期间点 V5 开始录屏，等 10 秒后 Ctrl+C，过滤：
   # 关注关键字：tobservice / tbservice / record / screenrecord / Surface /BufferQueue
   ```
   把 `v5-logcat.txt` 过滤结果附回报告，并记录当时日志区显示的是"已受理"还是"RemoteException"（两者指向不同结论：已受理+无画面=服务侧渲染问题候选；RemoteException=跨进程调用被拒）。
3. **日志回放验证**：重启设备 → 不点任何按钮先打开 demo → 界面历史区应出现 `[V8] AutoBootService onCreate` 行 + `—— 以下为本次启动 ——` 分隔行。

## 4. 验收标准对照

| 验收项 | 状态 |
|---|---|
| Build APK 成功 | ✅ 本环境已验证 |
| 真机 V5 预览窗出现实时画面 | ⏳ 待用户/QA 真机复测（§3.1 指引） |
| 仍无画面时 logcat 附报告标"服务侧问题候选" | 📋 抓取命令与判定口径已备好（§3.2） |
| 重启后界面可见 `[V8] AutoBootService onCreate` 历史行 | ⏳ 代码已实现，待真机复测（§3.3） |

## 5. 待确认问题（发回 Kimi）

1. 若 SurfaceView 版真机**仍**无画面且日志为"已受理"：结合 logcat 判断是否服务侧对录屏 Surface 有额外要求（如分辨率对齐、消费者权限），届时建议连同 v3 参考实现的实际表现一起提交官方对接人。
2. `clearLog()` 清屏一行属顺带改动（理由见 §1 表 #10），请 Kimi/QA 裁决保留或回退。
3. 真机复测结果（V5 画面截图/描述 + V8 回放截图）请回填本报告 §4 后再归档。
