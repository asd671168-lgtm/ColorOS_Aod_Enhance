package com.op.aod.enhance.hook

import android.util.Log
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.toClass
import com.op.aod.enhance.BuildConfig
import java.util.concurrent.atomic.AtomicLong

/**
 * AOD 单击唤醒屏蔽 Hook（安全模式）。
 *
 * 专注拦截手势单次点击分发与回调，绝不拦截全局电源/亮屏入口（如 powerOnScreen），
 * 确保电源键点亮、双击亮屏、指纹解锁 100% 正常，杜绝黑屏问题。
 */
internal object SingleClickBlockHook {

    /** 双击间隔阈值（ms），仅用于常规 AOD 的 onClick 区分 */
    private const val DOUBLE_CLICK_THRESHOLD = 350L

    // ── 路径 A：常规 AOD / 灵动岛 onClick 目标类 ─────────────────────────────
    private val CLICK_TARGETS = arrayOf(
        "com.oplus.systemui.aod.scene.AodViewSingleClickWakeUpHolder\$AodSingleClickWakeUpCallback" to "NormalAod_v16",
        "com.oplus.systemui.aod.scene.PanoramicAodSingleClickWakeUpController\$PanoramicAodSingleClickWakeUpCallback" to "PanoramicAod_v16",
        "com.oplus.systemui.aod.display.OplusWakeUpController\$AodSingleClickWakeUpCallback" to "WakeUpController_v16",
        "com.oplus.systemui.aod.scene.AodSingleClickWakeUpController\$AodSingleClickCallback" to "AodClick_v17",
        "com.oplus.systemui.aod.scene.AodSingleClickWakeUpController\$SingleClickCallback" to "AodClick_v17b",
        "com.oplus.systemui.lockscreen.island.AodIslandSingleClickCallback" to "IslandClick_v17",
        "com.oplus.systemui.aod.wakeup.AodWakeUpManager\$SingleClickCallback" to "WakeUpMgr_v17",
        "com.oplus.systemui.aod.wakeup.SingleClickWakeUpCallback" to "SingleClickCb_v17",
        "com.oplus.systemui.aod.panoramic.PanoramicSingleClickCallback" to "PanoClick_v17"
    )

    // ── 路径 B：手势监听器类 ──────────────────────────────────────────────────
    private val DOUBLE_CLICK_LISTENERS = arrayOf(
        "com.oplus.systemui.keyguard.gesture.OplusDoubleClickSleep\$OnDoubleClickListener",
        "com.oplus.systemui.aod.gesture.OplusDoubleClickSleep\$OnDoubleClickListener",
        "com.oplus.systemui.aod.gesture.AodDoubleTapSleepListener",
        "com.oplus.systemui.aod.touch.AodTouchController\$SingleTapListener"
    )

    fun YukiBaseHooker.hookSingleClickWakeUpBlock() {
        if (!AodConfigReader.read(MainHook.hostAppContext).blockSingleClick) {
            if (BuildConfig.DEBUG) Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: disabled by config")
            return
        }

        // 1. 拦截手势单次点击确认回调（GestureDetector 确认是单击而非双击时调用）
        hookDoubleClickSleepSingleTap()

        // 2. 拦截全景息屏点击分发处理（只拦截 processPanoramicWakeup，不触碰双击/电源键）
        hookDoubleClickSleepProcessWakeup()

        // 3. 常规 AOD 的 onClick 路径（带 350ms 双击计时判定放行）
        for ((cls, label) in CLICK_TARGETS) {
            registerClickHook(cls, label)
        }
    }

    /**
     * 核心防线 1：拦截 OplusDoubleClickSleep 中的全景单击唤醒分发。
     * 仅阻止 processPanoramicWakeup，不影响双击 onDoubleTap 或电源键。
     */
    private fun YukiBaseHooker.hookDoubleClickSleepProcessWakeup() {
        val classes = arrayOf(
            "com.oplus.systemui.keyguard.gesture.OplusDoubleClickSleep",
            "com.oplus.systemui.aod.gesture.OplusDoubleClickSleep"
        )
        for (targetClass in classes) {
            val cls = runCatching { targetClass.toClass(appClassLoader) }.getOrNull() ?: continue
            val methods = arrayOf("processPanoramicWakeup", "-${'$'}Nest${'$'}mprocessPanoramicWakeup")
            for (mName in methods) {
                runCatching {
                    cls.resolve().firstMethod { name = mName }
                }.getOrNull()?.hook {
                    before {
                        if (AodConfigReader.read(MainHook.hostAppContext).blockSingleClick) {
                            result = null
                            if (BuildConfig.DEBUG) Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: $targetClass#$mName blocked")
                        }
                    }
                }
            }
        }
    }

    /**
     * 核心防线 2：拦截手势监听器的 onSingleTapConfirmed 回调。
     * 返回 false 明确告知系统取消单击响应。
     */
    private fun YukiBaseHooker.hookDoubleClickSleepSingleTap() {
        for (listenerClass in DOUBLE_CLICK_LISTENERS) {
            val cls = runCatching { listenerClass.toClass(appClassLoader) }.getOrNull() ?: continue
            runCatching {
                cls.resolve().firstMethod { name = "onSingleTapConfirmed" }
            }.getOrNull()?.hook {
                before {
                    val cfg = AodConfigReader.read(MainHook.hostAppContext)
                    if (cfg.blockSingleClick) {
                        result = false
                        if (BuildConfig.DEBUG) {
                            Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: $listenerClass blocked (view touch path)")
                        }
                    }
                }
            }
        }
    }

    /**
     * 路径 A：常规 AOD 的 onClick 拦截（保留 350ms 判定供快速双击放行）。
     */
    private fun YukiBaseHooker.registerClickHook(targetClass: String, label: String) {
        val lastBlockedTime = AtomicLong(0L)
        val cls = runCatching { targetClass.toClass(appClassLoader) }.getOrNull() ?: return
        runCatching {
            cls.resolve().firstMethod { name = "onClick" }
        }.getOrNull()?.hook {
            before {
                val now = System.currentTimeMillis()
                val prev = lastBlockedTime.get()

                if (prev != 0L && now - prev < DOUBLE_CLICK_THRESHOLD) {
                    lastBlockedTime.set(0L)
                    if (BuildConfig.DEBUG) Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: $label allowed (double-click)")
                    return@before
                }

                lastBlockedTime.set(now)
                result = null
                if (BuildConfig.DEBUG) Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: $label blocked")
            }
        }
    }
}
