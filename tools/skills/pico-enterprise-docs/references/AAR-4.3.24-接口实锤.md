# TobService AAR 4.3.24 接口实锤速查（优先级高于文档记载）

> 来源：M0-FIX-01 对 tobservicelib-4.3.24.aar 实物考古（javap）+ 独立复核；官方技术支持答复。
> **写任何 TobService 调用代码前，以本文件为准；文档与本文件冲突时按本文件。**

## 包结构（文档未记载，勿用 com.picoxr.*）

| 类 | 全限定名 |
|---|---|
| 绑定入口 | `com.pvr.tobservice.ToBServiceHelper` |
| 绑定回调 | `ToBServiceHelper.BindCallBack`，单方法 `bindCallBack(Boolean)`（true=成功；C 大写，不是 onBindSuccess/onBindFailed） |
| 原始 AIDL 接口 | `com.pvr.tobservice.interfaces.IToBService`（`getServiceBinder()` 返回它） |
| 扩展代理接口 | `com.pvr.tobservice.interfaces.IToBServiceProxy`（extends IToBService） |
| 代理实现 | `com.pvr.tobservice.impl.ToBServiceProxyImpl(IToBService)` |
| 回调 | `com.pvr.tobservice.interfaces.IIntCallback` / `IBoolCallback`（AIDL，回调用 `object : IIntCallback.Stub()`） |
| 枚举 | `com.pvr.tobservice.enums.PBS_*Enum` |

## 标准用法（唯一正确姿势）

```kotlin
ToBServiceHelper.getInstance().bindTobService(context, object : ToBServiceHelper.BindCallBack {
    override fun bindCallBack(result: Boolean?) { /* true=成功 */ }
})
val proxy = ToBServiceProxyImpl(ToBServiceHelper.getInstance().serviceBinder) // IToBServiceProxy，全量方法可用
```

`getServiceBinder()` 返回原始 `IToBService`，**禁止**直接强转 `IToBServiceProxy`——必须经 `ToBServiceProxyImpl` 包装。

## 与文档不一致点（实锤）

1. 绑定回调形态：见上（BindCallBack/Boolean）。
2. `appCopyrightVerify(String, java.util.function.Consumer<Integer>)`——不是 IIntCallback。结果码：1=接口调用失败；100=不在校验名单；102=有版权；-102=无版权；-104/-105=签名对不上。
3. `setDeviceOwner(ComponentName)` 返回：0=成功，1=失败，**2=权限校验失败**——该接口是功能授权项「允许设置 Device Owner」，未授权返回 2（官方答复 2026-10-15）。替代通道：`adb shell dpm set-device-owner <pkg>/<ComponentName>`（无需授权）。

## 枚举全量（javap 抄录）

- `PBS_DeviceControlEnum`：`DEVICE_CONTROL_REBOOT`、`DEVICE_CONTROL_SHUTDOWN`
- `PBS_PackageControlEnum`：`PACKAGE_SILENCE_INSTALL`、`PACKAGE_SILENCE_UNINSTALL`
- `PBS_SwitchEnum`：`S_ON`、`S_OFF`
- `PBS_SystemInfoEnum`（25 个）：ELECTRIC_QUANTITY、PUI_VERSION、EQUIPMENT_MODEL、EQUIPMENT_SN、CUSTOMER_SN、INTERNAL_STORAGE_SPACE_OF_THE_DEVICE、DEVICE_BLUETOOTH_STATUS、BLUETOOTH_NAME_CONNECTED、BLUETOOTH_MAC_ADDRESS、DEVICE_WIFI_STATUS、WIFI_NAME_CONNECTED、WLAN_MAC_ADDRESS、DEVICE_IP、CHARGING_STATUS、DEVICE_KEY、BLUETOOTH_INFO_DEVICE、BLUETOOTH_INFO_CONNECTED、CAMERA_TEMPERATURE_CELSIUS、CAMERA_TEMPERATURE_FAHRENHEIT、LARGESPACE_MAP_INFO、CURRENT_CHARGING_PROTOCOL、USB_PWOER_MAX、HMD_POSITION_AND_ORIENTATION、PSENSOR_STATUS、LARGE_SPACE_MAP_SCALE

## 拼写红线（AAR 原样，"纠正"即编译失败）

`pbsControlAPPManger`（Manger）、`USB_PWOER_MAX`（PWOER）。

## 录屏接口（官方答复）

推荐 **SurfaceView**（`surfaceView.holder.surface`）；TextureView 需等 SurfaceTexture 就绪并设置 buffer 尺寸，否则"调用受理但无画面"。

## 真机验证基线（PICO 4 Ultra 企业版，PUI 5.15.7，2026-10-15）

绑定✅ / 带参拉起(返回0)✅ / getFocusedApp 前台目击✅ / 录屏受理✅(TextureView无画面) / 杀进程·息屏✅ / 静默安装(result=0)✅ / 保活+官方通道自启✅ / Home替换✅ / setDeviceOwner 返回2(未授权)⚠️
