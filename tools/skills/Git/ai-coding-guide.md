# AI 编码助手接入指南

本文档说明 Kimi 和 Claude Code 如何接入 pico-broadcast 仓库进行协作开发。

---

## 一、通用规则

1. **任务卡驱动**：所有开发任务由 `docs/specs/` 中的任务卡定义，AI 只读任务卡，不修改
2. **代码规范唯一权威**：`docs/conventions/` 目录下的规范文件是唯一标准
3. **裁决记录**：`docs/decisions/` 记录 STOP 问题的答复，AI 必须遵守
4. **分支策略**：每个任务卡对应一个 feature 分支，命名 `feature/{task-id}-{short-desc}`
5. **提交规范**：遵循 Conventional Commits（`feat:` / `fix:` / `docs:` / `chore:` 等）

---

## 二、Kimi 接入

### 2.1 账号与权限

| 项目 | 说明 |
|------|------|
| 平台 | Moonshot AI (Kimi) |
| 接入方式 | API Key 或 Web 端 |
| 仓库访问 | 通过 Git clone 获取代码，手动上传上下文 |
| 权限范围 | 只读代码 + 生成代码片段 |

### 2.2 连接方式

**方式 A：Web 端手动上传**
1. 打开 https://kimi.moonshot.cn
2. 将相关代码文件 / 任务卡拖入对话窗口
3. 在 prompt 中说明上下文和需求
4. 复制生成结果，本地提交

**方式 B：API 接入（推荐）**
```bash
# 环境变量
export KIMI_API_KEY="your-api-key"
export KIMI_BASE_URL="https://api.moonshot.cn/v1"

# 调用示例 (curl)
curl -X POST "${KIMI_BASE_URL}/chat/completions" \
  -H "Authorization: Bearer ${KIMI_API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "moonshot-v1-auto",
    "messages": [
      {"role": "system", "content": "你是 pico-broadcast 项目的 Kotlin 开发助手，遵守 docs/conventions/ 中的规范。"},
      {"role": "user", "content": "请根据任务卡实现 xxx 功能..."}
    ]
  }'
```

### 2.3 上下文投喂顺序

1. `docs/conventions/` — 代码规范（必须先读）
2. `docs/specs/{task-card}.md` — 当前任务卡
3. `docs/decisions/` — 相关裁决记录
4. 相关源代码文件

### 2.4 注意事项

- Kimi 无持久化仓库访问权限，每次需重新上传上下文
- 生成代码必须人工 review 后再提交
- 禁止将 API Key 提交到仓库

---

## 三、Claude Code 接入

### 3.1 账号与权限

| 项目 | 说明 |
|------|------|
| 平台 | Anthropic Claude Code (CLI) |
| 接入方式 | `claude` CLI 工具，直接在仓库目录运行 |
| 仓库访问 | 本地文件系统直接读取 |
| 权限范围 | 读/写代码（受 CLAUDE.md 约束）|

### 3.2 连接方式

**Step 1：安装 Claude Code CLI**
```bash
npm install -g @anthropic-ai/claude-code
```

**Step 2：认证**
```bash
claude login
# 按提示完成 OAuth 认证
```

**Step 3：在仓库中运行**
```bash
cd pico-broadcast/
claude
```

### 3.3 CLAUDE.md 配置

在仓库根目录创建 `CLAUDE.md`，Claude Code 会自动读取作为行为约束：

```markdown
# CLAUDE.md - Claude Code 项目约束

## 身份
你是 pico-broadcast 项目的开发助手，负责协助实现 VR 播控系统各模块。

## 规则
1. 任务卡 (docs/specs/) 是只读的，绝不修改
2. 代码规范以 docs/conventions/ 为准
3. 裁决记录 (docs/decisions/) 必须遵守
4. 修改代码前先理解现有架构
5. 提交前确保构建通过
6. 不提交密钥、凭证等敏感信息

## 模块速查
- agent-android/: Kotlin, Pico VR SDK
- controller/: Unity C#, PC/Pad
- local-service/: .NET 8, gRPC/WebSocket
- cloud/: .NET 8, Web API
- content-sdk/: Unity Plugin + UE Bridge
- tests/protocol-sim/: Python

## 提交规范
Conventional Commits: feat: / fix: / docs: / chore: / test: / refactor:
```

### 3.4 工作流

1. 人工创建任务卡 → `docs/specs/TASK-001.md`
2. 启动 Claude Code：`claude`
3. 提示词示例：
   ```
   请阅读 docs/specs/TASK-001.md，按照 docs/conventions/ 的规范，
   在 agent-android/ 中实现 xxx 功能。完成后运行构建确认通过。
   ```
4. Claude Code 生成代码 → 人工 review → `git add && git commit`
5. 推送分支 → 创建 PR

### 3.5 注意事项

- Claude Code 有文件系统访问权限，注意不要误改任务卡
- 建议在 feature 分支运行，不在 main 分支直接操作
- 大型重构建议分步执行，每步 review

---

## 四、对比总结

| 维度 | Kimi | Claude Code |
|------|------|-------------|
| 仓库访问 | 手动上传 / API | 本地文件系统直读 |
| 上下文持久化 | 无（每次重新上传）| 会话内持久 + CLAUDE.md |
| 代码修改 | 生成片段，人工粘贴 | 直接读写文件 |
| 适合场景 | 代码审查、方案讨论、片段生成 | 完整功能实现、重构、调试 |
| 成本 | API 按 token 计费 | 订阅制 / API 按 token 计费 |

---

## 五、安全红线

- **绝不** 将 API Key / Token 提交到仓库
- **绝不** 让 AI 直接操作生产环境
- **绝不** 让 AI 修改 `docs/specs/`（任务卡只读）
- 所有 AI 生成代码必须人工 review 后才能合入 main
