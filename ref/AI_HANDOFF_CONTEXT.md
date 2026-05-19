# DCTimerAI 项目交接背景

这份文档是给接手本项目的其他 AI / 自动化代理准备的快速背景说明，重点不是重复 README，而是补充“继续改代码时必须知道的上下文”。

---

## 1. 项目定位

- 项目名：`DCTimerAI`
- 基础仓库：`DCTimer-BLE`
- 当前主要维护仓库：`HrrToT/DCTimerAI`
- 技术栈：原生 Android Java 项目，AndroidX，Gradle 8.11.1，JDK 17，`compileSdk / targetSdk = 35`

这个分支不是从零做的新 App，而是在上游 `DCTimer-BLE` 基础上持续改出来的智能魔方增强版。  
当前工作的核心方向有三条：

1. `MoYu AI / MoYu32` 的姿态 / 陀螺仪接入
2. 智能魔方相关交互重做
3. 3D 魔方渲染与外观重构

---

## 2. 这个分支已经做过什么

### 2.1 智能魔方姿态

- 已接入 `MoYu AI / MoYu32` 的 `171` 姿态数据包
- 已完成姿态数据到四元数 / 屏幕坐标系的映射
- 已支持 `白顶绿前` 作为标准姿态参考
- 已支持在状态弹窗中 `重置姿态`
- 主界面右下角 3D 魔方支持双击快速重置视角

### 2.2 智能模式交互

- 智能模式现在是默认的重要模式，不再是附属入口
- 如果用户切到智能魔方计时模式，即使取消蓝牙连接弹窗，也保留在智能模式
- 主界面计时区域 / 右下角 3D 魔方都可作为连接入口
- 智能设置已经被移动到设置页第一组

### 2.3 3D 魔方渲染

- 已经从原先偏“贴纸式”的渲染，改成 `cubie` 级立体渲染
- 主界面与状态弹窗共用同一套渲染逻辑
- 当前外观目标偏向现代实色魔方：
  - 白色塑料主体
  - 彩色实色面
  - 同色深边
  - 更大的中心块圆角
  - 更窄的层间缝隙

### 2.4 白色中心块 Logo

- 已支持内置 Logo、自定义上传、圆形裁剪
- Logo 选择器已改为三列滚动网格
- 每个选项显示“圆形预览 + 名称”
- 已支持最多 `6` 个自定义 Logo 槽位
- `resetAll()` 已经会清理：
  - logo 偏好项
  - 自定义 logo 文件
  - 自定义槽位游标
- 已为 Logo 选择器加了轻量 bitmap 缓存：
  - 内置 Logo 常驻缓存
  - 自定义 Logo 按路径 + `lastModified` 做预览缓存

---

## 3. 当前重要设计约定

### 3.1 姿态基准

- 所有关于“默认方向”“重置姿态”“朝向描述”的讨论，都以 `白顶绿前` 为标准基准
- 当前白色中心块 Logo 的默认朝向已经调整为：
  - `Logo 顶部朝蓝色`
  - `Logo 底部朝绿色`

### 3.2 Logo 行为约定

- “无 Logo” 作为正式选项存在
- “上传 Logo” 作为正式选项存在
- 自定义 Logo 槽位固定命名：
  - `自定义1`
  - `自定义2`
  - ...
  - `自定义6`
- 超过 6 个时按固定槽位轮转覆盖
- 裁剪完成后自动应用刚上传的新 Logo

### 3.3 设置页约定

- 智能设置是设置页第一组，不只是视觉置顶，而是 section 构建顺序本身就在第一位
- `MainActivity` 中设置页 section 组装逻辑已经拆成多个私有方法，继续加设置项时优先沿用这种结构，不要再把大量 `Utils.addSection(...)` 直接堆回 `onCreate()`

---

## 4. 最关键的代码位置

如果要继续改功能，优先看这些文件：

### 智能模式 / 设置 / 主流程

- `app/src/main/java/com/dctimer/activity/MainActivity.java`

它目前承担的职责很多，包括：

- 设置页组装
- 智能模式入口逻辑
- 蓝牙连接流程
- Logo 选择器入口
- 计时模式切换
- 3D 魔方主界面显示逻辑

如果改交互，大概率要先看这里。

### MoYu 姿态协议

- `app/src/main/java/com/dctimer/util/Moyu32CubeProtocol.java`

如果要继续改 MoYu 数据解析、姿态包处理或扩展其他设备的姿态输入，先从这里入手。

### 3D 魔方渲染

- `app/src/main/java/com/dctimer/view/SmartCube3DView.java`

这里负责：

- cubie 级渲染
- 旋转动画
- 主视角交互
- 白色中心块 Logo 贴图

如果改：

- 外观
- Logo 大小
- Logo 朝向
- 立体效果
- 触控旋转

基本都在这里。

### 2D 打乱图 / 颜色风格

- `app/src/main/java/com/dctimer/view/SmartCubeImageView.java`
- `app/src/main/java/com/dctimer/util/Utils.java`

### 状态弹窗

- `app/src/main/java/com/dctimer/dialog/CubeStateDialog.java`
- `app/src/main/res/layout/dialog_cube_state.xml`

### Logo 存储与裁剪

- `app/src/main/java/com/dctimer/util/SmartCubeLogoProvider.java`
- `app/src/main/java/com/dctimer/activity/SmartCubeLogoCropActivity.java`
- `app/src/main/java/com/dctimer/view/SmartCubeLogoCropView.java`
- `app/src/main/res/layout/dialog_smart_cube_logo_picker.xml`
- `app/src/main/res/layout/item_smart_cube_logo_option.xml`

---

## 5. 当前已知状态

### 已经收口的点

- 智能设置已经真正放到第一组
- Logo 镜像问题已经修正
- Logo 默认朝向已经按 `顶部朝蓝色 / 底部朝绿色` 调整
- `resetAll()` 对 Logo 数据清理不完整的问题已经修复
- Logo 选择器已经做了轻量缓存
- `MainActivity` 的设置组装逻辑已经完成初步拆分

### 还要注意的现实情况

- 当前本地构建验证受 Android SDK / JDK / license 环境影响，不代表所有修改都已做过完整 release 构建回归
- 项目是持续演进的分支，不是一次性整理完的干净重写版本，所以部分逻辑仍集中在 `MainActivity`
- 如果继续扩展智能模式，优先保持“沿现有结构补充”，不要顺手做大范围重构，除非确实必要

---

## 6. 给后续 AI 的建议

1. 先确认任务属于哪一类：
   - 姿态 / 协议
   - 智能模式交互
   - 3D 外观
   - Logo 系统
   - 设置页组织

2. 如果改设置页：
   - 优先修改 `buildSettingSections(...)` 相关方法
   - 不要直接把逻辑重新堆回 `onCreate()`

3. 如果改 Logo：
   - 先看 `SmartCubeLogoProvider`
   - 再看 `MainActivity.buildSmartCubeLogoOptions()`
   - 最后看 `SmartCube3DView.drawTexturedQuad()`

4. 如果改 3D 外观：
   - 优先保留当前“cubie 级 + 主界面 / 弹窗共用”的方向
   - 避免退回成平面贴纸式做法

5. 如果改 README：
   - 当前偏向“项目主页介绍风格”
   - 用户要求：简洁、不要把同一个更新点拆太多 bullets

---

## 7. 这份文档的用途

这份文档默认不进 Git（`/ref/` 已在 `.gitignore` 中），主要用于：

- 给其他 AI / 代理交接上下文
- 记录当前分支的隐含约定
- 减少重复读整仓库才能上手的成本

如果后续又做了新的大改动，建议继续在这里补充，而不是把所有背景都塞进 README。
