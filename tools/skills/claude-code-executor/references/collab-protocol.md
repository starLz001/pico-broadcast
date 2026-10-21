# 协作协议（双方共用）

Spec Owner（总管理 Agent）与 Claude Code（实现方）之间的文件与回报格式约定。两侧技能各存一份，内容必须一致；修订时同步两侧。

## 仓库约定

```
docs/specs/          # 任务卡（Spec Owner 写，Claude Code 只读）
tests/protocol-sim/  # 协议仿真器与互通测试（Spec Owner 写）
docs/decisions/      # 规格裁决记录（Spec Owner 写；Claude Code 的 STOP 问题在此答复）
```

## Claude Code 回报格式（每个任务卡完成后必须输出）

```markdown
## 回报：任务卡 M1-02

### 变更摘要
一段话说清实现了什么。

### 文件清单
新增/修改的文件路径列表。

### 测试报告
- 单元测试：N 个通过（粘贴结果摘要）
- 仿真器互通：run.py 输出逐条粘贴（[PASS]/[FAIL] 对应 AC 编号）

### 与规格偏差
无 / 有（逐条说明：偏差内容、原因、建议处理方式）——未经说明的偏差视为不合格。

### 待确认问题（STOP 项）
实现中发现的规格矛盾或缺失，逐条列出。有此节时该任务卡不算完成。
```

## STOP 规则

Claude Code 发现以下情况必须停止实现并回报，不得自作主张：

1. 任务卡之间或任务卡与设计文档矛盾
2. 协议字段缺失/歧义，无法唯一确定实现
3. 需要修改 `docs/specs/` 或 `tests/protocol-sim/` 下的任何文件
4. 任务卡「不做」清单与实现目标冲突

STOP 项由 Spec Owner 裁决，结果写入 `docs/decisions/` 并修订任务卡（版本号 +1），Claude Code 按新卡继续。

## Spec Owner 评审输出格式

```
评审结论：通过 / 打回
打回项：（编号 + 类别 + 具体位置 + 期望行为）
建议：（非阻塞）
```
