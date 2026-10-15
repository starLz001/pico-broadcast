# Agent-Android 连接与数据读取规范

## 概述

`agent-android/` 是 PICO VR 端 Agent，使用 Kotlin 开发，运行在 Pico 头显上。本文档定义其与本地服务、云端、控制端的连接方式和数据读取规范。

## 网络拓扑

```
Pico VR 头显 (agent-android)
    │
    ├── WebSocket ──► local-service (局域网，低延迟控制)
    ├── HTTPS ──► cloud (公网，配置下发/日志上报)
    └── UDP Multicast ──► controller (设备发现)
```

## 1. 与 Local-Service 连接

### 协议
- **传输层**：WebSocket (ws://)
- **默认端口**：`9800`（可配置）
- **发现方式**：mDNS / SSDP / 手动配置 IP

### 消息格式
```json
{
  "type": "command|event|heartbeat",
  "seq": 1,
  "ts": 1697400000000,
  "payload": { ... }
}
```

### 连接生命周期
1. 启动时扫描局域网 local-service（mDNS `_pico-broadcast._tcp`）
2. 建立 WebSocket 连接，发送 `hello` 握手
3. 每 5s 发送 heartbeat，30s 无响应则重连
4. 断线自动重连，指数退避（1s → 2s → 4s → max 30s）

## 2. 与 Cloud 连接

### 协议
- **传输层**：HTTPS (TLS 1.2+)
- **端点**：`https://{cloud-host}/api/v1/`
- **认证**：设备证书 + JWT Token

### 主要接口
| 接口 | 方法 | 用途 |
|------|------|------|
| `/device/register` | POST | 设备首次注册 |
| `/config/pull` | GET | 拉取播控配置 |
| `/log/upload` | POST | 上报运行日志 |
| `/heartbeat` | POST | 设备心跳（60s 间隔）|

### 数据读取规则
- 配置拉取：启动时 + 每 5 分钟轮询 + WebSocket 推送变更
- 日志上报：本地缓存，WiFi 下批量上传，失败重试 3 次
- 所有请求带 `X-Device-Id` 和 `Authorization: Bearer {jwt}`

## 3. 与 Controller 连接

### 设备发现
- UDP Multicast `239.0.0.1:9801`，广播自身存在
- 控制端扫描后通过 local-service 中转指令

### 数据读取
- 播控指令由 local-service 转发，不直连 controller
- 状态上报走 local-service → cloud 链路

## 4. 本地数据存储

### 路径
```
/data/data/com.wanmu.picoagent/
├── config/          # 本地配置缓存
├── cache/           # 内容缓存
├── logs/            # 本地日志（滚动 7 天）
└── db/              # SQLite 元数据
```

### 读取优先级
1. 内存缓存（最新）
2. 本地 SQLite（离线可用）
3. local-service（实时）
4. cloud（兜底）

## 5. 安全要求

- 所有 WebSocket 连接需 TLS（生产环境 wss://）
- JWT Token 有效期 24h，自动刷新
- 敏感配置（密钥等）存 Android Keystore
- 禁止明文存储凭证

## 6. 错误处理

| 场景 | 策略 |
|------|------|
| local-service 不可达 | 使用本地缓存配置，每 10s 重试 |
| cloud 不可达 | 降级离线模式，日志本地堆积 |
| 配置解析失败 | 回滚到上一版本配置，上报错误 |
| 心跳超时 | 断开重连，记录断线事件 |
