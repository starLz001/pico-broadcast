# 万木 PICO 播控系统 (PICO Broadcast System)

VR 播控 Monorepo，包含 VR 端 Agent、控制端、本地服务、云端、内容 SDK 等模块。

## 目录结构

```
pico-broadcast/
├── docs/
│   ├── specs/           # 任务卡（签发，AI 只读）
│   ├── conventions/     # 代码规范（唯一权威）
│   └── decisions/       # 裁决记录（STOP 问题答复）
├── agent-android/       # VR 端 Agent（Kotlin）
├── controller/          # 控制端（Unity，PC/Pad 双端）
├── local-service/       # 本地服务（.NET 8）
├── cloud/               # 轻云端（.NET 8）
├── content-sdk/         # 内容 SDK（Unity 插件 + UE 桥接）
├── streamer-companion/  # 串流伴生程序（二期）
├── tests/
│   └── protocol-sim/    # 协议仿真器（Python）
└── tools/
    └── PicoM0Verify/    # 验证 demo
```

## 开发规范

参见 `docs/conventions/` 目录。

## AI 编码助手接入

- Kimi：参见 `docs/ai-coding/kimi-guide.md`
- Claude Code：参见 `docs/ai-coding/claude-code-guide.md`
