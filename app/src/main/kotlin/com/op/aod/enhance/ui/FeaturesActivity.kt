package com.op.aod.enhance.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.op.aod.enhance.data.AodConfigStore
import com.op.aod.enhance.data.AodUiConfig
import com.op.aod.enhance.hook.IconHideUtil
import com.op.aod.enhance.ui.glass.GlassDivider
import com.op.aod.enhance.ui.glass.GlassScaffold
import com.op.aod.enhance.ui.glass.GlassSwitchRow
import com.op.aod.enhance.ui.glass.GlassTheme
import com.op.aod.enhance.ui.glass.LiquidGlassCard
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

class FeaturesActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GlassScaffold(
                title = "AOD 功能设置",
                onBackClick = { finish() }
            ) { paddingValues ->
                FeaturesContent(
                    initial = AodConfigStore.read(contentResolver),
                    onSave = { cfg -> AodConfigStore.write(contentResolver, cfg) },
                    contentPadding = paddingValues
                )
            }
        }
    }
}

/**
 * AOD 功能特性可复用组件
 */
@OptIn(FlowPreview::class)
@Composable
internal fun FeaturesContent(
    initial: AodUiConfig,
    onSave: (AodUiConfig) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
) {
    val context = LocalContext.current
    var enablePanoramic by remember { mutableStateOf(initial.enablePanoramic) }
    var enableSettingsSupport by remember { mutableStateOf(initial.enableSettingsSupport) }
    var blockSingleClick by remember { mutableStateOf(initial.blockSingleClick) }
    var blockLowLightHide by remember { mutableStateOf(initial.blockLowLightHide) }
    var hideIcon by remember {
        mutableStateOf(
            IconHideUtil.isIconHidden(
                packageManager = context.packageManager,
                packageName = context.packageName,
                mainActivityClass = "${context.packageName}.ui.LauncherAlias"
            ) || initial.hideIcon
        )
    }

    val currentOnSave by rememberUpdatedState(onSave)
    val resolver = context.contentResolver

    // 监听状态变更并自动保存
    LaunchedEffect(Unit) {
        snapshotFlow {
            listOf(enablePanoramic, enableSettingsSupport, blockSingleClick, blockLowLightHide, hideIcon)
        }
            .drop(1)
            .debounce(300)
            .distinctUntilChanged()
            .collect { states ->
                val panoramic = states[0]
                val settingsSupport = states[1]
                val singleClick = states[2]
                val lowLightHide = states[3]
                val iconHidden = states[4]

                val base = AodConfigStore.read(resolver)
                currentOnSave(
                    base.copy(
                        enablePanoramic = panoramic,
                        enableSettingsSupport = settingsSupport,
                        blockSingleClick = singleClick,
                        blockLowLightHide = lowLightHide,
                        hideIcon = iconHidden,
                    )
                )

                // 实时切换桌面启动图标可见性
                IconHideUtil.setIconHidden(
                    packageManager = context.packageManager,
                    packageName = context.packageName,
                    mainActivityClass = "${context.packageName}.ui.LauncherAlias",
                    hidden = iconHidden
                )
            }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AOD 能力与行为开关组
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    GlassSwitchRow(
                        title = "系统界面 - 全天全景 AOD 支持",
                        summary = "让系统界面解锁全天全景 AOD 相关渲染能力",
                        checked = enablePanoramic,
                        onCheckedChange = { enablePanoramic = it }
                    )

                    GlassDivider(modifier = Modifier.fillMaxWidth().height(1.dp))

                    GlassSwitchRow(
                        title = "息屏设置 - 全天全景 AOD 开关",
                        summary = "在系统息屏设置中强制显示全天全景 AOD 开关",
                        checked = enableSettingsSupport,
                        onCheckedChange = { enableSettingsSupport = it }
                    )

                    GlassDivider(modifier = Modifier.fillMaxWidth().height(1.dp))

                    GlassSwitchRow(
                        title = "AOD 单击唤醒屏蔽",
                        summary = "拦截 AOD 单击误触唤醒（ColorOS 15/16/17 双击/电源键正常亮屏）",
                        checked = blockSingleClick,
                        onCheckedChange = { blockSingleClick = it }
                    )

                    GlassDivider(modifier = Modifier.fillMaxWidth().height(1.dp))

                    GlassSwitchRow(
                        title = "低光环境保持 AOD 显示",
                        summary = "阻止极暗环境与夜间超时自动隐藏 AOD 息屏时钟",
                        checked = blockLowLightHide,
                        onCheckedChange = { blockLowLightHide = it }
                    )
                }
            }
        }

        // 桌面图标隐藏设置组
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    GlassSwitchRow(
                        title = "隐藏桌面图标",
                        summary = "隐藏本应用在启动器中的图标，隐藏后仍可在 LSPosed 模块列表中打开",
                        checked = hideIcon,
                        onCheckedChange = { hideIcon = it },
                        activeColor = Color(0xFFFF7B72)
                    )
                }
            }
        }

        // 提示说明
        item {
            Text(
                text = "💡 提示：单击唤醒屏蔽与全景支持修改后，建议重启系统界面（SystemUI）以确保完整生效。",
                color = GlassTheme.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
