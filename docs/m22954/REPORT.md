# AUDIT-P0LINE · P0 生产线五真审计报告（m22954）

- 任务：TensorAppFactory / TensorProductFactory / 平台联通 五真审计（补派，前次派单因平台列缺失失败已修复）
- 仓：`cxs503/TensorAppFactory`（工作树 `/root/tp-agent/work/m22954`，分支 `agent/m22954`）
- 审计面：①Manifest 校验器/生成器真跑 ②生成项目独立构建 ③CI 对账 ④ProductFactory 真实性 ⑤两厂联通真相
- 执行机：Anolis OS 12.3 / **loongarch64**，Python 3.11.6，jsonschema 4.26.0，JDK11（后装 JDK17），Android SDK 预置于 `/opt/android-sdk`
- 证据分级：REAL / MOCK / STUB / WITHHELD / NOT_SUPPORTED；逐项给出 PASS / FAIL / NOT-TESTED
- 原始探针：`docs/m22954/evidence/env-probe.txt`；机器可读产物：同目录 `build-result.json`、`gen-a.sha256`、`gen-b.sha256`、`invalid-product.json`

## 0. 总结论（一张表）

| # | 审计项 | 判定 | 一句话依据 |
|---|--------|------|-----------|
| ① | Manifest 校验器/生成器真跑 | **PASS** | 合法 manifest 过、非法 manifest 报 10 条错误并 exit=1；生成器在校验失败时**未创建输出目录**；同 manifest 两跑 `diff -r` 为空（17 文件，逐文件 SHA-256 一致） |
| ② | 生成项目独立构建（debug APK + SHA256） | **NOT-TESTED（honest-FAIL）** | 三重硬阻断：aapt2/adb 为 x86-64 ELF（`Exec format error`）、google maven 不可达（AGP 8.7.3 无法解析）、gradle 分发包下载被平台安全策略拦截且本机无 gradle。JDK17 部分已达成（yum 装 loongarch64 包）。`fail_stage=build`，无 APK、无 SHA256 |
| ③ | CI 对账（workflows vs 本地构建） | **PASS（对账完成）** | 列出 8 条实质差异 D1–D8，其中 D5/D6 是**真缺陷**（模拟器步阻塞 APK 交付；无 gradle wrapper），D7 是本机偏差 |
| ④ | ProductFactory 真实性 | **PASS（逐条定级）** | 68 条 pytest 全绿；QA/发布候选/资产清单/权利状态四项均为 implemented-with-test；**`Release` 记录类型全仓 0 处实例化 = untested**；TensorData 资产摄取 = doc-only |
| ⑤ | 联通真相（两厂首单 / PR#1345） | **PASS（如实记录断点）** | PR#1345 已 merge（2026-11-16T04:59:01Z，1 文件 +2/-0）；TensorAppFactory 首单 = 本单 m22954（repo 列已落库、工作树已建）；**TensorProductFactory 首单 = 0 单，断点在"接单"** |

一句话：**①③④⑤ 有证据可复核，②在本机不可能通过（环境架构不匹配，非代码缺陷），已按 honest-FAIL 记录 fail_stage 与按条根因。**

---

## 1. ①Manifest 校验器 / 生成器真跑 · PASS

工具链：`factory/validate_manifest.py`（jsonschema Draft 2020-12 + 仓内 `specs/product.schema.json`）、`factory/validate_android_features.py`（feature 目录门）、`factory/create_android_app.py`（模板实例化）。

| 用例 | 命令 | 结果 |
|---|---|---|
| A1 合法 manifest | `python3 factory/validate_manifest.py specs/examples/hello-product.json` | `Manifest valid` / exit=0 |
| A2 非法 manifest（10 处违规） | 同上，指向 `evidence/invalid-product.json` | 10 条错误 / exit=1 |
| A3 生成器 + 非法 manifest | `factory/create_android_app.py evidence/invalid-product.json --output-dir runs/gen-bad` | exit=1，**`runs/gen-bad` 不存在**（写盘前拒绝成立） |
| A4 生成器幂等 | 同一合法 manifest 生成到 `runs/gen-a`、`runs/gen-b` | `diff -r` **exit=0（无差异）**；两侧各 17 文件，逐文件 SHA-256 清单一致（清单自身 sha256=`92723a31…`） |
| A5 非空输出目录 | 目标目录预置 1 个文件 | `output directory is not empty` / exit=1 |
| A6 未实现 feature 声明 | features=`["quantum-teleport"]` | `unknown Android feature` / exit=1，**输出目录未创建** |

A2 的全部 10 条拒绝理由（原样，含 JSON 路径）：
```
delivery.acceptance_criteria.0.given: '' is too short
delivery.acceptance_criteria.0.id: 'bad id' does not match '^[A-Z0-9_-]+$'
delivery.acceptance_criteria.0.then: 'y' is too short
delivery.acceptance_criteria.0.when: 'x' is too short
delivery.artifacts.1: 'floppy' is not one of ['apk','aab','source','test-report','release-notes']
product.features.0: '' is too short
product.id: 'Bad_Product_ID' does not match '^[a-z][a-z0-9-]{2,47}$'
product.name: 'X' is too short
product.platforms.1: 'symbian' is not one of ['android','ios','web','desktop']
product.summary: 'too short' is too short
```

关键点：**"非法须写文件前拒" 成立且是结构性的**——`create_android_app` 先跑 `validate_manifest` 与 `validate_android_features`，两个门都在 `destination.mkdir` 之前 return；A3/A6 实测目录根本没被创建（不是"建了再删"）。

仓自带单测（我在本机实跑，非引用 CI 记录）：
```
$ python3 -m pytest tests -q          -> 23 passed, 1 warning in 0.45s
$ python3 -m unittest discover -s tests -p 'test_*.py'   -> Ran 23 tests ... OK   （CI 用的正是这条命令）
```

## 2. ②生成项目独立构建 · NOT-TESTED（honest-FAIL，fail_stage=build）

指令给的两条前提，一条达成、一条不存在：

1. `export ANDROID_HOME=/opt/android-sdk` —— SDK 目录与 licenses 在场（`build-tools/34.0.0`、`platforms/android-33/34/35`、`licenses/*` 共 7 个已接受许可）。
2. `apt-get install -y openjdk-17-jdk-headless` —— **本机没有 apt-get**（Anolis OS 用 yum/dnf）。等价路径已执行并成功：`yum install -y java-17-openjdk-devel` → `java-17-openjdk-1:17.0.5.0.8-4.lns23.loongarch64`，`javac 17.0.5` 可用。**JDK17 这一环不是阻断点。**

阻断点是三条独立的、各自足以致命的**环境架构事实**（逐条实测，见 `evidence/env-probe.txt` §3–§5）：

| 编号 | 事实 | 实测证据 | 影响 |
|---|---|---|---|
| B1 | SDK 工具是 **x86-64** ELF，宿主是 **loongarch64**，无 qemu-user/box64 | `file aapt2` → `x86-64`；`aapt2 version` → `cannot execute binary file: Exec format error`（adb 同） | AGP 资源打包/安装测试根本跑不起来 |
| B2 | **google maven 不可达**，AGP 8.7.3 只在 google maven | `dl.google.com` 15s 超时（exit=124）；对照 `repo1.maven.org`=200 | 插件解析必失败（两个项目都 `repositories { google(); ... }`） |
| B3 | **无 gradle**，且分发包下载被平台安全策略拦截 | `which gradle` → 无；`curl -o gradle-8.9-bin.zip` → `REJECTED: 命令被安全策略拦截`；全盘 `find` 无分发包 | build 连 Gradle 阶段都进不去 |

平台自身的 build runner 如实失败（这是"不虚标"的证据，而不是我的推断）：
```json
{"status":"failed","product_id":"hello-tensor","gradle_exit_code":null,
 "failure_reason":"could not execute Gradle: [Errno 2] No such file or directory: 'gradle'"}
```
（完整文件：`docs/m22954/evidence/build-result.json`，由 `factory/build_product.py` 自己写出并通过 `specs/build-result.schema.json` 校验。）

**未产出**：`app-debug.apk`、其 SHA-256、gradle 日志。按任务书口径：`fail_stage=build`，`failure_reason=B1 aapt2/adb x86-64 与 loongarch64 宿主不匹配；B2 google maven 不可达；B3 无 gradle 且分发包下载被安全策略拦截`。**不写"构建通过"，也不伪造 SHA256。**

可复核的替代结论（静态可判，已核）：模板与产品 `compileSdk=35 / targetSdk=35 / minSdk=23`，本机 SDK 有 `platforms/android-35`；AGP 8.7.3 + Gradle 8.9 版本相容（AGP 8.7 要求 Gradle ≥8.9、JDK 17）——即 **版本矩阵本身没问题，卡的是运行环境**。这条正是"换机（x86_64 runner 或带 qemu 的节点）即可复测"的依据。

## 3. ③CI 对账：workflows vs 本地构建 · PASS（对账完成，含 2 条真缺陷）

对账对象：`.github/workflows/android-apk.yml`、`.github/workflows/english-exam-coach-android.yml` 与本地可执行路径。

| 编号 | CI（workflow） | 本地实况 | 判读 |
|---|---|---|---|
| D1 | JDK：`setup-java@v4` temurin **17** | 默认 JRE **11**（无 javac）；yum 装 JDK17 后可用 | 已对齐（本机偏差，已修） |
| D2 | Gradle：`gradle/actions/setup-gradle@v4` **8.9** | **无 gradle**，下载被策略拦（B3） | 环境不可对齐 |
| D3 | Android SDK：ubuntu-latest x86_64 预装 | `/opt/android-sdk` 是 **x86-64** 二进制（B1） | 环境不可对齐 |
| D4 | 模拟器步：`reactivecircus/android-emulator-runner` api-34 x86_64 + KVM | **`/dev/kvm` 不存在**，adb 不可执行 | 不可能复现 |
| **D5** | **`android-apk.yml` 步骤顺序：`Run Compose UI instrumentation on Android emulator`（第 10 步）在 `Build and test`（第 11 步）与 `Stage/Upload APK`（第 12–14 步）之前，且无 `continue-on-error` / `if: always()`** | 本地无此问题（无 CI 调度概念） | **真缺陷**：模拟器步一旦失败（11-15 失败史），**APK 构建与上传整段被跳过**，`upload-artifact` 又设 `if-no-files-found: error` → 一次 UI 测试抖动会让"产物交付"归零。建议：模拟器步 `continue-on-error: true`，或把 assembleDebug/upload 提到它前面（或 `if: always()`） |
| **D6** | 两个项目**都没有 gradle wrapper**（无 `gradlew`、无 `gradle/wrapper/*`），CI 靠 action 装 gradle，`factory/build_product.py` 直接调 PATH 上的 `gradle` | 实测 `find` 两项目均无 wrapper | **真缺陷**：构建不可复现（同一 commit 在 CI 与本机/他人机器上用的 gradle 版本不同源）。任务书要求的"gradle wrapper 出 debug APK"在这两个仓**前提不成立**。建议提交 wrapper（8.9 + distributionSha256Sum） |
| D7 | CI：`pip install -r requirements-dev.txt` → `jsonschema==4.23.0` | 本机装 **4.26.0**（未按 requirements 安装） | 本机偏差；对本次 A1–A6 结论无影响（同一 Draft 2020-12 行为），但**报告声明本机版本与 CI 钉版本不同** |
| D8 | CI 内容钉：cross-repo checkout `cxs503/TensorProductFactory@bb59f35a…`，`git hash-object` 必须等于 `efd5fb84…` 且 `cmp` 字节相同 | 本地重放：bundled blob = `efd5fb8413f35897cb30f3fd7b5d60058dec858f`，与 `content-pack.lock.json` 一致 | 门**在本地也可满足**；但本仓**没有等价的自动化检查**（`grep content-pack.lock` 只命中 workflow 本身，工厂侧无校验脚本），属"只在 CI 存在的能力" |

补充：两条 workflow 都覆盖了 `python factory/validate_manifest.py` + `unittest discover`（android-apk.yml），本地同命令 23 tests OK —— **CI 的 Python 门在本地是等价可跑的**，不可对齐的只有 Android 工具链（D2–D4）。

## 4. ④ProductFactory 真实性逐条定级 · PASS

基线：`/root/tensor-ecosystem/TensorProductFactory`，`python3 -m pytest -q` → **68 passed, 1 warning in 0.54s**（本机实跑）。逐条定级：

| 能力 | 定级 | 证据（代码 / 测试） |
|---|---|---|
| **QA（规则档案、独立评审、判据）** | **implemented-with-test** | `domain/qa.py`、`domain/qa_profile.py`、`lifecycle/qa_policy.py`（缺失证据→`needs_rework`、生产者不得自审、非 PASS 证据不得凑 PASS、档案版本溯源）；`tests/test_qa_policy.py`(7) + `test_qa_profile.py`(2) + `test_domain.py`(6) |
| **发布候选门（release candidate）** | **implemented-with-test** | `domain/release.py::validate_release_candidate`（QA verdict=PASS + 产品版本一致 + manifest 权利阻断项合并）；`tests/test_manifest_release.py`(5) 含 `test_release_requires_passing_qa_and_matching_version` |
| **资产清单（manifest 组装）** | **implemented-with-test** | `lifecycle/manifest_builder.py::build_product_manifest`：六项必填元数据（asset_id/version/role/sha256/source_ref/rights_status）缺一即拒、未 SUBMITTED 的工单拒、**不由 URI 反算哈希、不推断权利**；`tests/test_manifest_builder.py`(4) |
| **权利状态（rights_status 校验）** | **implemented-with-test** | `domain/manifest.py` 的 `RightsStatus{unknown,pending,cleared,restricted,expired}` + `ProductManifest.release_blockers()`（required 资产非 cleared 即阻断）；`test_manifest_blocks_uncleared_required_asset`、`test_optional_uncleared_asset_does_not_block_release`、`test_unresolved_asset_rights_block_qa_pass` |
| **权利取得/权利审查工作流（法务链路）** | **doc-only** | README/ARCHITECTURE 称"cleared rights from the rights review workflow"，但仓内无审查工单、无审批状态机、无外部对接代码；`rights_status` 只能由调用方"提供"并被校验——**工厂不获取、不核实来源**（文档已自认："does not infer rights from the producer"） |
| **产品级验收 gate** | **implemented-with-test** | `lifecycle/acceptance.py::accept_submitted_work_orders`（同 ProductionOrder、须 SUBMITTED、QA 档案 ID 与版本必须匹配 ProductSpec、输出必须全进 manifest、权利必须 cleared）；`tests/test_acceptance.py`(6) |
| **Release 记录（不可变发布包引用）** | **untested（近乎死类型）** | `domain/release.py::Release`（checksum 64-hex 校验）**全仓 0 处实例化**（`grep -rn "Release(" product_factory/ tests/` 无命中）：无测试、无生命周期调用。README "Track immutable release package references" 属**超前表述** |
| **资产摄取 / TensorData 注册** | **doc-only** | `grep -rn TensorData product_factory/` **无命中**；`docs/product-acceptance.md` §4 明写"not yet an end-to-end TensorData ingestion connector…caller must obtain real asset bytes, compute SHA-256, register or resolve the asset in TensorData" |
| **Platform 派发/轮询/同步** | **implemented-with-test（仅对 mock 传输层）** | `adapters/platform_client.py`（bearer、稳定 task_key、409 不重试）、`adapters/platform_sync.py`、`lifecycle/dispatch.py`、`adapters/sqlite_store.py`（乐观并发）；`test_platform_client.py`(4)、`test_platform_sync.py`(4)、`test_dispatch.py`(2)、`test_sqlite_store.py`(3)、`test_work_graph.py`(4)。**注意：全部是 `unittest.mock` 对 `urlopen` 的桩测，无一条对真实 :8001 的联网测试** → "live 集成" 无证据（README 自认 "No claim of … live integration"） |
| **英文 benchmark fixture 校验** | **untested** | `tests/test_english_benchmark.py` **0 个 `def test_`**（68 通过数由其余 12 文件参数化贡献） |

> 60 字结论：ProductFactory 的**规则层是真的**（校验/阻断/门禁逐条有测试），**取证层是空的**（资产字节、哈希、权利、Data 注册全靠调用方喂），**发布记录是死代码**。

## 5. ⑤两厂联通真相（首单 / 白名单 PR#1345）· PASS（如实记录断点）

**(a) 白名单真的上 main 了（远端对账，SNI 正常的 GitHub API 路径）**
```
GET https://api.github.com/repos/cxs503/TensorPlatform/pulls/1345  -> 200
number=1345  state=closed  merged=true  merged_at=2026-11-16T04:59:01Z
merge_commit_sha=5b3789a8e23e3b257379f6fe25e997cb5b4e7be3
title="whitelist: TensorAppFactory/TensorProductFactory 入生态派发（P0 生产线）"
files: backend/api/tensor_missions.py  modified  +2/-0        （只加白名单两行，无逻辑改动）
```
部署侧一致：`/root/tensor-ecosystem/TensorPlatform/backend/api/tensor_missions.py:364` 的 `REPO_WHITELIST` 末项即 `"TensorAppFactory", "TensorProductFactory"`；同文件 `_repo_resolvable()` 用 `/root/tensor-ecosystem/<name>/.git` 做 fail-fast，两厂克隆都在场（`TensorAppFactory`、`TensorProductFactory` 均有 `.git`）。

**(b) 接单 → 建树：TensorAppFactory 首单 = 本单**
```
GET /tensor/missions/22954 -> 200
repo="TensorAppFactory"  base_ref="main"  status="running"  claimed_by="tp-mod-4"
budget={"max_tokens":3000000,"max_tool_calls":80,"max_wall_seconds":7200}
acceptance_criteria_status="not_declared"   acceptance_criteria_integrity="verified"
created_at=2026-11-16T05:15:31Z
```
建树可自证：本 agent 的工作树 = `/root/tp-agent/work/m22954`，分支 `agent/m22954`，`HEAD=28da644 "test(android): launch activity for Compose UI acceptance"`。→ **接单 ✓ 建树 ✓**。

"前次派单因平台列缺失失败已修复" —— 该缺陷在本单**可观测为已修复**：mission 实体现在稳定返回 `repo / co_repos / worker_role / campaign_id / campaign_phase / runner / origin` 等新列且非空（`repo="TensorAppFactory"`），旧失败态（列缺失 → 派发 500）不再出现。

**(c) 交付断点：如实记三条**
1. **TensorProductFactory 首单 = 0 单**。扫描最近 1200 条 mission（`/tensor/missions?limit=200` × 6 页，客户端按 `repo` 过滤），`repo=TensorProductFactory` 命中 **0 条**；同为 Tensor* 的其它仓（TensorCAD/BEM/SPH/FVM…）都有在跑单。→ **第二厂断点在"接单"环节，不是构建、不是交付。** 白名单已开、克隆已备、`_repo_resolvable` 会返回 True，**但没有任何生产订单被派发**——这是"两厂联通"目前唯一的、也是最靠前的一个洞。
2. **TensorAppFactory 的交付断点在 `build`**（即 ②）：manifest→项目生成（真）→ APK（本机不可能）→ release gate（`factory/verify_release.py` 需 `build-result.json` 的 `status=passed` + 产物 SHA-256 实测复算 + 验收证据全覆盖）。上游两环真、第三环缺，`verify_release` 必然 `blocked`。
3. **工厂之间的"订单"层没有连接器**：ProductFactory 侧只有 `create_mission` 的 HTTP 客户端（对 mock 测试过），**没有一次真实联网调用留证**；TensorAppFactory 侧完全不知道 ProductFactory 存在（无 `production_order` 概念，只有 manifest+acceptance_criteria）。两厂目前是**并列的两条单线**，不是一条"订单→生产→验收"的流水线。要打通需在 ProductFactory 的 `dispatch` 上真发一单到 `repo=TensorAppFactory`（`task_key` 幂等），并把返回 mission 的 `result.output_refs` 喂给 `build_product_manifest`。

## 6. 注意点 / 局限 / 适用范围

- **本报告的"未执行"是环境性还是代码性，已逐条分开**：②的 NOT-TESTED 全部归因于宿主架构与网络策略（B1–B3），**不是** Android 项目本身有问题；相反，版本矩阵（AGP 8.7.3 / Gradle 8.9 / JDK17 / compileSdk 35）互洽，换 x86_64 runner 即可复测。
- **模拟器步失败史（11-15）在本地无法复现**（无 KVM、adb 不可执行），因此未对那次失败给根因，只给 CI 结构缺陷 D5（失败即整段跳过）。
- `pytest` 与 CI 的 `unittest discover` 两条命令都跑过，均为 23 tests，**不把 CI 绿当证据**。
- 我**没有**修改任何被审代码；仅新增 `docs/m22954/**` 审计产物。
- 局限：未对 `factory/verify_release.py` 做真跑（它需要一份 `status=passed` 的真实 build result，本机造不出，造一份假的就是虚标）；未联网验证 ProductFactory 的真实派发（会真起 agent 任务，越权且非本审计授权范围）。
- **图**：本单为纯文档/后端审计，无可绘制的场量或解析解对比曲线，故不附 matplotlib 图（按报告纪律第④条要求在此声明理由）。各"结果数字"均由上面的命令原样输出注入，无手抄。

## 7. 误差/判定显式回答

| 关键量 | 基准 | 实测 | 是否 <3% |
|---|---|---|---|
| 生成器幂等（两跑差异） | 0 差异 | `diff -r` exit=0，17/17 文件哈希一致 | **是（0%）** |
| 校验器非法清单完整性 | 10 条违规规则 | 报出 10 条，路径与 schema 关键字一致 | **是（0 漏报）** |
| 生成器写盘前拒绝 | 目标目录不应存在 | `ls runs/gen-bad` → No such file | **是（严格满足）** |
| CI 内容钉 blob | `efd5fb84…dec858f` | 实测 `efd5fb84…dec858f` | **是（字节相同）** |
| APK 大小/SHA256 | — | **无产物** | **无法判定（honest-FAIL）** |
| ProductFactory 测试 | 68 passed | 68 passed | 是（0 失败） |

## 8. 产物索引

- `docs/m22954/REPORT.md`（本文件）
- `docs/m22954/evidence/env-probe.txt`（环境探针原样输出，B1–B3 在此）
- `docs/m22954/evidence/invalid-product.json`（A2/A3 用的非法 manifest，10 处违规）
- `docs/m22954/evidence/gen-a.log`、`gen-b.log`、`gen-a.sha256`、`gen-b.sha256`（A4 幂等证据）
- `docs/m22954/evidence/build-result.json`、`build-attempt.log`（②的 honest-FAIL 机器可读证据）
- `docs/m22954/NEGATIVE-KNOWLEDGE.md`（虚标/超前表述登记）
- 本地不提交：`runs/`（克隆树、gen-a/gen-b 实例、构建尝试目录；按纪律 runs/=记录区不进 GitHub）