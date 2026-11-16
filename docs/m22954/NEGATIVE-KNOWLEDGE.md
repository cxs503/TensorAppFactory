# NEGATIVE-KNOWLEDGE 登记（m22954 · AUDIT-P0LINE）

登记对象：审计中发现的**虚标/超前表述/只在 CI 存在的门**。每条给出可复核的定位，供后续单"治本"取值。

| # | 仓 / 位置 | 表述 | 审计事实 | 处置建议 |
|---|---|---|---|---|
| N1 | TensorProductFactory `README.md` "Current implementation" 第 4 条 / `domain/release.py::Release` | "Track immutable release package references and real commercial outcomes" | `Release` 类**全仓 0 处实例化**（`grep -rn "Release(" product_factory/ tests/` 无命中）：无测试、无发布流程调用，仅 dataclass 定义 | 降级为 "domain type defined, no lifecycle wiring"；或补一条 `test_release.py` + 发布落库适配器 |
| N2 | TensorAppFactory `docs/QUALITY-GATES.md` 状态 3 "Built — artifact exists; package identity and version are verified" | 状态机给出 Built 定义 | 仓内**没有任何一处**能把产品推到 Built 的**可执行路径留证**（`build_product.py` 只是"调 gradle"，本机实测 `could not execute Gradle`；无 gradle wrapper、无预构建产物） | 在 CI 成功 run 上挂 `build-result.json` 作 Built 证据；否则标注 "Built 需 x86_64 runner 才能达成" |
| N3 | `.github/workflows/android-apk.yml` 步骤 10→14 | 隐含"CI 会产出 APK 制品" | 模拟器步（第 10 步）无 `continue-on-error`，且排在 assembleDebug 与 upload 之前；`upload-artifact` 设 `if-no-files-found: error` → **模拟器一失败，APK 与 build-result 全部不产出**（对应 11-15 模拟器失败史） | 模拟器步 `continue-on-error: true`，或 APK 步骤前置/`if: always()` |
| N4 | 两项目 `templates/android-native/`、`products/english-exam-coach/android/` | 教程/指令口径写作 `gradle wrapper` / `gradle assembleDebug` | 两处**均无 `gradlew`、无 `gradle/wrapper/gradle-wrapper.properties`**；构建版本只由 CI 的 action 注入 → 同 commit 在 CI 与本机用的是不同来源的 gradle | 提交 wrapper（8.9 + `distributionSha256Sum`），并把 `build_product.py` 默认命令改为 `./gradlew` |
| N5 | TensorAppFactory `products/english-exam-coach/content-pack.lock.json` + workflow 内容钉 | "sync_policy: byte-identical; update revision and blob SHA whenever canonical content changes" | 该门**只存在于 `.github/workflows/english-exam-coach-android.yml`**，工厂/测试侧无等价校验（`grep content-pack.lock` 仅命中 workflow）；本地重放虽一致（`efd5fb84…`），但离线/本地改内容不会被拦 | 在 `tests/` 加一条读 lock 并 `git hash-object` 比对的测试（CI 与本机共用） |
| N6 | TensorProductFactory `adapters/platform_client.py` 及其测试 | 文档称"authenticated HTTP client … mission polling" | 全部 4+4 条测试都是 `unittest.mock` 对 `urlopen` 的桩测；**零真实 :8001 联网证据** | 增加一条带 skip 标记的真实连通 smoke（有 token 时跑），把 "live integration" 从 WITHHELD 变 REAL |
| N7 | TensorProductFactory `tests/test_english_benchmark.py` | 文件名暗示有 benchmark 测试 | 文件内 **0 个 `def test_`**（68 通过全部来自其余 12 个文件） | 要么补断言，要么改名为 fixture 校验脚本，避免"看着有测试"的错觉 |
| N8 | 任务书前提 `apt-get install -y openjdk-17-jdk-headless` | 假定 Debian 系宿主 | 执行机为 **Anolis OS（yum/dnf）**，`apt-get: command not found`；等价 `yum install java-17-openjdk-devel` 成功 | 派单模板改为"JDK17 可用"而非绑定 apt；或按宿主探测安装器 |
| N9 | 任务书前提 "SDK 已预置 licenses 已带 → 应能出 APK" | 假定 SDK 可用 | SDK 二进制是 **x86-64**，宿主 **loongarch64**，`aapt2`/`adb` 均 `Exec format error`；google maven 不可达 | P0 线的 APK 验收必须落在 **x86_64 runner**（或装 qemu-user/box64 + 可达的 google maven 镜像），loongarch 节点只做 manifest/生成器/规则层验收 |

## 反例：本单**没有**虚标的两处（正向记录）

- `factory/build_product.py`：无 gradle 时**如实**写 `status=failed` + `failure_reason`，不伪造 passed/不伪造 SHA256。
- `factory/create_android_app.py`：校验失败**在建目录之前 return**（实测目标目录未创建），不是"先写后删"。