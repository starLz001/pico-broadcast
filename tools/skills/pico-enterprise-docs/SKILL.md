---
name: pico-enterprise-docs
description: PICO 企业版官方文档离线知识库（83 篇目录梳理 + 32 篇核心文档原文存档 + 研读纪要）。当开发 PICO 播控系统需要确认 TobService 企业级 API 接口名/签名/用法、企业应用套件（Business Suite）SDK-PC/PAD/HMD/Unity 的指令与回调、企业串流 SDK、MDM 平台能力、Device Owner/高风险接口/Launcher/开机自启等系统能力、设备兼容性与环境要求时使用。适用于 Spec Owner 与实现方两侧——写 Agent、控制端、本地服务代码前，先查本库核实接口，禁止凭记忆臆造 PICO 接口名。
---

# PICO 企业版文档知识库

## 使用顺序

0. **要写 TobService 调用代码时，先读 `references/AAR-4.3.24-接口实锤.md`**——AAR 实物考古的包名/签名/枚举全量/官方答复，优先级高于文档记载，冲突时以它为准。
1. **再读 `references/研读纪要.md`**——83 篇文档地图、核心发现、约束清单、方案↔文档依据对照表，多数问题在这里就能定位到答案和出处。
2. **需要接口签名/参数细节时检索原文存档** `references/raw/*.txt`（32 篇官方正文提取）：

```bash
# 按接口名全库检索
grep -rn "pbsStartActivity" references/raw/
# 按主题直接开对应文件
```

## 原文存档索引（references/raw/）

| 主题 | 文件 |
|---|---|
| TobService 入门/集成/适用设备/高风险声明 | `EnterpriseAPI-Intro.txt` |
| 设备控制（开关机/安装/杀进程/熄屏/定时） | `EnterpriseAPI-DeviceControl.txt` |
| 设备信息（SN/电量/PUI/CPU/温度/姿态） | `EnterpriseAPI-DeviceInfo.txt` |
| 应用类（带参启动/保活/前台监听/版权校验） | `EnterpriseAPI-App.txt` |
| 系统配置 | `EnterpriseAPI-SystemConfig.txt` |
| 录屏录音（画面直传 Surface） | `EnterpriseAPI-Record.txt` |
| 投屏 | `EnterpriseAPI-Cast.txt` |
| 大空间 LBE | `EnterpriseAPI-LBE.txt` |
| 数据结构/回调/错误码 | `EnterpriseAPI-DataClass.txt` `EnterpriseAPI-Callback.txt` `EnterpriseAPI-ErrorCode.txt` |
| 套件 PC/Pad 播控方案 | `PC-Instruction-BroadCast.txt` `Pad-BroadCast.txt` |
| 套件硬件网络要求/注意事项 | `Environment-Requirements-Broadcast.txt` `Environment-Requirements-Note.txt` |
| 套件应用场景/设备管理/VR Launcher | `BusinessSuite-appscenes.txt` `BusinessSuite-Instructions.txt` `BusinessSuite-VRlauncher.txt` |
| 套件 SDK | `BusinessSuite-SDK-PC.txt` `BusinessSuite-SDK-PAD.txt` `BusinessSuite-SDK-HMD.txt` `BusinessSuite-SDK-UnitySample.txt` |
| 企业串流 | `BusinessStreamingv22-SDK-Instruction.txt`（当前版本）`BusinessStreamingv2-SDK-Instruction.txt` |
| MDM 官方云平台 | `MDM-quickstart.txt` `MDM-device.txt` `MDM-content.txt` `MDM-videoencryption.txt` |
| 交付运维（Launcher/推荐位/DP直连/企业PUI） | `Set-As-Launcher-App.txt` `Customize-Featured-First.txt` `Streaming-DP.txt` `Enterprise-PUI.txt` |

## 硬性约束（写代码前必须确认）

1. 高风险接口需 manifest 声明 `pico_advance_interface`，声明后不能上架 PICO 商店——本项目走企业分发。
2. TobService 全系接口仅企业版设备可用（Neo3 系列企业版 / PICO4E / PICO4UE / G3）。
3. Device Owner 需 PUI 5.13.0+。
4. 画面监控官方上限 25 路；Pad 播控官方仅支持安卓 11/12；在线部署单包 ≤10G、5 台并行。
5. **禁止臆造接口名**——凡 `pbs*` 开头的调用必须能在 raw/ 中 grep 到原文；grep 不到就走 STOP 流程提确认。

## 文档未覆盖项

未发布新设备的 TobService 支持度、高风险接口授权流程、串流 SDK 获取方式等，官方文档没有答案，统一走商务渠道（pico-business-techsupport@bytedance.com），不要在代码里假设。
