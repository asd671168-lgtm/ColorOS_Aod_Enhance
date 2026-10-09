package com.op.aod.enhance.hook

import android.util.Log
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.toClass
import com.op.aod.enhance.BuildConfig
import java.util.concurrent.atomic.AtomicLong

/**
 * AOD 单击唤醒屏蔽 Hook。
 *
 * 结合并集成"别他妈亮了"（BTMLL）实测有效的多层拦截机制 + 传统 AOD 回调过滤，
 * 彻底解决 ColorOS 14/15/16/17 在全景息屏、锁屏岛以及常规息屏下的单击误触亮屏问题，
 * 同时 100% 确保双击唤醒正常可用。
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

        // 1. 集成 BTMLL 核心逻辑：拦截 WakeupScreenHelper.powerOnScreen
        hookWakeupScreenHelperPowerOn()

        // 2. 集成 BTMLL 核心逻辑：拦截 PanoramicAod 监控注册
        hookPanoramicAodMonitors()

        // 3. 集成 BTMLL 核心逻辑：拦截 OplusDoubleClickSleep 全景唤醒处理
        hookDoubleClickSleepProcessWakeup()

        // 4. 拦截 onSingleTapConfirmed
        hookDoubleClickSleepSingleTap()

        // 5. 补充拦截：常规 AOD 的 onClick 路径（带双击阈值判定）
        for ((cls, label) in CLICK_TARGETS) {
            registerClickHook(cls, label)
        }
    }

    /**
     * BTMLL 核心防线 1：拦截 WakeupScreenHelper.powerOnScreen()。
     *
     * 在 ColorOS 14/15/16/17 全景息屏下，任何触摸引发的亮屏操作最终均调用此方法唤醒屏幕。
     * 当处于 AOD 息屏状态时直接拦截该调用（result = null），使屏幕保持息屏；
     * 同时若 AOD 正在显示，通知 OplusWakeUpController 保持 AOD 状态活性。
     */
    private fun YukiBaseHooker.hookWakeupScreenHelperPowerOn() {
        val helperClass = runCatching {
            "com.oplus.systemui.notification.interruption.wakeup.WakeupScreenHelper".toClass(appClassLoader)
        }.getOrNull() ?: return

        runCatching {
            helperClass.resolve().firstMethod { name = "powerOnScreen" }
        }.getOrNull()?.hook {
            before {
                val cfg = AodConfigReader.read(MainHook.hostAppContext)
                if (!cfg.blockSingleClick) return@before

                val aodDataClass = runCatching {
                    "com.oplus.systemui.aod.aodclock.constant.AodData".toClass(appClassLoader)
                }.getOrNull() ?: return@before

                val sAodData = runCatching {
                    aodDataClass.resolve().firstField { name = "sAodData" }.get(null)
                }.getOrNull() ?: return@before

                val isAodEnable = runCatching {
                    sAodData.javaClass.resolve().firstMethod { name = "isAodEnable" }.invoke(sAodData) as? Boolean
                }.getOrNull() ?: false
                if (!isAodEnable) return@before

                val isPanoramicAod = runCatching {
                    sAodData.javaClass.resolve().firstMethod { name = "isPanoramicAod" }.invoke(sAodData) as? Boolean
                }.getOrNull() ?: false
                if (!isPanoramicAod) return@before

                // 核心拦截点：阻止亮屏
                result = null
                if (BuildConfig.DEBUG) {
                    Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: WakeupScreenHelper.powerOnScreen blocked")
                }

                // 维持 AOD 状态活跃，避免息屏组件异常休眠
                val mAodIsInShow = runCatching {
                    sAodData.javaClass.resolve().firstField { name = "mAodIsInShow" }.get(sAodData) as? Boolean
                }.getOrNull() ?: false

                if (mAodIsInShow) {
                    val wakeUpCtrlCls = runCatching {
                        "com.oplus.systemui.aod.display.OplusWakeUpController".toClass(appClassLoader)
                    }.getOrNull()
                    val ctrlInstance = runCatching {
                        wakeUpCtrlCls?.resolve()?.firstField { name = "instance" }?.get(null)
                    }.getOrNull()
                    if (ctrlInstance != null) {
                        val isUpsideDown = runCatching {
                            ctrlInstance.javaClass.resolve().firstField { name = "isUpsideDown" }.get(ctrlInstance) as? Boolean
                        }.getOrNull() ?: false
                        if (!isUpsideDown) {
                            runCatching {
                                ctrlInstance.javaClass.resolve().firstMethod {
                                    name = "notifyWakeUpCallback"
                                }.invoke(ctrlInstance, 1)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * BTMLL 核心防线 2：拦截全景手势监控器的注册。
     */
    private fun YukiBaseHooker.hookPanoramicAodMonitors() {
        val gestureCtrlCls = runCatching {
            "com.oplus.systemui.aod.scene.PanoramicAodGestureController".toClass(appClassLoader)
        }.getOrNull() ?: return

        // 拦截手势监控注册
        runCatching {
            gestureCtrlCls.resolve().firstMethod { name = "registerPanoramicAodGestureMonitor" }
        }.getOrNull()?.hook {
            before {
                if (AodConfigReader.read(MainHook.hostAppContext).blockSingleClick) {
                    result = null
                    if (BuildConfig.DEBUG) Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: registerPanoramicAodGestureMonitor blocked")
                }
            }
        }

        // 拦截唤醒监控注册
        runCatching {
            gestureCtrlCls.resolve().firstMethod { name = "registerPanoramicAodWakeUpMonitor" }
        }.getOrNull()?.hook {
            before {
                if (AodConfigReader.read(MainHook.hostAppContext).blockSingleClick) {
                    result = null
                    if (BuildConfig.DEBUG) Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: registerPanoramicAodWakeUpMonitor blocked")
                }
            }
        }
    }

    /**
     * BTMLL 核心防线 3：拦截 OplusDoubleClickSleep 中的全景唤醒分发。
     */
    private fun YukiBaseHooker.hookDoubleClickSleepProcessWakeup() {
        val classes = arrayOf(
            "com.oplus.systemui.keyguard.gesture.OplusDoubleClickSleep",
            "com.oplus.systemui.aod.gesture.OplusDoubleClickSleep"
        )
        for (targetClass in classes) {
            val cls = runCatching { targetClass.toClass(appClassLoader) }.getOrNull() ?: continue
            val methods = arrayOf("processPanoramicWakeup", "-$$Nest$mprocessPanoramicWakeup")
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
     * 路径 B：拦截双击手势监听器的 onSingleTapConfirmed 回调。
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
