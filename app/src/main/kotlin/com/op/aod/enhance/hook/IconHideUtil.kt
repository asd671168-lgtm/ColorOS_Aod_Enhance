package com.op.aod.enhance.hook

import android.content.ComponentName
import android.content.pm.PackageManager
import android.util.Log
import com.op.aod.enhance.BuildConfig

/**
 * 应用图标隐藏工具。
 *
 * 通过切换 LAUNCHER 主 Activity 的 PackageManager 组件启用状态，
 * 实现隐藏/显示桌面图标，而不卸载应用。
 *
 * - 隐藏：将 MainActivity 组件设为 COMPONENT_ENABLED_STATE_DISABLED，
 *   启动器将不再显示该图标。
 * - 显示：将 MainActivity 组件恢复为 COMPONENT_ENABLED_STATE_ENABLED。
 *
 * 注意：隐藏图标后，模块仍可通过 LSPosed Manager 中的模块管理页面访问。
 */
object IconHideUtil {

    private const val TAG = "AOD_Enhance_IconHide"

    /**
     * 设置应用图标可见性。
     *
     * @param packageManager 应用的 PackageManager 实例
     * @param packageName 应用包名
     * @param mainActivityClass MainActivity 的全限定类名
     * @param hidden true = 隐藏图标，false = 显示图标
     */
    fun setIconHidden(
        packageManager: PackageManager,
        packageName: String,
        mainActivityClass: String,
        hidden: Boolean
    ) {
        val component = ComponentName(packageName, mainActivityClass)
        val newState = if (hidden) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }

        runCatching {
            packageManager.setComponentEnabledSetting(
                component,
                newState,
                PackageManager.DONT_KILL_APP
            )
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Icon visibility set: hidden=$hidden for $mainActivityClass")
            }
        }.onFailure {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to set icon visibility: ${it.message}")
            }
        }
    }

    /**
     * 查询当前图标是否被隐藏。
     *
     * @return true = 图标已隐藏
     */
    fun isIconHidden(
        packageManager: PackageManager,
        packageName: String,
        mainActivityClass: String
    ): Boolean {
        val component = ComponentName(packageName, mainActivityClass)
        return runCatching {
            val state = packageManager.getComponentEnabledSetting(component)
            state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }.getOrDefault(false)
    }
}
