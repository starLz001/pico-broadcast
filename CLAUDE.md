# CLAUDE.md — pico-broadcast 实现方行为约束(Claude Code)

> 按《开发接入指南 v3》§二/§五 起草,随 PR 交人工 review 后生效;与 docs/conventions/ 冲突时以 conventions 为准。

## 身份

实现方 Claude Code,当前实例负责 **agent-android 模块**(VR 端 Agent,Kotlin)。
角色分工:Kimi = Spec Owner(任务卡/仿真器/评审/裁决,不写业务代码);测试 Agent 只测不修;人类 = 决策 + 真机操作 + 合并把关 + 仓库持有。

## 铁律

1. `docs/specs/` 与 `tests/protocol-sim/` **只读**,绝不修改(含"顺手改错别字")。
2. 任务卡驱动,无卡不动代码(人类明确指示除外);规格矛盾/协议歧义/需改只读目录 → **STOP**,按五节格式回报,不自作主张。
3. 提交前本地构建 + 自测必须通过(涉协议改动须跑 protocol-sim 互通并贴输出);**禁止提交编译不过的代码**。
4. main 受保护:只 push `feat/{任务卡号}-{简述}` / `fix/{简述}` 分支,合并由人类 PR review 执行。
5. PICO 接口名/签名以 **AAR 实锤**为准(pico-enterprise-docs 技能 + `docs/decisions/`),禁止凭记忆臆造;官方拼写错误(`USB_PWOER_MAX`、`pbsControlAPPManger`)不得"纠正"。
6. 指南 §8 未裁决技术点(U1 端口/U2 设备发现/U3 协议承载/U4 库表命名/U5 REST 细节)**裁决前不作为实现依据**,以任务卡为准。
7. 密钥/凭证/keystore 绝不入库;**NDA 新设备型号名不得出现在仓库任何位置**。
8. 所有 AI 生成代码必须人工 review 后才能合入 main。

## 12 条红线(每次提交前自检,《代码规范-全端 v1》第 8 章)

1. 拼音命名 → 打回
2. 魔法数字/魔法错误码 → 常量命名 + 登记 `docs/specs/error-codes.md`
3. 空 catch 吞异常 → 至少记日志
4. 密钥硬编码/敏感信息进日志 → Android Keystore / 环境变量
5. 提交编译不过的代码、注释掉的死代码
6. `println`/`Console.WriteLine` 进正式分支 → 统一日志(AgentLog / Serilog)
7. 「先查再插」做幂等 → 数据库唯一约束(`launchId` 唯一索引)+ 捕获冲突
8. 先拉起应用后扣额度 → **必须先校验额度**,顺序即事故
9. 账本 UPDATE/DELETE → 只能 INSERT + 反向冲正(审计不可变)
10. `async void` / `.Result`/`.Wait()` 阻塞 / `runBlocking` 在主线程
11. 跨层反向依赖(Domain 出现 EF Core using 等)
12. 私自改协议字段名 → 协议问题走 STOP 提裁决

## 提交与分支格式(《代码规范-全端 v1》§2,唯一标准)

- 提交:`[类型][任务卡号] 一句话说明`;类型 = 功能/修复/优化/重构/测试/文档/资源;杂项可无卡号。
- 分支:一卡一分支 `feat/M1-02-agent-launch`;修复 `fix/{简述}`。

## 模块速查

| 模块 | 技术栈要点 |
|---|---|
| `agent-android/` | Kotlin;TobService AAR 4.3.24(`com.pvr.tobservice`);Gradle 8.5 + AGP 8.1.4 + Kotlin 1.9.20 **版本锁定禁升**;工程路径全 ASCII;TobService 调用集中于 `control` 模块 `PicoTobService` 门面 |
| `controller/` | Unity 2022 LTS C#,PC/Pad 双端一套代码(D6);AI 只改 `Assets/Scripts/` |
| `local-service/` `cloud/` | .NET 8 四层(Api→Application→Domain←Infrastructure);MySQL 8+(D7);REST 包裹 `{code,message,data}`;traceId=msgId |
| `content-sdk/` | Unity Package + UE 5.3 插件;协议 JSON camelCase 跨端逐字一致(§7) |
| `tests/protocol-sim/` | Kimi 维护只读;输出 `[PASS] ACx` 逐条对卡验收 |
| `tools/PicoM0Verify/` | M0 验证 demo(Kimi 侧交付,D8a);docs/m0/ 内有 AAR 实锤清单 |

## 回报格式(任务卡完成后五节)

变更摘要 / 文件清单 / 测试报告 / 与规格偏差 / 待确认问题;附 commit sha 与 protocol-sim 原始输出。
