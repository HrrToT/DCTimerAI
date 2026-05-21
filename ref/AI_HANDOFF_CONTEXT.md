# DCTimerAI 项目交接背景

这份文档是给接手本项目的其他 AI / 自动化代理准备的快速背景说明。更细的约定拆到了同目录下的专题文档，接手时建议先读本文件，再读专题文件。

## 1. 项目定位

- 项目名：`DCTimerAI`
- 基础仓库：`DCTimer-BLE`
- 当前维护仓库：`HrrToT/DCTimerAI`
- 技术栈：原生 Android Java，AndroidX，Gradle 8.11.1，JDK 17，compileSdk/targetSdk=35
- minSdk：14

核心方向：

1. MoYu AI / MoYu32 姿态与智能魔方数据接入
2. 智能魔方成绩记录、解法重建、复盘回放
3. 3D 魔方渲染、外观、手指交互
4. 数据导入导出与本地备份

## 2. 必读专题文档

- [EMS_HANDLING_CONVENTIONS.md](EMS_HANDLING_CONVENTIONS.md)：EMS 处理、物理动作链、显示动作压缩、统计口径、回放约定。
- [VIRTUAL_CUBE_INTERACTION_CONSTRAINTS.md](VIRTUAL_CUBE_INTERACTION_CONSTRAINTS.md)：虚拟魔方手指交互方法、`U/R/F` basis 维护、拖动残量、实现约束。

这两个文档记录的是当前讨论后的方向，不要只依据旧代码中的白顶绿前固定假设继续改。

## 3. 最近新增和调整的功能

### 3.1 一键导出/导入

- ZIP 文件使用 `.dct` 扩展名，包含 `database.db`、`settings.json`、`logos/`。
- 导入策略：合并，不覆盖已有成绩数据。
- 入口：`ImportExportDialog` 中的导入/导出选项。
- 主要文件：`BackupManager.java`、`ImportExportDialog.java`、`dialog_import_export.xml`。

### 3.2 解法回放

- `SolveReplayDialog.java` 在 3D 魔方上动画回放整条还原过程。
- `SolveReplayRenderer.java` 绘制火花图：每步耗时按对数映射成柱状图，并按 CFOP 阶段着色。
- 回放界面支持播放/暂停、上一步/下一步、跳起点/终点、速度切换、火花图点击跳转。
- 入口：`ResultDialog` 中“回放”按钮，仅智能魔方成绩可见。
- 数据来源：成绩表中的 `moves` 与 `solve_meta`。若 `solve_meta.physicalMoves` 存在，回放优先使用物理动作链。

经验教训：

- `CubieCube.move(int)` 返回新对象，必须接住返回值：`cc = cc.move(m)`。
- `getMoveSequence()` 返回的是 pretty solve，可能含 `// Cross` 等注释；解析 token 时必须限制合法记号长度，避免把 `F2L` 误当成 `F`。
- 任何涉及 facelet 偏移的逻辑要区分 face 索引 `0..5` 和 face 偏移 `0,9,18,27,36,45`。

### 3.3 解法重建和 EMS 显示压缩

- `SmartCubeSolveReconstruction` 现在同时维护：
  - `physicalMoves`：真实物理动作链，只包含智能魔方实际上报/可执行的外层动作。
  - `displayMoves`：给用户看的公式文本，可把连续的外层对向组合压缩成 `E/M/S`。
  - `moves`：兼容旧字段，目前等同于 `displayMoves`。
- `moveCount` 与 TPS 统计按 `displayMoves` 口径计算，所以一个识别出的 EMS 显示步算一步。
- 详细规则见 [EMS_HANDLING_CONVENTIONS.md](EMS_HANDLING_CONVENTIONS.md)。

### 3.4 3D 魔方手指交互

- `SmartCube3DView` 增加 replay interaction mode，用于回放弹窗中的虚拟魔方查看。
- 回放模式不再应该依赖“永远白顶绿前”的固定判断，而是维护当前提交的 `U/R/F` basis。
- 横向拖动对应当前 `U` 轴的 `y/y'`，纵向拖动对应当前 `R` 轴的 `x/x'`，不提供 `z` 手势。
- 详细规则和当前约束见 [VIRTUAL_CUBE_INTERACTION_CONSTRAINTS.md](VIRTUAL_CUBE_INTERACTION_CONSTRAINTS.md)。

## 4. 当前重点风险

### 4.1 EMS 不应重新成为底层真相

智能魔方硬件按外层/轴心动作上报，不能直接产生真正的中层切片动作。当前产品约定是：

- 底层重建只相信物理动作链。
- EMS 只作为显示层压缩和未来回放视觉包装。
- 白顶绿前只作为解法开始的初始基准，不是所有后续公式的永久基准。

后续修 EMS 时不要把 `E/M/S` 写回成内部主线动作，也不要用 EMS 去改变解法重建 basis。

### 4.2 虚拟魔方拖动仍是敏感区域

当前交互方向的核心不是“屏幕上下左右固定映射到白/绿”，而是：

- `x` 绕当前 `R` 面对应轴转。
- `y` 绕当前 `U` 面对应轴转。
- 每次提交后必须更新 `U/R/F`。
- 横向未过 90 度阈值的残量只是视觉残量，不参与下一次 `x` 的轴计算。

如果继续修拖动 bug，优先检查 `SmartCube3DView.CubeViewBasis` 的 `turnX/turnY`、方向表和 residual 组合顺序。

## 5. 安装和验证方式

当前 debug 安装常用流程：

```powershell
$env:JAVA_HOME='C:\Users\Lenovo\.jdks\ms-17.0.18'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
adb push app\build\outputs\apk\debug\app-debug.apk /data/local/tmp/
adb shell pm install -r /data/local/tmp/app-debug.apk
```

注意：

- 换回 release 版通常需要先卸载 `com.dctimer.ble`，签名不同会导致数据丢失，卸载前必须跟用户确认。
- 小的文档或单点 UI 修改不一定要跑完整构建；涉及解法重建、回放、`SmartCube3DView`、Gradle 配置时应至少跑 `testDebugUnitTest`，必要时跑 `assembleDebug` 并装机验证。

## 6. 最关键的代码位置

| 模块 | 文件 |
|---|---|
| 主流程/设置/蓝牙 | `app/src/main/java/com/dctimer/activity/MainActivity.java` |
| 解法重建 | `app/src/main/java/com/dctimer/model/SmartCubeSolveReconstruction.java` |
| 回放弹窗 | `app/src/main/java/com/dctimer/dialog/SolveReplayDialog.java` |
| 火花图 View | `app/src/main/java/com/dctimer/view/SolveReplayRenderer.java` |
| 3D 渲染和手势 | `app/src/main/java/com/dctimer/view/SmartCube3DView.java` |
| EMS/切片工具 | `app/src/main/java/com/dctimer/util/SliceMoveUtils.java` |
| EMS 调试弹窗 | `app/src/main/java/com/dctimer/dialog/EmsDebugDialog.java` |
| 备份管理 | `app/src/main/java/com/dctimer/util/BackupManager.java` |
| MoYu 协议 | `app/src/main/java/com/dctimer/util/Moyu32CubeProtocol.java` |
| 结果弹窗 | `app/src/main/java/com/dctimer/dialog/ResultDialog.java` |
| 导入导出弹窗 | `app/src/main/java/com/dctimer/dialog/ImportExportDialog.java` |
| Logo 管理 | `app/src/main/java/com/dctimer/util/SmartCubeLogoProvider.java` |
| 数据库 | `app/src/main/java/com/dctimer/database/DBHelper.java` |
| 设置存储 | `app/src/main/java/com/dctimer/APP.java` |
| 菜单 | `app/src/main/res/menu/activity_main_drawer.xml` |

## 7. 开发约束

- 不要回滚用户或其他代理已有改动；本仓库经常处于 dirty worktree。
- 智能魔方与蓝牙计时器业务链保持分离。
- 设置页逻辑已拆分到 `buildSettingSections` 私有方法，继续沿用此结构。
- 涉及公式文本、动作统计、回放动作源时，先区分 `physicalMoves` 和 `displayMoves`。
- 涉及虚拟魔方视角时，先区分“已提交 basis”和“拖动中的视觉 residual”。
