---
name: "git-collab-protocol"
description: "云端与本地 agent 用 git 协作开发：任务卡、文件级 ownership、分支、集成部署。触发：协同开发、多 agent。"
---

# Git 协作协议（云端 agent ↔ 本地 coding agent）

两边（云端 OpenClaw + 本地 Claude Code 等）读**同一份**规则，总线只有两个：**git 仓库**（代码 + 任务卡）与**任务卡本身**（状态与交接）。集成者是唯一有权合并与上线的一方。

## 角色

- **派活人**：提需求、验收
- **执行者**：领任务卡、改代码、跑通构建与测试、推分支
- **集成者**：云端 OpenClaw，负责 review + 自检 + 合并 + 部署

## 步骤

1. **建卡**：任务落到仓库 `<repo>/board/tasks/T-<编号>.md`。frontmatter 必填 `id / title / owner / agent / status / files / updated`。**一个任务一个文件**，绝不共用。
2. **领卡**：`status` 由 `todo` 改为 `doing`，填上 `owner` 与 `agent`。`files` 列出**本次会改的文件**；同一文件被两张进行中的卡同时声明时，后领的人**停手**。
3. **同步**：开工前先 `git pull --rebase`。基于旧代码开工是本协议最常见的翻车点。
4. **开分支**：命名 `<agent>/<任务编号>-<短语>`，如 `claude/T-005-dedupe-fix`。共享分支（master/main）**只接受合并，不接受直接提交**。
5. **干活**：改代码 → 本地跑通构建与测试。改动范围**不得越过卡里声明的 `files`**；要扩大范围，先改卡再动手。
6. **提交**：走 Conventional Commits，footer 带任务号（`task: T-005`）。一个逻辑改动一个 commit。
7. **交付**：push 分支 → 卡 `status` 改 `review`，正文写清「改了什么 / 怎么验证的 / 风险与未决项」，并附本地构建与测试的原始结论。
8. **集成**（只有集成者做）：review 差异 → 跑上线前自检（构建 + 针对改动的断言含边界与非法输入 + UI 文本断言）→ **全绿才合并** → 部署按「备份 → 换件 → 生效 → 验证」，并把 commit sha 与备份路径写回卡上。
9. **收尾**：卡 `status` 改 `done`。结论、决策、踩坑写进卡或 `<repo>/board/log/`，**不许只留在对话里**。

## 硬边界

- 共享分支**禁止 force-push**。
- 同一文件同一时刻**只允许一个 agent 写**。
- 生产环境变更**必须人工批准**；未全绿不得上线。
- 状态只在对话里 = 没发生。一切落盘。

## 完成判据

- 建卡后：frontmatter 完整，`files` 与其它进行中卡不重叠
- 交付时：本地构建与测试有原始输出，卡上有结论
- 集成后：卡上能查到 commit sha、自检结果、备份路径

## 接入一个新仓库

一个项目一个裸库：工作区与裸库（如 `/www/git/<项目>.git`）分开，工作区挂 `origin` 后 `git push -u origin <分支>`。

1. **先判断能不能就地 `git init`**：工作目录在 Web 根目录下时，**不要**在里面建 `.git` —— nginx 会把 `/.git/config` 当静态文件直接吐出去。改用独立源码目录（如 `/www/wwwroot/<项目>-src`）建仓库，部署靠 `rsync` 到线上目录；不在 Web 根目录下的项目（如 `/opt/...`）可以就地 init。
2. **排除备份与密钥再提交**：`.gitignore` 至少写 `*.bak.*`、`*.tar.gz`、`.env`。提交**前后各验一次**密钥没进去：`git ls-files | grep -cE '(^|/)\.env$'` 必须为 0。
3. **root 操作他人所有的仓库会报 `fatal: detected dubious ownership`**：先 `git config --global --add safe.directory <绝对路径>`，再执行 git 操作。
4. **首版快照后独立复核**：`git clone <裸库> /tmp/_v && cd /tmp/_v && git ls-files | wc -l`，确认文件数与分支齐全，而不是只看 push 成功。
5. **把「哪些文件绝不入库」写进该仓库的 `PROJECT.md`**，下一个人不必重新推一遍。
