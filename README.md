# ColorOS AOD Enhance (v1.6 / ColorOS 17 支持版)

![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)

ColorOS 息屏显示（AOD）增强模块（Xposed / LSPosed）。提供全新**液态玻璃 (Liquid Glass)** 可视化配置界面，修改自动保存。

## 功能

**亮度调节**
- 熄屏前暗光环境初始 AOD 亮度
- 熄屏前亮光环境初始 AOD 亮度
- 熄屏时 AOD 自动亮度倍率（1.0～2.0）

**功能开关**
- 全天全景 AOD 支持（系统界面能力解锁）
- 息屏全景 AOD 显示设置（息屏设置开关常显）
- **AOD 单击唤醒屏蔽**（全面适配 ColorOS 15 / 16 / 17，彻底拦截误触唤醒，仅放行双击）
- 低光环境保持 AOD 显示（阻止极暗与夜间超时关闭 AOD）
- **隐藏桌面图标**（支持在应用内一键隐藏桌面启动图标，隐藏后仍可在 LSPosed 模块列表中直接打开）

**界面设计**
- **液态玻璃 (Liquid Glass)** 风格重构：全原生 Jetpack Compose 渲染的磨砂毛玻璃多层光泽、内发光、液态发光 Switch 开关与滑块组件。

## 使用

1. 安装 APK，在 Xposed/LSPosed 中激活模块
2. 勾选目标作用域：`系统界面（com.android.systemui）`、`息屏（com.oplus.aod）`
3. 重启系统界面或设备后生效
4. 打开模块 App 配置各项参数，修改后自动保存

> ⚠️ 提示：
> - 隐藏桌面图标后，若需重新进入配置界面，请在 **LSPosed Manager** 中点击本模块卡片即可打开。
> - 单击唤醒屏蔽与全景支持修改后，建议重启系统界面（SystemUI）以确保完整生效。

## 技术栈

| 组件 | 用途 |
|---|---|
| YukiHookAPI + KavaRef | Xposed Hook 框架 |
| Jetpack Compose + Liquid Glass Components | 原生安卓液态玻璃组件与现代化 UI |
| ContentProvider + SharedPreferences | 配置跨进程存储（带 CAS 缓存与 TTL） |
| AGP 9.2.1 / Kotlin 2.4.0 / Gradle 9.5.1 | 构建系统 |

## 更新日志

### v1.6 — ColorOS 17 适配与液态玻璃 UI
- **修复 ColorOS 17 单击唤醒屏蔽失效**：
  - 适配 ColorOS 17 全新 Aqua Dynamics / Lockscreen Island 架构重构后的 AOD 事件分发链；
  - 增加多组候选 Hook 拦截点（`AodSingleClickWakeUpController`、`AodIslandSingleClickCallback`、`AodWakeUpManager` 等）；
  - 兼容 ColorOS 15/16/17 双击唤醒 350ms 计时放行与 `onSingleTapConfirmed` 直接拦截。
- **新增隐藏桌面图标功能**：
  - 支持在设置页面一键隐藏启动器桌面图标，通过 `PackageManager` 组件动态开关实现，不影响模块后台与 LSPosed 打开。
- **全 UI 升级为液态玻璃 (Liquid Glass) 组件**：
  - 采用流体光晕背景（Fluid Aura）、多层半透明高光磨砂卡片（`LiquidGlassCard`）、发光液态开关（`GlassSwitch`）及玻璃滑块。
