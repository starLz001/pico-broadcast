# M0-FIX-01 修复报告：PicoM0Verify 编译修复（import/签名核对）

> **执行方**：Claude Code　**发卡方**：Kimi（spec-owner）　**验收方**：QA 测试 Agent
> **日期**：2026-10-15　**状态**：✅ 编译通过，APK 已产出
> **铁律遵守情况**：全部结论来自对 `tobservicelib-4.3.24.aar` 的实际检查（`jar tf` 类清单 + `javap` 反编译），**零臆造**。

---

## 1. 变更摘要

| # | 变更 | 性质 |
|---|---|---|
| 1 | `MainActivity.kt` import 块：按 AAR 实锤补全 9 个真实 import（`com.pvr.tobservice.*` + `java.util.function.Consumer`） | 修复编译 |
| 2 | `v1Bind()`：绑定回调从臆测的 `BindCallback{onBindSuccess/onBindFailed}` 改为真实的 `BindCallBack{bindCallBack(Boolean)}` | 修复编译（接口形态与假设不符） |
| 3 | `binder()`：`getServiceBinder()` 实际返回原始 AIDL 接口 `IToBService`，改为经 `ToBServiceProxyImpl(IToBService)` 包装后返回，下游各 V 函数的 `as? IToBServiceProxy` 判定逻辑保持不变 | 修复运行时必现的强转失败 |
| 4 | `v11Copyright()`：`appCopyrightVerify` 第二参从 `IIntCallback.Stub` 改为 AAR 实锤的 `Consumer<Integer>` 形态 | 修复编译（文档歧义实锤） |

**业务验证逻辑（V1–V11 按钮与验证步骤）零改动**，仅修正接口形态。其余文件（`AutoBootService.kt` / `AdminReceiver.kt` / `AndroidManifest.xml` / 各 gradle 文件）检查后**无编译问题，未改动**。

## 2. 文件清单

| 文件 | 动作 |
|---|---|
| `app/src/main/java/com/broadcast/m0verify/MainActivity.kt` | 修改（4 处，见 §1） |
| `docs/m0/tobservicelib-4.3.24-classes.txt` | 新增 — AAR 完整类清单（218 行，`jar tf classes.jar` 输出） |
| `docs/m0/tobservicelib-4.3.24-javap-key-classes.txt` | 新增 — 13 个关键类的完整 javap 输出（供 QA 抽查复验，286 条方法签名） |
| `docs/m0/M0-FIX-01-修复报告.md` | 新增 — 本文件 |
| `app/build/outputs/apk/debug/app-debug.apk` | 构建产物（9,055,450 字节） |

## 3. 考古结果（任务卡 §3.3 要求项）

### 3.1 类全限定名对照表（推断值 → 实际值）

侦察路径：`%USERPROFILE%\.gradle\caches\modules-2\files-2.1\com.picoxr.tobservice\tobservicelib\4.3.24\94f184fb…\tobservicelib-4.3.24.aar` → 解压取 `classes.jar` → `jar tf` 全量类清单。

| 类 | 原推断包名（任务卡 §1） | **AAR 实际全限定名** |
|---|---|---|
| ToBServiceHelper | `com.picoxr.tobservice.ToBServiceHelper` | `com.pvr.tobservice.ToBServiceHelper` |
| ToBServiceHelper.BindCallback | `…BindCallback`（onBindSuccess/onBindFailed） | `com.pvr.tobservice.ToBServiceHelper$BindCallBack`（**C 大写**，单方法 `bindCallBack(java.lang.Boolean)`） |
| IToBServiceProxy | `com.picoxr.tobservice.IToBServiceProxy` | `com.pvr.tobservice.interfaces.IToBServiceProxy` |
| IIntCallback | `com.picoxr.tobservice.IIntCallback` | `com.pvr.tobservice.interfaces.IIntCallback`（AIDL，含 `$Stub`） |
| IBoolCallback | `com.picoxr.tobservice.IBoolCallback` | `com.pvr.tobservice.interfaces.IBoolCallback`（AIDL，含 `$Stub`） |
| PBS_SystemInfoEnum | `com.picoxr.tobservice.PBS_SystemInfoEnum` | `com.pvr.tobservice.enums.PBS_SystemInfoEnum` |
| PBS_DeviceControlEnum | `com.picoxr.tobservice.PBS_DeviceControlEnum` | `com.pvr.tobservice.enums.PBS_DeviceControlEnum` |
| PBS_PackageControlEnum | `com.picoxr.tobservice.PBS_PackageControlEnum` | `com.pvr.tobservice.enums.PBS_PackageControlEnum` |
| PBS_SwitchEnum | `com.picoxr.tobservice.PBS_SwitchEnum` | `com.pvr.tobservice.enums.PBS_SwitchEnum` |

**规律**：真实包名 `com.pvr.tobservice`（非 `com.picoxr`——那只是 Maven 坐标 groupId）；枚举在 `.enums` 子包、AIDL 回调在 `.interfaces` 子包、代理实现在 `.impl` 子包。

### 3.2 调用点签名核对表（任务卡 §2.2 逐条，全部抄自 javap 输出）

证据来源：`docs/m0/tobservicelib-4.3.24-javap-key-classes.txt`（下文片段均可在该文件复核）。

| 调用点 | 原代码假设 | **AAR 真实签名** | 结论 |
|---|---|---|---|
| 绑定 | `bindTobService(ctx, BindCallback{onBindSuccess/onBindFailed})` | `public void bindTobService(android.content.Context, ToBServiceHelper$BindCallBack)`；回调接口仅一个方法：`public abstract void bindCallBack(java.lang.Boolean)` | ❌ **不一致 → 已修** |
| 取 binder | `getServiceBinder()` 返回值可直接强转 `IToBServiceProxy` | `public IToBService getServiceBinder()` —— 返回**原始 AIDL 接口**；须经 `public ToBServiceProxyImpl(IToBService)` 包装得到 `IToBServiceProxy` | ❌ **不一致 → 已修** |
| 系统信息 | `pbsStateGetDeviceInfo(PBS_SystemInfoEnum.PUI_VERSION, 0)` | `IToBService`: `public abstract String pbsStateGetDeviceInfo(PBS_SystemInfoEnum, int) throws RemoteException`；`PUI_VERSION` 成员存在 | ✅ 一致 |
| 拉起应用 | `pbsStartActivity(String,String,String,String,String[],int[],int)` | `IToBService`: `public abstract int pbsStartActivity(String, String, String, String, String[], int[], int) throws RemoteException`（7 参，返回 int） | ✅ 一致 |
| 前台应用 | 强转后 `getFocusedApp()` | `IToBServiceProxy`: `public abstract ComponentName getFocusedApp()` | ✅ 一致 |
| 录屏 | `pbsStartRecordScreenBySurface(Surface,int,int,int)` / `pbsStopRecordScreenBySurface(int)` | `IToBService`: `public abstract void pbsStartRecordScreenBySurface(android.view.Surface, int, int, int) throws RemoteException` / `public abstract void pbsStopRecordScreenBySurface(int) throws RemoteException` | ✅ 一致 |
| 设备控制 | `pbsControlSetDeviceAction(DEVICE_CONTROL_REBOOT, IIntCallback)` | `IToBService`: `public abstract void pbsControlSetDeviceAction(PBS_DeviceControlEnum, IIntCallback) throws RemoteException`；`DEVICE_CONTROL_REBOOT` 存在 | ✅ 一致 |
| 包管理 | `pbsControlAPPManger(PBS_PackageControlEnum,String,int,IIntCallback)` | `IToBService`: `public abstract void pbsControlAPPManger(PBS_PackageControlEnum, String, int, IIntCallback) throws RemoteException`（**拼写就是 APPManger**，AAR 原样） | ✅ 一致 |
| 杀进程 | `pbsKillAppsByPidOrPackageName(int[],String[],int)` | `IToBService`: `public abstract void pbsKillAppsByPidOrPackageName(int[], String[], int) throws RemoteException` | ✅ 一致 |
| 息屏 | `pbsScreenOff()` | `IToBService`: `public abstract void pbsScreenOff() throws RemoteException` | ✅ 一致 |
| 保活 | `pbsAppKeepAlive(String,boolean,int)` | `IToBService`: `public abstract void pbsAppKeepAlive(String, boolean, int) throws RemoteException` | ✅ 一致 |
| DeviceOwner | `setDeviceOwner(ComponentName)` 返回 int | `IToBServiceProxy`: `public abstract int setDeviceOwner(android.content.ComponentName)` | ✅ 一致 |
| Home 替换 | `pbsAppSetAPPAsHomeTwo(S_ON/S_OFF, String)` | `IToBService`: `public abstract void pbsAppSetAPPAsHomeTwo(PBS_SwitchEnum, String) throws RemoteException`；`S_ON`/`S_OFF` 存在 | ✅ 一致 |
| 版权校验 | `appCopyrightVerify(String, IIntCallback.Stub)` | `IToBServiceProxy`: `public abstract void appCopyrightVerify(String, java.util.function.Consumer<Integer>)`（**无** RemoteException 声明） | ❌ **与文档不一致（歧义实锤）→ 已修为 Consumer 形态** |

`IIntCallback` 为 AIDL 接口（`callback(int) throws RemoteException`），V6/V7 的回调写法维持 `object : IIntCallback.Stub() { override fun callback(result: Int) }`，与 AAR 形态吻合。

**关键 javap 证据片段**（完整输出见 javap 证据文件）：

```
public class com.pvr.tobservice.ToBServiceHelper {
  public static com.pvr.tobservice.ToBServiceHelper getInstance();
  public void bindTobService(android.content.Context, com.pvr.tobservice.ToBServiceHelper$BindCallBack);
  public com.pvr.tobservice.interfaces.IToBService getServiceBinder();
}
public interface com.pvr.tobservice.ToBServiceHelper$BindCallBack {
  public abstract void bindCallBack(java.lang.Boolean);
}
public final class com.pvr.tobservice.impl.ToBServiceProxyImpl
    implements com.pvr.tobservice.interfaces.IToBServiceProxy {
  public com.pvr.tobservice.impl.ToBServiceProxyImpl(com.pvr.tobservice.interfaces.IToBService);
}
public interface com.pvr.tobservice.interfaces.IToBServiceProxy
    extends com.pvr.tobservice.interfaces.IToBService {          // ← pbs* 底层方法在父接口上
  public abstract void appCopyrightVerify(java.lang.String, java.util.function.Consumer<java.lang.Integer>);
  public abstract android.content.ComponentName getFocusedApp();
  public abstract int setDeviceOwner(android.content.ComponentName);
}
```

**架构要点**（回写文档库时务必带上）：`IToBServiceProxy extends IToBService`。`pbs*` 系列（启动/录屏/杀进程/息屏/保活/Home/包管理/设备控制）全部声明在 `IToBService`（AIDL 原始接口）；扩展方法（`getFocusedApp`/`setDeviceOwner`/`appCopyrightVerify`/虚拟屏/对话框等）在 `IToBServiceProxy`。标准用法 = `ToBServiceProxyImpl(getServiceBinder())` 拿到 proxy 后全量可用。

### 3.3 枚举全量成员（javap 抄录，V2/V7"考古"交付物）

**PBS_SystemInfoEnum（25 个成员）**：
`ELECTRIC_QUANTITY`、`PUI_VERSION`、`EQUIPMENT_MODEL`、`EQUIPMENT_SN`、`CUSTOMER_SN`、`INTERNAL_STORAGE_SPACE_OF_THE_DEVICE`、`DEVICE_BLUETOOTH_STATUS`、`BLUETOOTH_NAME_CONNECTED`、`BLUETOOTH_MAC_ADDRESS`、`DEVICE_WIFI_STATUS`、`WIFI_NAME_CONNECTED`、`WLAN_MAC_ADDRESS`、`DEVICE_IP`、`CHARGING_STATUS`、`DEVICE_KEY`、`BLUETOOTH_INFO_DEVICE`、`BLUETOOTH_INFO_CONNECTED`、`CAMERA_TEMPERATURE_CELSIUS`、`CAMERA_TEMPERATURE_FAHRENHEIT`、`LARGESPACE_MAP_INFO`、`CURRENT_CHARGING_PROTOCOL`、`USB_PWOER_MAX`（**AAR 原样拼写，PWOER 是官方 typo，勿"纠正"**）、`HMD_POSITION_AND_ORIENTATION`、`PSENSOR_STATUS`、`LARGE_SPACE_MAP_SCALE`

**PBS_DeviceControlEnum（2 个成员）**：`DEVICE_CONTROL_REBOOT`（重启）、`DEVICE_CONTROL_SHUTDOWN`（关机）

**PBS_PackageControlEnum（2 个成员）**：`PACKAGE_SILENCE_INSTALL`（静默安装）、`PACKAGE_SILENCE_UNINSTALL`（静默卸载）
→ V7 代码按名称匹配 `INSTALL`（排除 `UNINSTALL`）将命中 `PACKAGE_SILENCE_INSTALL`，逻辑成立。

**PBS_SwitchEnum（2 个成员）**：`S_ON`、`S_OFF`

### 3.4 缺失 / 与文档不一致清单

**缺失：无。** 任务卡 §2.2 的 14 个调用点全部在 AAR 4.3.24 中存在，所用枚举成员全部存在。

**与文档/原假设不一致（3 条，均已修复）**：

| # | 条目 | 差异 |
|---|---|---|
| 1 | 绑定回调 | 不是 `BindCallback{onBindSuccess()/onBindFailed()}`，而是 `BindCallBack{bindCallBack(Boolean)}`（单方法，true=成功；类名 C 大写） |
| 2 | getServiceBinder() | 返回 `IToBService`（原始 AIDL），**不能**直接当 `IToBServiceProxy` 用，必须 `ToBServiceProxyImpl(...)` 包装 |
| 3 | appCopyrightVerify | 第二参是 `Consumer<Integer>`，**不是** `IIntCallback`（任务卡记载的文档歧义就此实锤，建议回写纪要并同步官方对接人） |

## 4. 测试报告（自测）

- 构建命令：`gradle -p PicoM0Verify :app:assembleDebug --console=plain`
- 工具链：Gradle 8.5（本地 wrapper 缓存发行版）+ AGP 8.1.4 + Kotlin 1.9.20 + JBR JDK 21 —— **未升级任何版本**（遵守禁止事项）
- 结果：**BUILD SUCCESSFUL in 27s**，35 个任务全部执行，`compileDebugKotlin` 零错误零警告
- 产物：`app/build/outputs/apk/debug/app-debug.apk`（9,055,450 字节，2026-10-15 17:20）
- 构建期唯一警告：`Unable to strip … libtobservice.so`（AAR 自带 .so 无 strip 符号，原样打包，**无害**，与任务卡"已知无害报错"同类）
- 验收第 4 条（装机启动、V1 可点击）：**未执行** —— 本环境无真机，属 QA/M0 真机点测范围

## 5. 与规格偏差

无。未改业务验证逻辑，未改 specs，未动 gradle 版本。`binder()` 内部实现改为包装 `ToBServiceProxyImpl`，但其对下游契约（返回可 `as? IToBServiceProxy` 成功的对象或 null）保持不变。

## 6. 待确认问题（发回 Kimi）

1. **回写文档库**：§3.1 包名规律、§3.2 架构要点（`IToBServiceProxy extends IToBService` + `ToBServiceProxyImpl` 包装用法）、§3.3 枚举全量、§3.4 三条不一致，请回写《PICO官方文档研读纪要》与 pico-enterprise-docs 技能库。
2. **官方确认项**：`appCopyrightVerify` 的 `Consumer<Integer>` 形态与文档 `IIntCallback` 记载冲突（AAR 4.3.24 实锤为 Consumer）——建议随四件待确认事项一并问官方对接人：设备端系统服务版本是否与 AAR 4.3.24 对齐（V11 真机若抛 `NoSuchMethodError` 即此因，demo 已留日志钩子）。
3. **拼写红线**：`pbsControlAPPManger`、`USB_PWOER_MAX` 是 AAR 原样拼写，全队后续代码以此为准，勿按"正确拼写"改写（会编译失败）。
4. **QA 抽查建议**：验收第 2 条抽查 javap 复验时，可直接比对 `docs/m0/tobservicelib-4.3.24-javap-key-classes.txt`，或自行解压 Gradle 缓存中同一 AAR 重跑 javap。
