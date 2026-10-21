# PicoM0Verify —— M0 技术验证 Demo

对应《M0-技术验证清单.md》的 **V1–V11**（V12 串流、V13 官方套件在 APK 外做）。
一个按钮 = 一项验证，日志同步显示在屏幕下半区并写入文件，用于回填验证矩阵。

## 一、打开与编译

1. Android Studio → Open → 选择 `PicoM0Verify/` 目录（不是 app/ 子目录）。
2. 首次同步会自动从 PICO Maven 仓库拉 `tobservicelib:4.3.24`，需能访问：
   `https://artifacts-cn-beijing.volces.com/repository/picoxrtob`
3. **若 Kotlin 报 `Unresolved reference`（import 标红）**：
   官方文档没给 import 路径，MainActivity.kt 里的包名是推断的。修法：
   删掉标红的 import → 光标放到代码里报红的类名上 → **Alt+Enter → Import**，让 IDE 从 AAR 里找真实包名。
   `ToBServiceHelper` / `IToBServiceProxy` / `IIntCallback` / `PBS_*Enum` 共 7 个类。
4. 编译：`Build → Build APK(s)`，产物在 `app/build/outputs/apk/debug/app-debug.apk`。

## 二、安装到设备

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
# 再推一个测试 APK 供 V7 静默安装验证：
adb push 某个测试.apk /sdcard/test.apk
```

设备里打开「M0验证」。**两台设备分别跑一遍**：PICO 4 Ultra 企业版 + 新设备。

## 三、点测顺序（V1→V11）

| 按钮 | 看什么 | 矩阵回填 |
|---|---|---|
| V1 绑定 | 日志出现 `✓ V1 绑定成功` | TobService 可用性 |
| V2 系统信息 | PUI 版本号 + 枚举全量列表 | PUI 版本列 |
| V3 拉起App | 先把目标包名改成设备里真实安装的游戏包名；游戏是否被拉起、能否收到 extra | 启动能力 + 启动参数透传 |
| V4 前台轮询 | 手动切几个 App，看 15 次轮询是否跟上 | 计费"目击通道"可行性 |
| V5 录屏 | 预览窗口出现设备实时画面 | 投屏回传可行性 + 帧率观感 |
| V6 杀进程/息屏/重启 | 目标 App 退出 / 屏幕熄灭 / 设备重启 | 三项控制能力 |
| V7 静默安装 | 先 `/sdcard/test.apk` 就位；日志列出的枚举全量 + 安装回调=0 | 安装能力 + 枚举值实锤（重要！） |
| V8 保活+自启 | 设保活后**重启设备**，开机后看日志有无 `[V8] AutoBootService onCreate` | 保活 + 自启两项 |
| V9 DeviceOwner | 返回 0 | Owner 能力（PUI≥5.13.0） |
| V10 Home 替换 | S_ON 后按 Home 进目标 App；S_OFF 恢复 | Launcher 替换能力 |
| V11 版权校验 | 回调 result；若报 NoSuchMethodError 属文档歧义，记下来问官方 | 内容保护接口形态 |

⚠ V6 重启按钮会弹确认框，别误触。

## 四、导出日志 & 回填矩阵

```bash
adb pull /sdcard/Android/data/com.broadcast.m0verify/files/m0.log m0-P4U.log
adb pull /sdcard/Android/data/com.broadcast.m0verify/files/m0.log m0-新设备.log
```

把两份日志 + 两台的结论填进《M0-技术验证清单.md》§4 可用性矩阵，发我。
⛔ 阻塞项（V1/V3/V4/V7/V8）任何一台不过，直接停，先找官方对接人。

## 五、已知边界

- 本 demo 只做接口探活，不含计费/额度任何逻辑（M1 才开始）。
- V7/V2 的枚举全量日志是关键交付物——官方文档没列枚举值，这两步等于替团队"考古"。
- 新设备若某接口行为不同，不必慌，如实回填，差异本身就是 M0 要的东西。
