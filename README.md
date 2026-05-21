<h4 align="right"><strong><a href="README-en.md">English</a></strong> | 简体中文</h4>

<div align="center">
  <img src=".github/assets/dctimer-logo.png" alt="DCTimerAI logo" width="128" height="128" />

  <h1>DCTimerAI</h1>

  <p>
    基于 DCTimer-BLE 二次开发的魔方计时器，增加 MoYu AI / MoYu32 智能魔方姿态跟随、一键姿态校准，并重做了更接近现代实色魔方的 3D 外观。
  </p>

  <p>
    <img alt="Android" src="https://img.shields.io/badge/Android-targetSdk%2035-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
    <img alt="Java" src="https://img.shields.io/badge/Java-17-007396?style=for-the-badge&logo=openjdk&logoColor=white" />
    <img alt="Gradle" src="https://img.shields.io/badge/Gradle-8.11.1-02303A?style=for-the-badge&logo=gradle&logoColor=white" />
  </p>

  <p>
    <img src="JPG/35.jpg" alt="DCTimerAI main screen preview" height="280" />
    <img src="JPG/36.jpg" alt="DCTimerAI smart cube state preview" height="280" />
    <img src="JPG/37.jpg" alt="DCTimerAI cubie renderer preview" height="280" />
    <img src="JPG/HnVideoEditor_2026_05_19_124114096.gif" alt="DCTimerAI feature demo" height="280" />
  </p>
</div>

---

## 鸣谢

感谢 [DCTimer-BLE](https://github.com/huizhiLLL/DCTimer-BLE) 作者 [huizhiLLL](https://github.com/huizhiLLL) 的开创性工作，实现了三阶智能魔方蓝牙连接以及免 MAC 地址快速连接功能，为本项目奠定了坚实基础。

---

## 项目说明

`DCTimerAI` 是基于 [DCTimer-BLE](https://github.com/huizhiLLL/DCTimer-BLE) 的二次开发版本。原项目已经扩展智能魔方连接。

本项目了更新了 `MoYu AI / MoYu32` 的陀螺仪 / 姿态数据接入，复盘回放功能，并支持从其他计时器导入。

## 下载与安装

- [GitHub Releases](https://github.com/HrrToT/DCTimerAI/releases/latest)

> DCTimerAI 与原 DCTimer 使用不同包名，不会与原版 DCTimer 发生安装冲突。  
> 自2.2.7起，与 DCTimerBLE 使用不同包名，不会发生安装冲突。  


## 本分支更新

### v2.2.7
- **成绩排序**：将原“默认（最新在下）/ 最新在上”改为按完成日期排序，分别对应“日期（从新到旧）/ 日期（从旧到新）”；日期为空时按最早处理。
- **导入导出**
  - 新增侧边栏“导入导出”分类，集中放置备份、数据库迁移和外部计时器导入入口。
  - **数据设置一步导出**：支持一键导出数据库、当前设置、背景图以及智能魔方 Logo 等本地资源。
  - **从其他计时器导入**：支持 **CSTimer** 和 **Twisty Timer**，只解析三阶成绩；可选择“新增分组 / 追加已有分组 / 覆盖已有分组”，并保留复原时间、penalty、打乱、原始日期。
  - **原有导出 / 导入数据库**：继续兼容原项目 DCTimer，可通过旧入口迁移历史成绩。
- **解法与回放**
  - 详情页“解法”和回放共用同一套 `solve_meta / displaySteps` 数据源，CFOP 分段逻辑保持一致。
  - **EMS 显示**：支持将同一 CFOP 阶段内的相邻外层双步压缩显示为 `E / M / S` 记号，并在回放统计中按一步计算。
  - **回放界面**：重排回放布局，复用主界面背景，压缩火花图和按钮区，为 3D 魔方预留更大的显示空间。
### v2.2.4-v2.2.6
- **智能魔方**：支持 `MoYu AI / MoYu32` 的 `171` 姿态数据包读取、坐标映射和白顶绿前一键校准；主界面双击可快速重置视角，取消连接弹窗后也会保留智能模式，智能相关设置已置顶到设置页第一组。
  - **交互**：智能模式设置为默认时间产生方式，可直接从主界面进入连接流程。
  - **Logo**：新增白色中心块 Logo 功能，支持内置 Logo、自定义上传以及记忆。
- **3D 外观**：将原先偏贴纸式的预览重构为 `cubie` 级立体渲染，统一主界面和状态弹窗的显示方案，缩小层间缝隙，调整圆角与配色，让整体更接近现代实色魔方。

## BLE版本原有功能

- 普通魔方计时、观察计时、成绩保存和统计。
- 打乱生成、打乱导入 / 导出、数据库导入 / 导出。
- 智能魔方自动起停表、打乱进度提示、偏差纠错。
- 智能魔方实时 3D 状态预览。
- QiYi Smart Timer 蓝牙计时器支持。
- WCA 观察模式 8 秒 / 12 秒语音提醒。

## 支持设备

原项目已支持：

- `Moyu32` / `MoYu AI`
- `QYSC` / `Tornado V4`
- `GAN v2 / v3 / v4`
- `QiYi Smart Timer`

本项目当前新增的姿态跟随能力面向：

- `MoYu AI / MoYu32`


## 开发环境

- AndroidX
- Android Gradle Plugin 8.9.2
- Gradle 8.11.1
- JDK 17
- `compileSdk / targetSdk` 35

## 主要改动文件

- `app/src/main/java/com/dctimer/APP.java`
- `app/src/main/java/com/dctimer/activity/MainActivity.java`
- `app/src/main/java/com/dctimer/database/DBHelper.java`
- `app/src/main/java/com/dctimer/model/Result.java`
- `app/src/main/java/com/dctimer/model/SmartCubeSolveReconstruction.java`
- `app/src/main/java/com/dctimer/dialog/ImportExportDialog.java`
- `app/src/main/java/com/dctimer/dialog/OtherTimerImportDialog.java`
- `app/src/main/java/com/dctimer/dialog/ResultDialog.java`
- `app/src/main/java/com/dctimer/dialog/SolveReplayDialog.java`
- `app/src/main/java/com/dctimer/util/BackupManager.java`
- `app/src/main/java/com/dctimer/util/ExternalTimerImportManager.java`
- `app/src/main/java/com/dctimer/util/Moyu32CubeProtocol.java`
- `app/src/main/java/com/dctimer/util/Stats.java`
- `app/src/main/java/com/dctimer/util/SliceMoveUtils.java`
- `app/src/main/java/com/dctimer/view/SmartCube3DView.java`
- `app/src/main/java/com/dctimer/view/SmartCubeImageView.java`
- `app/src/main/java/com/dctimer/view/SolveReplayRenderer.java`
- `app/src/main/java/com/dctimer/util/SmartCubeLogoProvider.java`
- `app/src/main/java/com/dctimer/activity/SmartCubeLogoCropActivity.java`
- `app/src/main/java/com/dctimer/view/SmartCubeLogoCropView.java`
- `app/src/main/java/com/dctimer/dialog/CubeStateDialog.java`
- `app/src/main/res/menu/activity_main_drawer.xml`
- `app/src/main/res/layout/dialog_import_export.xml`
- `app/src/main/res/layout/dialog_cube_state.xml`
- `app/src/main/res/layout/dialog_other_timer_import.xml`
- `app/src/main/res/layout/dialog_other_timer_import_target.xml`
- `app/src/main/res/layout/dialog_solve_replay.xml`
- `app/src/main/res/layout-land/dialog_solve_replay.xml`
- `app/src/main/res/layout/dialog_smart_cube_logo_picker.xml`

## 当前维护

- 当前维护与定制：胡图图
- 技术协作：Codex、Claude Code
- 当前仓库地址：[HrrToT/DCTimerAI](https://github.com/HrrToT/DCTimerAI)

## 致谢

- [DCTimer-Android](https://github.com/MeigenChou/DCTimer-Android)
- [DCTimer-BLE](https://github.com/huizhiLLL/DCTimer-BLE)
- [cstimer](https://github.com/cs0x7f/cstimer)
- [smartcube-web-bluetooth](https://github.com/poliva/smartcube-web-bluetooth)
- [qiyi_smartcube_protocol](https://codeberg.org/Flying-Toast/qiyi_smartcube_protocol)
- [CubicTimer](https://github.com/hato-ya/CubicTimer)

## License

本项目沿用原项目的 GPLv3 协议。
