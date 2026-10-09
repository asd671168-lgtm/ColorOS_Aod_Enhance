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
 * 屏蔽 AOD 场景下的单击误触唤醒，仅允许双击唤醒。
 *
 * 覆盖两条触摸事件分发路径：
 *
 * - 路径 A（黑屏手势服务上报）：onClick 回调
 *   通过多个候选类名列表，同时兼容 ColorOS 16/17 及更新版本。
 *   每个目标类维护独立的 [lastBlockedTime]，用 350ms 双击计时判据避免误放行。
 *
 * - 路径 B（AOD 视图触摸事件）：OnDoubleClickListener.onSingleTapConfirmed()
 *   GestureDetector 已确认"不是双击"后的回调，直接拦截即可，无需双击计时。
 *   同时兼容 ColorOS 16/17 的不同内部类名。
 *
 * ColorOS 17 变更说明：
 * ColorOS 17 将 AOD 架构迁移到新的 Aqua Dynamics / Lockscreen Island 框架，
 * 部分内部类被重命名或迁移到新包下。本 Hook 通过维护候选类名列表，
 * 对每个版本的候选类逐一尝试注册，注册失败时静默跳过（runCatching）。
 */
internal object SingleClickBlockHook {

    /** 双击间隔阈值（ms）。 */
    private const val DOUBLE_CLICK_THRESHOLD = 350L

    // 路径 A：onClick 回调目标类列表
    // 每组 Pair：(类全名, 日志标签)
    // ColorOS 16 类名 + ColorOS 17 候选类名（Aqua Dynamics/Lockscreen Island 架构）
    private val CLICK_TARGETS = arrayOf(
        // ── ColorOS ≤ 16 ──────────────────────────────────────────────────────
        "com.oplus.systemui.aod.scene.AodViewSingleClickWakeUpHolder\$AodSingleClickWakeUpCallback" to "NormalAod_v16",
        "com.oplus.systemui.aod.scene.PanoramicAodSingleClickWakeUpController\$PanoramicAodSingleClickWakeUpCallback" to "PanoramicAod_v16",
        "com.oplus.systemui.aod.display.OplusWakeUpController\$AodSingleClickWakeUpCallback" to "WakeUpController_v16",

        // ── ColorOS 17 / Aqua Dynamics 架构候选 ──────────────────────────────
        // 统一的 SingleClickWakeUp 控制器（新架构合并了多个旧回调类）
        "com.oplus.systemui.aod.scene.AodSingleClickWakeUpController\$AodSingleClickCallback" to "AodClick_v17",
        "com.oplus.systemui.aod.scene.AodSingleClickWakeUpController\$SingleClickCallback" to "AodClick_v17b",
        // Lockscreen Island / 新 WakeUpManager
        "com.oplus.systemui.lockscreen.island.AodIslandSingleClickCallback" to "IslandClick_v17",
        "com.oplus.systemui.aod.wakeup.AodWakeUpManager\$SingleClickCallback" to "WakeUpMgr_v17",
        "com.oplus.systemui.aod.wakeup.SingleClickWakeUpCallback" to "SingleClickCb_v17",
        // 全景 AOD 新版路径
        "com.oplus.systemui.aod.panoramic.PanoramicSingleClickCallback" to "PanoClick_v17",
    )

    // 路径 B：双击监听器内部类候选列表
    private val DOUBLE_CLICK_LISTENERS = arrayOf(
        // ColorOS 16
        "com.oplus.systemui.keyguard.gesture.OplusDoubleClickSleep\$OnDoubleClickListener",
        // ColorOS 17 候选
        "com.oplus.systemui.aod.gesture.OplusDoubleClickSleep\$OnDoubleClickListener",
        "com.oplus.systemui.aod.gesture.AodDoubleTapSleepListener",
        "com.oplus.systemui.aod.touch.AodTouchController\$SingleTapListener",
    )

    // 路径 C：BTMLL (别他妈亮了) 验证有效的高层控制器直接拦截
    private val BTMLL_NOTIFY_TARGETS = arrayOf(
        "com.oplus.systemui.aod.display.OplusWakeUpController",
        "com.oplus.systemui.aod.scene.PanoramicAodSingleClickWakeUpController",
        "com.oplus.systemui.aod.scene.AodSingleClickWakeUpController",
        "com.oplus.systemui.notification.interruption.wakeup.WakeupScreenHelper"
    )

    private val BTMLL_PROCESS_TARGETS = arrayOf(
        "com.oplus.systemui.keyguard.gesture.OplusDoubleClickSleep",
        "com.oplus.systemui.aod.gesture.OplusDoubleClickSleep"
    )

    fun YukiBaseHooker.hookSingleClickWakeUpBlock() {
        if (!AodConfigReader.read(MainHook.hostAppContext).blockSingleClick) {
            if (BuildConfig.DEBUG) Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: disabled by config")
            return
        }

        // 路径 A：遍历所有候选 onClick 目标类
        for ((cls, label) in CLICK_TARGETS) {
            registerClickHook(cls, label)
        }

        // 路径 B：遍历所有候选双击监听器类
        hookDoubleClickSleepSingleTap()

        // 路径 C：BTMLL 同款高层 Controller 拦截
        hookBtmllTargets()
    }

    /**
     * Hook 所有候选双击监听器的 onSingleTapConfirmed(MotionEvent)。
     *
     * 覆盖路径 B：AOD 视图触摸事件分发链。
     * - 双击 → onDoubleTap() → wakeUp / goToSleep（不 Hook，确保双击正常）
     * - 单击确认 → onSingleTapConfirmed() → processPanoramicWakeup() → wakeUp
     *
     * onSingleTapConfirmed 是 GestureDetector 在确认"这不是双击"之后才调用的，
     * 因此直接 result = false 即可，无需像 onClick Hook 那样做双击计时。
     */
    private fun YukiBaseHooker.hookDoubleClickSleepSingleTap() {
        for (listenerClass in DOUBLE_CLICK_LISTENERS) {
            runCatching {
                listenerClass
                    .toClass(appClassLoader)
                    .resolve()
                    .firstMethod { name = "onSingleTapConfirmed" }
                    .hook {
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
                if (BuildConfig.DEBUG) {
                    Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: Hooked listener: $listenerClass")
                }
            }.onFailure {
                if (BuildConfig.DEBUG) {
                    Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: $listenerClass not available (${it.javaClass.simpleName})")
                }
            }
        }
    }

    /**
     * 为单个目标类注册 onClick 拦截 Hook。
     *
     * 每个目标拥有独立的 [lastBlockedTime]，避免不同手势区域之间
     * 的单击/双击状态互相干扰（如 A 区域单击后，B 区域在 350ms 内
     * 被触发不会错误放行）。
     *
     * 注册失败时静默跳过（runCatching），确保某个版本不存在的类不影响其他 Hook。
     */
    private fun YukiBaseHooker.registerClickHook(targetClass: String, label: String) {
        val lastBlockedTime = AtomicLong(0L)
        runCatching {
            targetClass
                .toClass(appClassLoader)
                .resolve()
                .firstMethod { name = "onClick" }
                .hook {
                    before {
                        val now = System.currentTimeMillis()
                        val prev = lastBlockedTime.get()

                        if (prev != 0L && now - prev < DOUBLE_CLICK_THRESHOLD) {
                            // 放行：快速第二次点击（双击）
                            lastBlockedTime.set(0L)
                            if (BuildConfig.DEBUG) {
                                Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: $label allowed (double-click)")
                            }
                            return@before
                        }

                        // 拦截：首次单击或慢速重试
                        lastBlockedTime.set(now)
                        result = null
                        if (BuildConfig.DEBUG) {
                            Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: $label blocked")
                        }
                    }
                }
            if (BuildConfig.DEBUG) {
                Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: Hooked $label ($targetClass)")
            }
        }.onFailure {
            if (BuildConfig.DEBUG) {
                Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: $label not available (${it.javaClass.simpleName})")
            }
        }
    }

    /**
     * 路径 C：直接拦截 BTMLL (别他妈亮了) 验证过的核心唤醒方法。
     * 直接丢弃 notifyWakeUpCallback 和 processPanoramicWakeup。
     */
    private fun YukiBaseHooker.hookBtmllTargets() {
        for (targetClass in BTMLL_NOTIFY_TARGETS) {
            runCatching {
                targetClass.toClass(appClassLoader).resolve()
                    .firstMethod { name = "notifyWakeUpCallback" }
                    .hook {
                        before {
                            if (AodConfigReader.read(MainHook.hostAppContext).blockSingleClick) {
                                result = null
                                if (BuildConfig.DEBUG) Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: Blocked notifyWakeUpCallback in $targetClass")
                            }
                        }
                    }
            }.onFailure {
                if (BuildConfig.DEBUG) Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: notifyWakeUpCallback not found in $targetClass")
            }
        }

        for (targetClass in BTMLL_PROCESS_TARGETS) {
            runCatching {
                targetClass.toClass(appClassLoader).resolve()
                    .firstMethod { name = "processPanoramicWakeup" }
                    .hook {
                        before {
                            if (AodConfigReader.read(MainHook.hostAppContext).blockSingleClick) {
                                result = null
                                if (BuildConfig.DEBUG) Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: Blocked processPanoramicWakeup in $targetClass")
                            }
                        }
                    }
            }.onFailure {
                if (BuildConfig.DEBUG) Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: processPanoramicWakeup not found in $targetClass")
            }
        }
    }
}
