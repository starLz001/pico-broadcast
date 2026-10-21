# 任务卡 M0-FIX-01：PicoM0Verify 编译修复（import/签名核对）

> **发卡方**：Kimi（spec-owner）　**执行方**：Claude Code　**验收方**：QA 测试 Agent
> **工程位置**：本机 `PicoM0Verify/`（Android Studio 工程，Gradle sync 已通过，AGP 8.1.4 + Kotlin 1.9.20 + Gradle 8.5）
> **铁律**：禁止凭记忆臆造 PICO 接口名/包名/签名。一切结论必须来自对 `tobservicelib-4.3.24.aar` 的实际检查（反编译/class 列表/javap）。

## 1. 背景与问题

`app/src/main/java/com/broadcast/m0verify/MainActivity.kt` 顶部 7 行 import 是按惯例推断的，编译不过：

```kotlin
import com.picoxr.tobservice.ToBServiceHelper        // ✗ 包名错误
import com.picoxr.tobservice.IToBServiceProxy
import com.picoxr.tobservice.IIntCallback
import com.picoxr.tobservice.PBS_SystemInfoEnum
import com.picoxr.tobservice.PBS_DeviceControlEnum
import com.picoxr.tobservice.PBS_PackageControlEnum
import com.picoxr.tobservice.PBS_SwitchEnum
```

**已确认线索**：Android Studio 反编译显示 `ToBServiceHelper.class` 的真实路径为 `com/pvr/tobservice/`（包名 `com.pvr.tobservice`）；其内部用 `ServiceConnection` 绑定服务。依赖本身已正常解析（`com.picoxr.tobservice:tobservicelib:4.3.24` 是 Maven 坐标，不是类包名）。

**已知无害报错可忽略**：`Could not find tobservicelib-4.3.24-sources.jar`（PICO 仓库不提供源码包，仅影响 IDE 源码预览，不影响编译）。

## 2. 任务内容

### 2.1 探明真实包结构（先侦察，后改码）

1. 找到本地缓存的 AAR：`~/.gradle/caches/modules-2/files-2.1/com.picoxr.tobservice/tobservicelib/4.3.24/*/tobservicelib-4.3.24.aar`（Windows 在 `%USERPROFILE%\.gradle\...`）。
2. 解压 AAR 取 `classes.jar`，列出全部类清单（`jar tf` 或解压后 `find`），**把完整类清单保存为 `docs/m0/tobservicelib-4.3.24-classes.txt`**。
3. 从类清单确认以下 7 个类的全限定名（注意可能在子包，如 `.aidl`、`.callback`、`.enums` 等）：
   `ToBServiceHelper` / `IToBServiceProxy` / `IIntCallback` / `IBoolCallback` / `PBS_SystemInfoEnum` / `PBS_DeviceControlEnum` / `PBS_PackageControlEnum` / `PBS_SwitchEnum`

### 2.2 核对公开方法签名

用 `javap` 或 Android Studio 反编译逐个核对 MainActivity.kt 中实际调用的方法，**以 AAR 为准**：

| 调用点 | 当前代码假设 | 需确认 |
|---|---|---|
| 绑定 | `ToBServiceHelper.getInstance().bindTobService(context, ToBServiceHelper.BindCallback)`，回调含 `onBindSuccess()/onBindFailed()` | 真实方法名、回调接口全名与方法名 |
| 取 binder | `ToBServiceHelper.getInstance().getServiceBinder()` | 真实方法名与返回类型 |
| 系统信息 | `pbsStateGetDeviceInfo(PBS_SystemInfoEnum.PUI_VERSION, 0)` | 方法所在类、枚举真实成员名 |
| 拉起应用 | `pbsStartActivity(String, String, String, String, String[], int[], int)` | 参数个数与类型 |
| 前台应用 | 强转 `IToBServiceProxy` 后 `getFocusedApp()` | 方法真实名字与返回类型 |
| 录屏 | `pbsStartRecordScreenBySurface(Surface, int, int, int)` / `pbsStopRecordScreenBySurface(int)` | 同上 |
| 设备控制 | `pbsControlSetDeviceAction(PBS_DeviceControlEnum.DEVICE_CONTROL_REBOOT, IIntCallback)` | 枚举真实成员名（重启/关机叫什么） |
| 包管理 | `pbsControlAPPManger(PBS_PackageControlEnum, String, int, IIntCallback)` | 方法名拼写、枚举全量成员（列出全部 values） |
| 杀进程 | `pbsKillAppsByPidOrPackageName(int[], String[], int)` | 同上 |
| 息屏 | `pbsScreenOff()` | 同上 |
| 保活 | `pbsAppKeepAlive(String, boolean, int)` | 同上 |
| DeviceOwner | `setDeviceOwner(ComponentName)` 返回 `int` | 同上 |
| Home替换 | `pbsAppSetAPPAsHomeTwo(PBS_SwitchEnum.S_ON/S_OFF, String)` | 枚举真实成员名 |
| 版权校验 | `appCopyrightVerify(String, IIntCallback.Stub)` | 真实签名（文档记载有歧义，以 AAR 为准） |

### 2.3 修复代码

- 修正全部 import 与调用点，使 **Build → Build APK(s) 成功**。
- 若某方法/枚举在 AAR 中不存在：不要改名硬凑，不要删功能静默跳过——在报告中标记 `缺失`，代码里用 `try/catch (Throwable)` 保留调用点并注释说明。
- `IIntCallback` 若是 AIDL 接口，回调写法保持 `object : IIntCallback.Stub() { override fun callback(result: Int) }`；若实际是 Java interface，按真实形态调整。
- 其余文件（AndroidManifest.xml、AutoBootService.kt、AdminReceiver.kt、gradle 文件）如发现编译问题一并修复。

### 2.4 禁止事项

- 禁止臆造包名/类名/方法名/枚举值（项目红线）。
- 禁止改业务验证逻辑（V1–V11 的按钮与验证步骤保持不变）。
- 禁止升级 AGP/Kotlin/Gradle 版本绕过问题。

## 3. 交付物（全部提交到工程内）

1. 修复后**可编译出 APK** 的工程（`Build APK(s)` 成功）。
2. `docs/m0/tobservicelib-4.3.24-classes.txt`：AAR 完整类清单。
3. `docs/m0/M0-FIX-01-修复报告.md`，必须包含：
   - 7 个类的真实全限定名对照表（推断值 → 实际值）；
   - §2.2 表格中每个调用点的真实签名（从 AAR 抄出，附证据来源如 `javap` 输出片段）；
   - `PBS_PackageControlEnum` / `PBS_DeviceControlEnum` / `PBS_SystemInfoEnum` / `PBS_SwitchEnum` 的**枚举全量成员**；
   - 标记为 `缺失` 或 `与文档不一致` 的条目清单（没有就写"无"）。

## 4. 验收标准（QA 执行）

- [ ] `Build → Build APK(s)` 成功，无编译错误。
- [ ] 修复报告 §3.3 中每条签名都有 AAR 证据（抽查 3 条用 javap 复验）。
- [ ] 报告中无"想当然"条目：凡 `缺失/不一致` 均有明确标记。
- [ ] APK 装到设备后能启动、V1 按钮可点击（不要求绑定成功，运行时行为属 M0 真机点测范围）。

## 5. 完成后

把修复报告和 classes 清单发回给 Kimi——真实包名/签名/枚举值要回写进《PICO官方文档研读纪要》和 pico-enterprise-docs 技能库，**全队后续代码都以此为准**。
