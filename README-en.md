<h4 align="right">English | <strong><a href="README.md">简体中文</a></strong></h4>

<div align="center">
  <img src=".github/assets/dctimer-logo.png" alt="DCTimerAI logo" width="128" height="128" />

  <h1>DCTimerAI</h1>

  <p>
    A speedcubing timer based on DCTimer-BLE, with MoYu AI / MoYu32 gyroscope orientation tracking, one-tap orientation calibration, and a rebuilt stickerless-style smart cube renderer.
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

## Acknowledgements

Thanks to [huizhiLLL](https://github.com/huizhiLLL), the author of [DCTimer-BLE](https://github.com/huizhiLLL/DCTimer-BLE), for the pioneering work on smart cube Bluetooth connection and MAC-free quick connection, which laid the foundation for this project.

---

## About

`DCTimerAI` is a modified version of [DCTimer-BLE](https://github.com/huizhiLLL/DCTimer-BLE). The upstream project already provides speedcubing timing, smart cube connection, Bluetooth timer support, scramble generation, solve history, statistics, and real-time smart cube 3D previews.

This fork mainly adds `MoYu AI / MoYu32` gyroscope and orientation support, so the 3D cube can follow the physical cube rotation. It also unifies the renderer used by the main page and the cube-state dialog, and adds a white center-cap logo system.

## Download

- [GitHub Releases](https://github.com/HrrToT/DCTimerAI/releases/latest)

> DCTimerAI uses a different package name from the original DCTimer, so it will not conflict during installation.  
> The data format remains compatible with the upstream project. You can export data from the original DCTimer / DCTimer-BLE and import it into this version.

## Changes In This Fork

- **Smart cube**: reads `171` orientation packets from `MoYu AI / MoYu32`, maps them into the app's coordinate system, supports white-top / green-front calibration, keeps smart mode active even if the connection dialog is canceled, and places smart-related settings at the top of the settings page.
- **Interaction and logo**: lets smart mode enter the connection flow directly from the main page, and adds a white center-cap logo system with built-in presets, custom uploads, circular crop editing, a three-column scrolling picker, and up to `6` remembered custom slots.
- **3D appearance**: rebuilds the preview into a `cubie`-based renderer, unifies the main page and dialog rendering, narrows the layer gaps, and updates the overall look to better match a modern stickerless cube.

## Upstream Features

- Standard speedcubing timer, WCA inspection timing, solve history, and statistics.
- Scramble generation, scramble import / export, and database import / export.
- Smart cube auto start / stop, scramble progress hints, and deviation correction.
- Real-time 3D smart cube state preview.
- QiYi Smart Timer Bluetooth timer support.
- 8s / 12s voice reminders for WCA inspection mode.

## Supported Devices

The upstream project supports:

- `Moyu32` / `MoYu AI`
- `QYSC` / `Tornado V4`
- `GAN v2 / v3 / v4`
- `QiYi Smart Timer`

This fork currently adds orientation tracking for:

- `MoYu AI / MoYu32`

## Usage

1. Open the project in Android Studio and run it on a physical Android device.
2. Select smart cube timing mode in the app.
3. Scan for and connect a `MoYu AI / MoYu32` smart cube.
4. Open the cube-state dialog, place the cube in a white-top / green-front pose, and tap `Reset orientation`.
5. If needed, open `White center logo` from smart settings to switch built-in logos or upload a custom one.

## Development Environment

- AndroidX
- Android Gradle Plugin 8.9.2
- Gradle 8.11.1
- JDK 17
- `compileSdk / targetSdk` 35

## Main Changed Files

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

## Current Maintenance

- Current maintenance and customization: Hu Tutu
- Technical collaboration: Codex
- Current repository: [HrrToT/DCTimerAI](https://github.com/HrrToT/DCTimerAI)

## Acknowledgements

- [DCTimer-Android](https://github.com/MeigenChou/DCTimer-Android)
- [DCTimer-BLE](https://github.com/huizhiLLL/DCTimer-BLE)
- [cstimer](https://github.com/cs0x7f/cstimer)
- [smartcube-web-bluetooth](https://github.com/poliva/smartcube-web-bluetooth)
- [qiyi_smartcube_protocol](https://codeberg.org/Flying-Toast/qiyi_smartcube_protocol)
- [CubicTimer](https://github.com/hato-ya/CubicTimer)

## License

This project follows the upstream GPLv3 license. Original copyright and attribution should be preserved. The MoYu AI orientation-tracking and 3D appearance changes in this fork are maintained in the current repository.
