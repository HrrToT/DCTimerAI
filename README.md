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

感谢 [DCTimer-BLE](https://github.com/huizhiLLL/DCTimer-BLE) 作者 [huizhiLLL](https://github.com/huizhiLLL) 的开创性工作，实现了智能魔方蓝牙连接以及免 MAC 地址快速连接功能，为本项目奠定了坚实基础。

---

## 项目说明

`DCTimerAI` 是基于 [DCTimer-BLE](https://github.com/huizhiLLL/DCTimer-BLE) 的二次开发版本。原项目已经支持普通魔方计时、智能魔方连接、蓝牙计时器、打乱生成、成绩保存、统计和智能魔方 3D 状态预览。

这个分支重点补上了 `MoYu AI / MoYu32` 的陀螺仪 / 姿态数据接入，让 3D 魔方能够跟随实物整体转动；同时也统一了主界面与状态弹窗的 3D 渲染，并加入了白色中心块 Logo 功能。

## 下载与安装

- [GitHub Releases](https://github.com/HrrToT/DCTimerAI/releases/latest)

> DCTimerAI 与原 DCTimer 使用不同包名，不会与原版 DCTimer 发生安装冲突。  
> 数据格式兼容原项目，可通过导出 / 导入数据库迁移历史成绩。

## 本分支改动

- **智能魔方**：支持 `MoYu AI / MoYu32` 的 `171` 姿态数据包读取、坐标映射和白顶绿前一键校准；主界面双击可快速重置视角，取消连接弹窗后也会保留智能模式，智能相关设置已置顶到设置页第一组。
- **交互与 Logo**：智能模式可直接从主界面进入连接流程，并新增白色中心块 Logo 功能，支持内置 Logo、自定义上传、圆形裁剪、三列滚动选择器，以及最多 `6` 个自定义槽位记忆。
- **3D 外观**：将原先偏贴纸式的预览重构为 `cubie` 级立体渲染，统一主界面和状态弹窗的显示方案，缩小层间缝隙，调整圆角与配色，让整体更接近现代实色魔方。

## 原有功能

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

本分支当前新增的姿态跟随能力优先面向：

- `MoYu AI / MoYu32`

## 使用说明

1. 在 Android Studio 中打开项目并运行到安卓真机。
2. 在 App 中选择智能魔方计时模式。
3. 扫描并连接 `MoYu AI / MoYu32` 智能魔方。
4. 打开状态弹窗，将实物摆成白顶绿前后点击 `重置姿态`。
5. 如有需要，可在智能设置中打开 `白色中心块 Logo`，切换内置 Logo 或上传自定义 Logo。

## 开发环境

- AndroidX
- Android Gradle Plugin 8.9.2
- Gradle 8.11.1
- JDK 17
- `compileSdk / targetSdk` 35

## 主要改动文件

- `app/src/main/java/com/dctimer/activity/MainActivity.java`
- `app/src/main/java/com/dctimer/util/Moyu32CubeProtocol.java`
- `app/src/main/java/com/dctimer/view/SmartCube3DView.java`
- `app/src/main/java/com/dctimer/view/SmartCubeImageView.java`
- `app/src/main/java/com/dctimer/util/SmartCubeLogoProvider.java`
- `app/src/main/java/com/dctimer/activity/SmartCubeLogoCropActivity.java`
- `app/src/main/java/com/dctimer/view/SmartCubeLogoCropView.java`
- `app/src/main/java/com/dctimer/dialog/CubeStateDialog.java`
- `app/src/main/res/layout/dialog_cube_state.xml`
- `app/src/main/res/layout/dialog_smart_cube_logo_picker.xml`

## 当前维护

- 当前维护与定制：胡图图
- 技术协作：Codex
- 当前仓库地址：[HrrToT/DCTimerAI](https://github.com/HrrToT/DCTimerAI)

## 致谢

- [DCTimer-Android](https://github.com/MeigenChou/DCTimer-Android)
- [DCTimer-BLE](https://github.com/huizhiLLL/DCTimer-BLE)
- [cstimer](https://github.com/cs0x7f/cstimer)
- [smartcube-web-bluetooth](https://github.com/poliva/smartcube-web-bluetooth)
- [qiyi_smartcube_protocol](https://codeberg.org/Flying-Toast/qiyi_smartcube_protocol)
- [CubicTimer](https://github.com/hato-ya/CubicTimer)

## License

本项目沿用原项目的 GPLv3 协议。原项目版权和署名应继续保留，本分支新增的 MoYu AI 姿态跟随与 3D 外观相关代码由当前仓库维护。
