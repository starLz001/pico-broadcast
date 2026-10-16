# 任务卡 M0-FIX-02：PicoM0Verify V5 录屏无画面修复 + 日志回放

> **发卡方**：Kimi（spec-owner）　**执行方**：Claude Code　**验收方**：QA / Kimi
> **工程**：本机 `PicoM0Verify/`（M0-FIX-01 后的可编译版本）
> **优先级**：高——V5 是投屏监看的地基验证，M0 收尾前必须复测

## 1. 问题

真机（PICO 4 Ultra 企业版，PUI 5.15.7）实测：`pbsStartRecordScreenBySurface` **调用被受理（无异常，日志 ✓）**，但预览窗口始终无画面。

**诊断**：demo 当前用 TextureView 的 `Surface(SurfaceTexture)`；官方文档示例用的是 **SurfaceView**（`surfaceHolder.getSurface()`）。TobService 往 Surface 渲染时对 Surface 类型可能有要求。Kimi 已备有 v3 参考实现（`PicoM0Verify-验证demo工程.zip` 内 MainActivity.kt，SurfaceView + SurfaceHolder.Callback 版），**可参考但由你实现并自测**。

## 2. 任务内容

### 2.1 V5 改 SurfaceView（主任务）
- 预览窗口 TextureView → **SurfaceView**，`holder.addCallback`，在 `surfaceCreated` 后缓存 `holder.surface`
- `pbsStartRecordScreenBySurface(holder.surface, 1024, 1024, 0)`（与官方示例参数形态一致）
- 调用前后加明确日志：受理/RemoteException 分别可辨
- Surface 未就绪时按钮要提示而非静默

### 2.2 日志历史回放（顺带）
- 现象：开机自启的 AutoBootService 日志写进 m0.log，但打开 demo 界面看不到历史行（V8 验证时被迫用 adb cat 确认）
- 要求：`onCreate` 时把 m0.log 尾部（如最近 200 行）读进日志区，再追加新行；标记一行 `—— 以下为本次启动 ——` 分隔

### 2.3 禁止事项
- 不改其他 V 项逻辑；不动 gradle 版本；不臆造接口（一切以 AAR 4.3.24 为准）

## 3. 验收标准
- [ ] Build APK 成功
- [ ] 真机点 V5：预览窗出现设备实时画面（受理+渲染都通）
- [ ] 若仍无画面：抓 `adb logcat`（tbservice/record 相关）附在报告里，标"服务侧问题候选"，不要硬绕
- [ ] 重启设备后打开 demo，界面能看到 `[V8] AutoBootService onCreate` 历史行

## 4. 交付
修复后的工程 + 简短修复说明（改了哪几处、真机 V5 结果截图/描述）。
