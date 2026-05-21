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

Thanks to [huizhiLLL](https://github.com/huizhiLLL), the author of [DCTimer-BLE](https://github.com/huizhiLLL/DCTimer-BLE), for the pioneering work on 3x3 smart cube Bluetooth connection and MAC-free quick connection, which laid the foundation for this project.

---

## About

`DCTimerAI` is a modified version of [DCTimer-BLE](https://github.com/huizhiLLL/DCTimer-BLE). The upstream project already supports smart cube connectivity.

This fork adds `MoYu AI / MoYu32` gyroscope and orientation data support, solve replay, and import from other timers.

## Download

- [GitHub Releases](https://github.com/HrrToT/DCTimerAI/releases/latest)

> DCTimerAI uses a different package name from the original DCTimer, so it will not conflict during installation.  
> Since v2.2.7, it also uses a different package name from DCTimer-BLE, so it will not conflict during installation.

## Updates In This Fork

### v2.2.7
- **Result sorting**: Changes the old "default (newest last) / newest first" to sort by completion date, corresponding to "Date (new to old) / Date (old to new)". Empty dates are treated as the earliest entries.
- **Import / Export**
  - Adds a dedicated "Import / Export" section in the drawer for backups, database migration, and external timer imports.
  - **One-step data export**: Supports one-tap export of the database, current settings, background images, and smart cube logo assets.
  - **Import from other timers**: Supports **CSTimer** and **Twisty Timer**, parsing 3x3 solves only. Options include "New session / Append to existing session / Replace existing session", preserving solve time, penalty, scramble, and original date.
  - **Legacy database import / export**: Continues to support compatibility with the original DCTimer for migrating historical solves.
- **Solve reconstruction and replay**
  - The "Solve" detail page and replay now share the same `solve_meta / displaySteps` data source, keeping CFOP phase splits consistent.
  - **EMS display**: Supports compressing adjacent outer-layer move pairs within the same CFOP phase into `E / M / S` notation for display, counted as a single step in replay statistics.
  - **Replay UI**: Reorganized replay layout, reuses the main screen background, compresses sparkline and button area to give the 3D cube more display space.
### v2.2.4-v2.2.6
- **Smart cube**: Supports `171` orientation packet reading and coordinate mapping for `MoYu AI / MoYu32`, with white-top / green-front one-tap calibration. Double-tap on the main screen quickly resets the view, smart mode is retained after canceling the connection dialog, and smart-related settings are placed at the top of the settings page.
  - **Interaction**: Smart mode is set as the default timing method, and the connection flow can be entered directly from the main screen.
  - **Logo**: Adds a white center-cap logo system with built-in presets, custom uploads, and remembered slots.
- **3D appearance**: Rebuilds the sticker-style preview into a `cubie`-level solid-color renderer, unifies the main screen and state dialog display, narrows inter-layer gaps, and updates corner radii and colors to better match a modern stickerless cube.

## Upstream BLE Version Features

- Standard speedcubing timer, inspection timing, solve history and statistics.
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

## Development Environment

- AndroidX
- Android Gradle Plugin 8.9.2
- Gradle 8.11.1
- JDK 17
- `compileSdk / targetSdk` 35

## Main Changed Files

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

## Current Maintenance

- Current maintenance and customization: Hu Tutu
- Technical collaboration: Codex, Claude Code
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
