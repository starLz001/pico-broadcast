# 万木 PICO 播控系统 — 开发接入指南

> 面向所有参与开发的工程师与 AI 编码助手（Kimi / Claude Code），说明角色分工、各模块技术栈与开发环境、协作流程。
> **v3 合并版**：取代《AI 编码助手接入指南 v2》与《全模块开发接入指南 v1.0》。
> v3 修订：与《代码规范-全端 v1》及决策 D8a/D10 对齐（提交格式、分支命名、任务卡签发者、Kimi 角色）；v1.0 中未经裁决的技术点移入 §8「未裁决清单」，裁决前不作为实现依据。

---

## 一、角色与分工

| 角色 | 职责 | 仓库权限 |
|---|---|---|
| **Spec Owner（Kimi）** | 任务卡、协议仿真器、评审、验收、裁决、架构与文档归档 | 只读代码；只写 `docs/specs/`、`tests/protocol-sim/`、`docs/decisions/`（经人类放入仓库） |
| **实现方（Claude Code）** | 全部业务代码实现，任务卡驱动 | 读写代码；`docs/specs/`、`tests/protocol-sim/` 只读 |
| **测试 Agent** | 独立验证，**只测不修** | 只读 |
| **工程师 / 项目负责人（人类）** | 决策、真机操作、代码合并把关、仓库持有 | 全部 |

---

## 二、仓库总览

```
pico-broadcast/
├── CLAUDE.md                # Claude Code 行为约束（红线+铁律，自动加载）
├── docs/
│   ├── specs/               # 任务卡（Kimi 撰写，实现方只读）
│   ├── conventions/         # 代码规范（唯一权威）
│   └── decisions/           # 规格裁决记录
├── agent-android/           # VR 端 Agent（Kotlin）
├── controller/              # 控制端 PC/Pad（Unity C#）
├── local-service/           # 本地服务（.NET 8）
├── cloud/                   # 轻云端（.NET 8）
├── content-sdk/             # 内容 SDK（Unity + UE）
├── streamer-companion/      # 串流伴生（二期）
├── tests/protocol-sim/      # 协议仿真器（Kimi 维护，只读）
└── tools/PicoM0Verify/      # M0 验证 demo
```

---

## 三、通用规则

1. **任务卡驱动**：所有开发任务由 `docs/specs/` 中的任务卡定义，**由 Spec Owner（Kimi）签发**；实现方只读，不修改。
2. **代码规范唯一权威**：`docs/conventions/`（《代码规范-全端 v1》+《Unity C# 代码规范》）。
3. **裁决记录**：`docs/decisions/` 记录 STOP 问题的答复，双方必须遵守。
4. **分支策略**：一张任务卡一个分支，命名 `feat/{任务卡号}-{简述}`（如 `feat/M1-02-agent-launch`）；修复用 `fix/{简述}`；main 受保护，只接受 MR。
5. **提交规范**（唯一格式，与《代码规范-全端》§2.2 一致）：
   ```
   [类型][任务卡号] 一句话说明
   ```
   类型：功能 / 修复 / 优化 / 重构 / 测试 / 文档 / 资源。示例：`[功能][M1-02] Agent 实现 LAUNCH_APP 指令拉起与 ACK`
6. **安全红线**：绝不提交密钥/凭证；AI 生成代码必须人工 review 后才能合入 main。

---

## 四、各模块接入指南

### 4.1 agent-android（VR 端 Agent）

| 项目 | 说明 |
|---|---|
| 语言 | Kotlin |
| 平台 | PICO VR 头显（Android 29+），Device Owner 独立 APK |
| 构建 | Gradle（wrapper 锁 8.5，AGP 8.1.4 + Kotlin 1.9.20） |
| 关键依赖 | TobService 企业 SDK（AAR 4.3.24，包名 `com.pvr.tobservice`）、WebSocket |

**开发环境**：Android Studio Hedgehog+；PICO 真机调试；**工程路径必须全 ASCII**（Windows AGP 限制）。

**结构约定**：包名 `com.{公司}.broadcast.agent.{模块}`，模块 = `command`（指令通道）/ `control`（设备管控）/ `billing`（记账）/ `quota`（额度）/ `monitor`（前台监听）/ `capture`（画面采集）/ `guard`（防篡改）。

**红线提示**：
- TobService 接口名/签名以 AAR 实锤为准（见 `docs/decisions/` 与 pico-enterprise-docs 技能），禁止凭记忆臆造；枚举拼写错误（`USB_PWOER_MAX`、`pbsControlAPPManger`）不得"纠正"。
- TobService 调用集中封装在 `control` 模块 `PicoTobService` 门面类。
- 计费红线见根目录 `CLAUDE.md`：先校验额度后拉起应用；`launchId` 数据库唯一约束；账本只 INSERT。

### 4.2 controller（控制端 PC / Pad）

| 项目 | 说明 |
|---|---|
| 引擎 | Unity 2022 LTS+ |
| 语言 | C# |
| 目标平台 | Windows PC + Pad（Android/iPad），建议一套代码双端（D6） |

**开发环境**：Unity Hub + Unity 2022 LTS；Visual Studio 2022 / Rider。

**结构约定**：
```
Assets/
├── Scripts/          # 业务逻辑（AI 只修改此目录下 .cs 文件）
├── Prefabs/
├── Scenes/
└── Plugins/
```

**通信**：经 local-service 中转指令；设备发现方式见 §8 未裁决清单（裁决前以任务卡为准）。

### 4.3 local-service（本地服务）

| 项目 | 说明 |
|---|---|
| 框架 | .NET 8（ASP.NET Core），Windows 后台服务，与控制端同机 |
| 语言 | C# |
| 通信 | WebSocket + gRPC（端口分配见 §8 未裁决清单） |

**开发环境**：.NET 8 SDK；Rider / VS 2022 / VS Code。

**分层**（物理隔离，依赖方向 Api → Application → Domain ← Infrastructure）：
```
local-service/
├── src/
│   ├── LocalService.Api/
│   ├── LocalService.Application/
│   ├── LocalService.Domain/
│   └── LocalService.Infrastructure/
├── tests/
└── LocalService.sln
```

**规范要点**：公开方法 XML 注释；异常走全局中间件；日志用 Serilog（结构化，带 traceId=msgId）；补传队列与实时上报共用同一幂等键，失败留队不丢弃。

### 4.4 cloud（轻云端）

| 项目 | 说明 |
|---|---|
| 框架 | .NET 8（ASP.NET Core）/ EF Core |
| 数据库 | **MySQL 8+**（D7 已定）+ Redis |
| 部署 | 生产 = 阿里云 ECS（Ubuntu + 宝塔）；测试 = 本地 Ubuntu 机 |

**分层**：同 local-service 四层结构；EF Core 迁移随代码提交，禁止手改生产库。

**规范要点**：
- REST 统一包裹 `{ code, message, data }`，业务错误码分段（1xxx–5xxx），登记表 `docs/specs/error-codes.md`。
- 账本表只 INSERT 不 UPDATE/DELETE（审计不可变），对账冲正用反向记录；验签失败拒绝 + 审计日志。
- 所有表带 `created_at` / `tenant_id`，跨租户查询显式过滤。
- 机密（签名私钥、DB 密码）只走环境变量/密钥服务。

### 4.5 content-sdk（内容 SDK）

| 项目 | 说明 |
|---|---|
| Unity 部分 | C#，Unity Package |
| UE 部分 | C++ / Blueprint 桥接（UE 5.3+） |
| 职责 | 运行时令牌校验（R15，内容保护唯一手段）、计费事件埋点、与 Agent 本机握手 |

**结构约定**：
```
content-sdk/
├── unity/    # Runtime/ + Editor/ + package.json
├── ue/       # Source/ + Content/ + PicoBroadcastSDK.uplugin
└── shared/   # 跨端共享协议定义
```

**红线提示**：协议字段 JSON camelCase 跨端逐字一致（《代码规范》§7）；协议承载格式（JSON / protobuf）见 §8 未裁决清单，裁决前按任务卡执行。UE 侧桥接代码集中在 `BroadcastBridge` 模块，游戏逻辑不直接碰 JNI/平台 API。

### 4.6 streamer-companion（串流伴生）

> ⚠️ 二期再建，当前仅保留目录占位。预期职责：串流会话管理、与 Agent 状态同步、串流侧计费见证；技术栈待定。

### 4.7 tests/protocol-sim（协议仿真器）

| 项目 | 说明 |
|---|---|
| 语言 | Python 3.11+，asyncio，零外部依赖优先（标准库 + websockets） |
| 维护者 | **Spec Owner（Kimi）编写维护，实现方只读** |

**结构约定**：
```
tests/protocol-sim/
├── simulators/     # agent_sim.py / controller_sim.py
├── scenarios/      # 测试场景
├── run.py
└── requirements.txt
```

**规范**：每个模拟器独立进程；用例断言行为而非日志；输出 `[PASS] AC2: ...` / `[FAIL] AC3: ...` 逐条对应任务卡验收标准；可独立重复运行、无残留状态。实现方任务卡完成后必须运行互通测试并把输出粘贴进回报。

### 4.8 tools/PicoM0Verify（验证 demo）

M0 里程碑验证工具（V1–V11 接口点测 + m0.log 落盘回放），属测试工具，由 Kimi 侧维护（D8a）。

---

## 五、AI 编码助手工作流

### 5.1 Claude Code（实现方）

```bash
cd pico-broadcast/
claude
```

- 根目录 `CLAUDE.md` 自动加载：身份、铁律、**12 条红线**、STOP 规则、模块速查，以仓库内版本为准（不要另行创建简化版模板覆盖它）。
- 提示词示例：
  ```
  请阅读 docs/specs/M1-02-agent-launch.md，遵守 CLAUDE.md 与 docs/conventions/ 规范实现。
  完成后本地构建并自测（含 tests/protocol-sim 互通），按 docs/specs/README.md 回报格式输出。
  ```
- 每个任务卡完成后输出五节回报：变更摘要 / 文件清单 / 测试报告 / 与规格偏差 / 待确认问题。
- 遇规格矛盾、协议歧义、需改只读目录、超出「不做」清单 → STOP 回报，不自作主张。

### 5.2 Kimi（Spec Owner）

Kimi 是规格与验收侧：出任务卡、写协议仿真器、评审回报、记录裁决、维护架构与记忆档案。**不写业务实现代码（含 demo 修复）**。

评审时上下文投喂顺序：`docs/conventions/` → 相关任务卡 → `docs/decisions/` → 实现方回报 + 相关源码。

### 5.3 构建命令速查

| 模块 | 命令 |
|---|---|
| agent-android / tools | `./gradlew build` |
| local-service / cloud | `dotnet build` |
| protocol-sim | `python run.py` |

---

## 六、Git 与仓库同步（D10 修订：GitHub 远端）

- 远端：GitHub **私有仓** `pico-broadcast`（必须 Private——涉及商业计费逻辑与未发布设备；**仓库任何位置不得出现 NDA 新设备型号名**）。
- main 分支开启保护：禁止直推，必须 PR + 人工 review。
- `.gitignore` 已预置密钥拦截（*.keystore、*.key、.env 等），不要放松。
- **Kimi 无仓库直连权限**：评审/归档由人类执行——日常评审 zip 变更模块 + 回报文件上传；里程碑节点整仓 zip（含 `.git`）归档。

---

## 七、安全红线

- **绝不**将 API Key / Token / 签名私钥提交到仓库。
- **绝不**让 AI 直接操作生产环境。
- **绝不**让实现方修改 `docs/specs/`、`tests/protocol-sim/`。
- 所有 AI 生成代码必须人工 review 后才能合入 main。
- 测试 Agent 只测不修：发现缺陷开回报，由实现方修复。

---

## 八、未裁决技术点清单（⚠️ 裁决前不作为实现依据）

以下条目来自 v1.0 指南或讨论草案，**尚未经任务卡/裁决确认**。实现方遇到时按 STOP 处理；Spec Owner 将在 M1 出卡时逐项裁决，结果写入 `docs/decisions/`：

| # | 条目 | v1.0 草案 | 状态 |
|---|---|---|---|
| U1 | local-service 端口分配 | WebSocket 9800（VR 端）/ gRPC 50051（控制端） | 待裁决 |
| U2 | 设备发现机制 | mDNS `_pico-broadcast._tcp` / UDP Multicast | 待裁决 |
| U3 | 协议承载格式 | protobuf（protocol.proto 双端生成） | 待裁决（现行规范为 JSON camelCase） |
| U4 | 库表命名规范 | 表名/字段小写蛇形、主键 bigint 自增、软删除 is_deleted | 待裁决（注意：账本表禁止 UPDATE/DELETE，软删除不适用账本） |
| U5 | cloud REST 细节 | `/api/v1/` 版本前缀、JWT、分页参数 | 待裁决（错误码分段已定） |

---

*文档版本：v3.0（合并版） | 最后更新：2026-10-16 | 与《代码规范-全端 v1》、CLAUDE.md、协作技能（spec-owner / claude-code-executor / qa-tester）配套使用*
