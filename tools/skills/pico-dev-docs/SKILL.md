---
name: pico-dev-docs
description: PICO 开发者门户 SDK 文档离线知识库（developer-cn.picoxr.com，7 个文档库 613 页原文存档 + 研读纪要）。当开发 PICO 内容端/客户端涉及 Unity Integration SDK、Unity/UE OpenXR、PICO Unreal SDK、PICO Spatial SDK 的使用——SDK 选型、Unity/UE 版本配套、渲染特性（FFR/合成层/超分）、交互（手柄/手势/眼动）、打包配置、Manifest、包名规范、空间应用开发时使用。适用于 Spec Owner 与实现方两侧——写 Unity/UE/安卓空间应用代码前先查本库，禁止凭记忆臆造 PXR_* SDK API。
---

# PICO 开发者 SDK 文档知识库

## 定位

与 `pico-enterprise-docs`（企业站：TobService/套件/串流/MDM，管"管控"）互补，本库管"**内容开发**"：游戏和 XR 应用怎么写、怎么打包。

## 使用顺序

1. **先读 `references/研读纪要.md`**——SDK 版图与选型、版本配套红线、与本项目的接口点，多数问题在此定位。
2. **需要 API/步骤细节时检索原文存档** `references/raw/`（613 页）：

```bash
grep -rn "PXR_Manager" references/raw/unity/
grep -rn "注视点" references/raw/unreal/
```

## 存档索引

| 子库 | 页数 | 内容 |
|---|---|---|
| `raw/unity/` | 197 | Unity Integration SDK 全量（渲染/交互/平台服务/打包） |
| `raw/unity-quickstart/` | 34 | Unity 快速入门（含渲染模式选择） |
| `raw/unity-openxr/` | 26 | Unity OpenXR 指南 |
| `raw/unreal/` | 144 | Unreal Integration SDK 全量（含 UE5 特性、PICO UE 分支） |
| `raw/unreal-quickstart/` | 22 | Unreal 快速入门 |
| `raw/unreal-openxr/` | 51 | Unreal OpenXR 指南 |
| `raw/spatial-sdk/` | 139 | Spatial SDK 全量（PICO OS 6 空间应用/Spatial UI/容器/环境感知） |

## 硬性事实（写代码前确认）

1. Unity 编辑器 2021.3.26–6.3；Unity 2022 需 SDK≥2.1.5+OS 5.5.0；2023 需 SDK≥3.0.0+OS 5.11.0；Unity 6 需 SDK≥3.1.0+OS 5.12.0。PICO 4 需 OS 5.13.0，4 Ultra 需 OS 5.14.0+。
2. 已知坑：Unity 2022 + URP + Linear + 4xAA + OpenGL 崩溃；Unity 2022 + Vulkan + Development Build 崩溃。
3. UE 开发机：Win10、GTX 1060+、8G+、USB 3.0。
4. Spatial SDK 基于 Android 体系、跑 PICO OS 6——与 XR 游戏 SDK 是不同形态，别混用概念。
5. **禁止臆造 SDK API**——凡 `PXR_*` / `UPICO*` 调用必须能在 raw/ 中 grep 到；grep 不到走 STOP 流程。

## 本项目接口点速记

- 内容端一体机游戏：Integration SDK 或 OpenXR（随内容方现状）；播控 SDK 与其并存不冲突。
- 串流 PC 版内容：不用 PICO SDK（画面走企业串流）。
- 控制端 Unity：普通 2D 工程，不用 XR SDK。
- Agent：原生 Kotlin + TobService，不在本库范围（查 pico-enterprise-docs）。
