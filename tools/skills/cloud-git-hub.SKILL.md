---
name: "cloud-git-hub"
description: "云端 Git 枢纽：多 agent 经 SSH 裸库互传项目文件与信息。触发：git服务器、裸库、clone、push、信息互通。"
---

# 云端 Git 枢纽 (cloud-git-hub)

一个项目一个裸库，作为云端 OpenClaw 与本地/其他 agent 之间**唯一的信息与文件总线**。信息只在仓库里才算发生；对话只用来指路。协作流程（任务卡、分支、集成）见技能 `git-collab-protocol`，本技能只管**枢纽 + 布局 + 信息契约**。

## 枢纽地址

- 主机：`wanmuchenglin.com`（解析到 8.138.150.201，即本机）
- 裸库根：`/www/git/`
- 访问：专用 `git` 用户 + SSH 公钥，shell 锁为 `git-shell`（只能做 git，拿不到命令行）
- URL：`ssh://git@wanmuchenglin.com/www/git/<项目>.git`

## 权限

- 每台机器各自生成 ed25519 key，公钥交云端加入 `/home/git/.ssh/authorized_keys`。
- 私钥不出本机；**绝不**把私钥、`.env`、口令写进仓库或 URL。

## 一个项目 = 一个裸库，库内按"端"分目录

```
<项目>/
├─ README.md
├─ PROJECT.md        # 哪些文件绝不入库：.env / *.bak.* / *.tar.gz
├─ board/tasks/      # 任务卡 T-001.md …（对齐 git-collab-protocol）
├─ board/log/        # 决策与踩坑记录
├─ docs/             # 前期策划（Kimi 产出）
├─ <端1>/            # 如 server/ 系统部分
└─ <端2>/            # 如 android/ 安卓端
```

加端 = 加一个顶层目录，不新建库。

## 接入一台机器（Win/Linux/mac 通用）

1. 生成 key：`ssh-keygen -t ed25519 -C "<机器名>"`
2. 公钥发云端 → 加入 `/home/git/.ssh/authorized_keys`
3. 克隆：`git clone ssh://git@wanmuchenglin.com/www/git/<项目>.git`
4. 开工前先 `git pull --rebase`

## 信息互通契约（关键）

- **一切落盘**：状态、决策、交接只在仓库里（`board/`、`docs/`）；只在对话里说的不算数。
- **开工先同步**：基于旧代码开工是本协议最常见的翻车点。
- **交付带证据**：分支 push 后，任务卡写「改了什么 / 怎么验证的 / 风险与未决项」并附 commit sha。
- **交接指路**：给对方 commit sha 或 URL，不转述内容。

## Kimi（无 git 能力）的桥接

Kimi 不能 push。它的策划产出由**用户或云端**落到 `docs/`：用户粘贴，云端整理成文件后提交，并在 `board/log/` 记一条 `source: kimi/<日期>`。

## 备份与快照

- 裸库本身即云端备份；关键节点再 `git bundle` 一份到 `/root/.openclaw/backups/`。
- 首版快照后独立复核：`git clone <裸库> /tmp/_v && (cd /tmp/_v && git ls-files | wc -l)`，确认文件齐全，而不是只看 push 成功。

## 硬边界

- 共享分支禁 force-push；同一文件同一时刻只一个 agent 写。
- 生产变更必须人工批准；未全绿不上线。
- 不在 Web 根目录内建 `.git`（nginx 会泄露 `/.git/config`）。
- 仓库与 URL 内不出现任何凭证。

## 完成判据

- 新机器：`git clone` 成功，且能 push 一个测试分支。
- 一次交接后：仓库里能查到 commit sha、任务卡状态、备份路径。

## 首次搭建（云端，按序）

1. **先备份**：`tar czf /root/.openclaw/backups/www-git-$(date +%Y%m%d-%H%M%S).tar.gz -C /www git`。
2. **建 git 用户**：先 `export PATH=/usr/sbin:/sbin:$PATH`（root 的 PATH 常缺 `/usr/sbin`，否则 `useradd: command not found`）；`grep -qx /usr/bin/git-shell /etc/shells || echo /usr/bin/git-shell >> /etc/shells`；`useradd -m -d /home/git -s /usr/bin/git-shell git`；`mkdir -p /home/git/.ssh && chmod 700 /home/git/.ssh && touch /home/git/.ssh/authorized_keys && chmod 600 /home/git/.ssh/authorized_keys && chown -R git:git /home/git`。
3. **每身份一把 key**：`ssh-keygen -t ed25519 -N "" -C "hub:<身份>" -f <目录>/<身份>`——生成到文件，**绝不 `cat` 私钥**；公钥逐条写入 authorized_keys，行首加 `restrict `，最后 `chown git:git` + `chmod 600`。云端代生成时，让用户经 SFTP 取走私钥并 chmod 600，随后删掉云端副本。
4. **只动 /www/git 目录本身**：`chgrp git /www/git && chmod 775 /www/git`——**不要** chown/chmod 里面已有的库。
5. **所有 git 操作以 git 用户执行**：`runuser -u git -- git init --bare --initial-branch=master /www/git/<项目>.git`（避开 dubious ownership，且新库与对象都归 git 所有）。
6. **种子提交**：骨架写进临时目录后 commit 再 `git push -u origin master`；写文件用 `cat > f <<'EOF' …EOF`，**不要用 `printf` 传含中文的格式串**——在嵌套 `bash -c` 里会报 `printf: usage:` 并被 `set -e` 中断整个脚本。
7. **复核**：全新 clone 数文件数（`git ls-files | wc -l`），而不是只看 push 成功。
