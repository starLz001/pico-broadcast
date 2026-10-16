# 万木 PICO 播控系统 — 全模块开发接入指南

> 本文档面向所有参与开发的工程师与 AI 编码助手（Kimi / Claude Code 等），说明各子模块的技术栈、开发环境搭建、协作流程。

---

## 一、仓库总览

```
pico-broadcast/
├── docs/                    # 文档（任务卡、规范、裁决）
├── agent-android/           # VR 端 Agent（Kotlin）
├── controller/              # 控制端（Unity C#）
├── local-service/           # 本地服务（.NET 8）
├── cloud/                   # 轻云端（.NET 8）
├── content-sdk/             # 内容 SDK（Unity + UE）
├── streamer-companion/      # 串流伴生（二期）
├── tests/protocol-sim/      # 协议仿真（Python）
└── tools/PicoM0Verify/      # 验证 demo
```

---

## 二、通用规则

1. **任务卡驱动**：所有开发任务由 `docs/specs/` 中的任务卡定义，AI 只读不修改
2. **代码规范唯一权威**：`docs/conventions/` 目录下的规范文件
3. **裁决记录**：`docs/decisions/` 记录 STOP 问题的答复，必须遵守
4. **分支策略**：每个任务卡一个 feature 分支，命名 `feature/{task-id}-{short-desc}`
5. **提交规范**：Conventional Commits（`feat:` / `fix:` / `docs:` / `chore:` / `test:` / `refactor:`）
6. **安全红线**：绝不提交密钥/凭证，AI 生成代码必须人工 review

---

## 三、各模块接入指南

### 3.1 agent-android（VR 端 Agent）

| 项目 | 说明 |
|------|------|
| 语言 | Kotlin |
| 平台 | Pico VR 头显（Android） |
| 构建工具 | Gradle |
| 最低 API | Android 29+ |
| 关键依赖 | Pico SDK、WebSocket、mDNS |

**开发环境**：
- Android Studio Hedgehog+
- Pico SDK（从 Pico Developer Portal 下载）
- 真机调试（Pico 头显）

**连接规范**：详见 `docs/conventions/agent-android-connection.md`

**AI 助手接入**：
```
上下文投喂顺序：
1. docs/conventions/agent-android-connection.md
2. docs/specs/{task-card}.md
3. agent-android/ 下相关源码
```

---

### 3.2 controller（控制端）

| 项目 | 说明 |
|------|------|
| 引擎 | Unity 2022 LTS+ |
| 语言 | C# |
| 目标平台 | PC (Windows) + Pad (Android/iPad) |
| UI 框架 | Unity UI Toolkit 或 UGUI |

**开发环境**：
- Unity Hub + Unity 2022 LTS
- Visual Studio 2022 / Rider
- 目标平台：Windows Standalone + Android

**通信方式**：
- 通过 local-service 中转指令
- UDP Multicast 设备发现

**AI 助手接入**：
```
Unity 项目结构约定：
Assets/
├── Scripts/          # 业务逻辑
├── Prefabs/          # 预制体
├── Scenes/           # 场景文件
└── Plugins/          # 第三方插件

AI 只修改 Scripts/ 下的 .cs 文件
```

---

### 3.3 local-service（本地服务）

| 项目 | 说明 |
|------|------|
| 框架 | .NET 8 (ASP.NET Core) |
| 语言 | C# |
| 通信 | WebSocket + gRPC |
| 部署 | 局域网内 Windows/Linux 主机 |

**开发环境**：
- .NET 8 SDK
- IDE：Rider / VS Code / Visual Studio 2022
- Docker（可选，用于容器化部署）

**项目结构**：
```
local-service/
├── src/
│   ├── LocalService.Core/         # 核心业务逻辑
│   ├── LocalService.Api/          # HTTP/WebSocket 端点
│   └── LocalService.Grpc/         # gRPC 服务定义
├── tests/
└── LocalService.sln
```

**AI 助手接入**：
```
关键接口：
- WebSocket 端口 9800（VR 端连接）
- gRPC 端口 50051（控制端调用）
- mDNS 服务发现：_pico-broadcast._tcp

代码规范：
- 所有公开方法必须有 XML 注释
- 异常处理使用全局中间件
- 日志使用 Serilog
```

---

### 3.4 cloud（轻云端）

| 项目 | 说明 |
|------|------|
| 框架 | .NET 8 (ASP.NET Core) |
| 语言 | C# |
| 数据库 | PostgreSQL / MySQL |
| 部署 | 阿里云 ECS / Docker |

**开发环境**：
- .NET 8 SDK
- PostgreSQL 15+ 或 MySQL 8+
- Docker + Docker Compose

**项目结构**：
```
cloud/
├── src/
│   ├── Cloud.Core/                # 核心业务
│   ├── Cloud.Api/                 # REST API
│   ├── Cloud.Infrastructure/      # 数据库/缓存
│   └── Cloud.Worker/              # 后台任务
├── migrations/                    # EF Core 迁移
├── docker-compose.yml
└── Cloud.sln
```

**AI 助手接入**：
```
API 设计规范：
- RESTful，资源命名用复数
- 版本控制：/api/v1/...
- 认证：JWT Bearer Token
- 分页：?page=1&pageSize=20

数据库规范：
- 表名小写 + 下划线（device_info）
- 字段小写 + 下划线（created_at）
- 主键统一用 bigint 自增
- 软删除：is_deleted 字段
```

---

### 3.5 content-sdk（内容 SDK）

| 项目 | 说明 |
|------|------|
| Unity 部分 | C#，Unity Package |
| UE 部分 | C++ / Blueprint 桥接 |
| 功能 | 内容加载、播控指令解析、状态同步 |

**开发环境**：
- Unity 2022 LTS（Unity 侧）
- Unreal Engine 5.3+（UE 侧）
- 双端测试工程

**项目结构**：
```
content-sdk/
├── unity/
│   ├── Runtime/                   # Unity Package 运行时
│   ├── Editor/                    # Unity 编辑器扩展
│   └── package.json
├── ue/
│   ├── Source/                    # C++ 源码
│   ├── Content/                   # UE 资源
│   └── PicoBroadcastSDK.uplugin
└── shared/                        # 跨引擎共享协议定义
    └── protocol.proto
```

**AI 助手接入**：
```
Unity 侧：遵循 Unity Package 规范
UE 侧：遵循 UE 插件规范
共享协议：Protocol Buffers 定义

修改 protocol.proto 后需重新生成两端代码：
- Unity: protoc --csharp_out
- UE: protoc --cpp_out
```

---

### 3.6 streamer-companion（串流伴生）

> ⚠️ 二期再建，当前仅保留目录占位

| 项目 | 说明 |
|------|------|
| 预期技术栈 | 待定（可能 Electron / Tauri） |
| 功能 | 串流场景下的辅助控制 |

---

### 3.7 tests/protocol-sim（协议仿真器）

| 项目 | 说明 |
|------|------|
| 语言 | Python 3.11+ |
| 用途 | 模拟 VR 端/控制端行为，测试协议 |
| 依赖 | websockets, protobuf, asyncio |

**开发环境**：
```bash
cd tests/protocol-sim
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

**项目结构**：
```
tests/protocol-sim/
├── simulators/
│   ├── agent_sim.py               # 模拟 VR 端
│   └── controller_sim.py          # 模拟控制端
├── scenarios/                     # 测试场景
├── requirements.txt
└── README.md
```

**AI 助手接入**：
```
仿真器开发规范：
- 使用 asyncio 异步框架
- 每个模拟器独立进程
- 日志输出到 stdout，方便调试
- 场景用 YAML 定义
```

---

### 3.8 tools/PicoM0Verify（验证 demo）

| 项目 | 说明 |
|------|------|
| 用途 | M0 里程碑功能验证 |
| 类型 | 独立 demo 工具 |

---

## 四、AI 编码助手通用工作流

### 4.1 Kimi（Web/API）

```
1. 打开 https://kimi.moonshot.cn
2. 上传上下文（按顺序）：
   - docs/conventions/ 下相关规范
   - docs/specs/{task-card}.md
   - 相关源码文件
3. 在 prompt 中说明任务
4. 复制生成结果，本地 review 后提交
```

### 4.2 Claude Code（CLI）

```bash
# 1. 进入仓库目录
cd pico-broadcast/

# 2. 启动 Claude Code
claude

# 3. 提示词示例
请阅读 docs/specs/TASK-001.md，按照 docs/conventions/ 的规范，
在 local-service/ 中实现 xxx 功能。完成后运行 dotnet build 确认通过。
```

### 4.3 CLAUDE.md 模板

在仓库根目录创建 `CLAUDE.md`：

```markdown
# CLAUDE.md - Claude Code 项目约束

## 身份
你是 pico-broadcast 项目的开发助手。

## 规则
1. docs/specs/ 只读，绝不修改
2. 代码规范以 docs/conventions/ 为准
3. docs/decisions/ 必须遵守
4. 修改代码前先理解现有架构
5. 提交前确保构建通过
6. 不提交密钥、凭证

## 模块速查
- agent-android/: Kotlin, Pico VR SDK
- controller/: Unity C#
- local-service/: .NET 8, WebSocket + gRPC
- cloud/: .NET 8, REST API
- content-sdk/: Unity Plugin + UE Bridge
- tests/protocol-sim/: Python asyncio

## 构建命令
- local-service: dotnet build
- cloud: dotnet build
- agent-android: ./gradlew build
- protocol-sim: python -m pytest

## 提交规范
Conventional Commits: feat: / fix: / docs: / chore: / test: / refactor:
```

---

## 五、安全红线

- **绝不** 将 API Key / Token / 密码提交到仓库
- **绝不** 让 AI 直接操作生产环境
- **绝不** 让 AI 修改 `docs/specs/`（任务卡只读）
- 所有 AI 生成代码必须人工 review 后才能合入 main
- 敏感配置使用环境变量或 Secret 管理工具

---

## 六、联系与反馈

- 任务卡签发：项目负责人
- 规范疑问：查阅 `docs/decisions/` 或发起讨论
- AI 接入问题：参考本文档或联系运维

---

*文档版本：v1.0 | 最后更新：2026-10-16*
