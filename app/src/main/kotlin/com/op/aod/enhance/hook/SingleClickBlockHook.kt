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
 * 100% 完整复刻“别他妈亮了”（BTMLL）反编译确认的拦截逻辑，
 * 并在每次调用时动态检查用户配置 [AodConfig.blockSingleClick]，实现即开即用。
 */
internal object SingleClickBlockHook {

    /** 双击间隔阈值（ms），仅用于常规 AOD onClick 判据 */
    private const val DOUBLE_CLICK_THRESHOLD = 350L

    // 常规 AOD / 灵动岛 onClick 目标类
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

    private val DOUBLE_CLICK_LISTENERS = arrayOf(
        "com.oplus.systemui.keyguard.gesture.OplusDoubleClickSleep\$OnDoubleClickListener",
        "com.oplus.systemui.aod.gesture.OplusDoubleClickSleep\$OnDoubleClickListener",
        "com.oplus.systemui.aod.gesture.AodDoubleTapSleepListener",
        "com.oplus.systemui.aod.touch.AodTouchController\$SingleTapListener"
    )

    fun YukiBaseHooker.hookSingleClickWakeUpBlock() {
        // 注意：绝不能在此处做 if (!blockSingleClick) return 早期返回！
        // 否则模块启动时未注册 Hook，后续在界面开启开关将永远无法生效。
        // 必须无条件注册，由各个 Hook 回调在运行时动态判定 blockSingleClick 配置。

        // 1. BTMLL 核心：拦截手势监控注册
        hookPanoramicAodMonitors()

        // 2. BTMLL 核心：拦截 OplusDoubleClickSleep$OnDoubleClickListener.onSingleTapConfirmed
        hookDoubleClickSleepSingleTap()

        // 3. BTMLL 核心：拦截 OplusDoubleClickSleep.processPanoramicWakeup
        hookDoubleClickSleepProcessWakeup()

        // 4. BTMLL 核心：拦截 WakeupScreenHelper.powerOnScreen()
        hookWakeupScreenHelperPowerOn()

        // 5. 常规 AOD onClick 回调拦截（双击 350ms 阈值放行）
        for ((cls, label) in CLICK_TARGETS) {
            registerClickHook(cls, label)
        }
    }

    /**
     * BTMLL 核心 1：拦截 PanoramicAodGestureController 的注册。
     *
     * 对应 BTMLL 反编译逻辑：
     * if (findClassIfExists("PanoramicAodSingleClickWakeUpController") != null) {
     *     hook registerPanoramicAodWakeUpMonitor -> DO_NOTHING
     * } else {
     *     hook registerPanoramicAodGestureMonitor -> DO_NOTHING
     * }
     */
    private fun YukiBaseHooker.hookPanoramicAodMonitors() {
        val singleClickCtrlCls = runCatching {
            "com.oplus.systemui.aod.scene.PanoramicAodSingleClickWakeUpController".toClass(appClassLoader)
        }.getOrNull()

        val gestureCtrlCls = runCatching {
            "com.oplus.systemui.aod.scene.PanoramicAodGestureController".toClass(appClassLoader)
        }.getOrNull() ?: return

        val targetMethodName = if (singleClickCtrlCls != null) {
            "registerPanoramicAodWakeUpMonitor"
        } else {
            "registerPanoramicAodGestureMonitor"
        }

        runCatching {
            gestureCtrlCls.resolve().firstMethod { name = targetMethodName }
        }.getOrNull()?.hook {
            before {
                val cfg = AodConfigReader.read(MainHook.hostAppContext)
                if (cfg.blockSingleClick) {
                    result = null
                    if (BuildConfig.DEBUG) Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: $targetMethodName blocked")
                }
            }
        }
    }

    /**
     * BTMLL 核心 2：拦截手势监听器的 onSingleTapConfirmed 回调，强制返回 false。
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
                            Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: $listenerClass#onSingleTapConfirmed blocked")
                        }
                    }
                }
            }
        }
    }

    /**
     * BTMLL 核心 3：拦截 OplusDoubleClickSleep 中的全景单击唤醒分发。
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
                        val cfg = AodConfigReader.read(MainHook.hostAppContext)
                        if (cfg.blockSingleClick) {
                            result = null
                            if (BuildConfig.DEBUG) Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: $targetClass#$mName blocked")
                        }
                    }
                }
            }
        }
    }

    /**
     * BTMLL 核心 4：精确复刻 BTMLL 对 WakeupScreenHelper.powerOnScreen() 的拦截逻辑。
     *
     * 关键逻辑（来自 BTMLL 字节码 007c if-eqz）：
     * 当 mAodIsInShow 为 FALSE（AOD 还未点亮显示）：通知 OplusWakeUpController 启动显示 AOD（notifyWakeUpCallback(1)）；
     * 当 mAodIsInShow 为 TRUE（AOD 已经显示在屏幕上）：拦截 powerOnScreen() 防止触摸跳转到锁屏！
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
                    val f = aodDataClass.getDeclaredField("sAodData")
                    f.isAccessible = true
                    f.get(null)
                }.getOrNull() ?: return@before

                val isAodEnable = runCatching {
                    val m = sAodData.javaClass.getDeclaredMethod("isAodEnable")
                    m.isAccessible = true
                    m.invoke(sAodData) as? Boolean
                }.getOrNull() ?: false
                if (!isAodEnable) return@before

                val isPanoramicAod = runCatching {
                    val m = sAodData.javaClass.getDeclaredMethod("isPanoramicAod")
                    m.isAccessible = true
                    m.invoke(sAodData) as? Boolean
                }.getOrNull() ?: false
                if (!isPanoramicAod) return@before

                // 核心拦截：阻止亮屏跳锁屏
                result = null
                if (BuildConfig.DEBUG) {
                    Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: WakeupScreenHelper.powerOnScreen blocked")
                }

                // BTMLL 原版反编译逻辑：007c if-eqz v5, target=0082（若 mAodIsInShow 为 FALSE 则通知启动 AOD）
                val mAodIsInShow = runCatching {
                    val f = sAodData.javaClass.getDeclaredField("mAodIsInShow")
                    f.isAccessible = true
                    f.getBoolean(sAodData)
                }.getOrNull() ?: false

                if (!mAodIsInShow) {
                    val wakeUpCtrlCls = runCatching {
                        "com.oplus.systemui.aod.display.OplusWakeUpController".toClass(appClassLoader)
                    }.getOrNull()
                    val ctrlInstance = runCatching {
                        val f = wakeUpCtrlCls?.getDeclaredField("instance")
                        f?.isAccessible = true
                        f?.get(null)
                    }.getOrNull()
                    if (ctrlInstance != null) {
                        val isUpsideDown = runCatching {
                            val f = ctrlInstance.javaClass.getDeclaredField("isUpsideDown")
                            f.isAccessible = true
                            f.getBoolean(ctrlInstance)
                        }.getOrNull() ?: false
                        if (!isUpsideDown) {
                            runCatching {
                                val m = ctrlInstance.javaClass.getDeclaredMethod("notifyWakeUpCallback", Int::class.javaPrimitiveType)
                                m.isAccessible = true
                                m.invoke(ctrlInstance, 1)
                            }
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
                val cfg = AodConfigReader.read(MainHook.hostAppContext)
                if (!cfg.blockSingleClick) return@before

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
