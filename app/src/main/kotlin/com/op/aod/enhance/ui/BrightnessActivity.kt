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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.op.aod.enhance.data.AodConfigStore
import com.op.aod.enhance.data.AodUiConfig
import com.op.aod.enhance.ui.glass.GlassDivider
import com.op.aod.enhance.ui.glass.GlassInputField
import com.op.aod.enhance.ui.glass.GlassScaffold
import com.op.aod.enhance.ui.glass.GlassSliderRow
import com.op.aod.enhance.ui.glass.GlassTheme
import com.op.aod.enhance.ui.glass.LiquidGlassCard
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

class BrightnessActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GlassScaffold(
                title = "AOD 亮度设置",
                onBackClick = { finish() }
            ) { paddingValues ->
                BrightnessContent(
                    initial = AodConfigStore.read(contentResolver),
                    onSave = { cfg -> AodConfigStore.write(contentResolver, cfg) },
                    contentPadding = paddingValues
                )
            }
        }
    }
}

/**
 * AOD 亮度调节可复用组件
 */
@OptIn(FlowPreview::class)
@Composable
internal fun BrightnessContent(
    initial: AodUiConfig,
    onSave: (AodUiConfig) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
) {
    var initDark by remember { mutableFloatStateOf(initial.initDark.toFloat()) }
    var initBright by remember { mutableFloatStateOf(initial.initBright.toFloat()) }
    var runningMultiplier by remember { mutableFloatStateOf(initial.runningMultiplier) }
    val currentOnSave by rememberUpdatedState(onSave)
    val resolver = LocalContext.current.contentResolver

    LaunchedEffect(Unit) {
        snapshotFlow { Triple(initDark, initBright, runningMultiplier) }
            .drop(1)
            .debounce(300)
            .distinctUntilChanged()
            .collect { (dark, bright, multi) ->
                val base = AodConfigStore.read(resolver)
                currentOnSave(
                    base.copy(
                        initDark = dark.toInt(),
                        initBright = bright.toInt(),
                        runningMultiplier = multi,
                    )
                )
            }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 滑块调节卡片
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    GlassSliderRow(
                        label = "熄屏前暗光环境 AOD 亮度",
                        value = initDark,
                        onValueChange = { initDark = it.coerceIn(0f, 255f) },
                        valueRange = 0f..255f,
                        steps = 254,
                        valueDisplay = "${initDark.toInt()}"
                    )

                    GlassDivider(modifier = Modifier.fillMaxWidth().height(1.dp))

                    GlassSliderRow(
                        label = "熄屏前亮光环境 AOD 亮度",
                        value = initBright,
                        onValueChange = { initBright = it.coerceIn(0f, 255f) },
                        valueRange = 0f..255f,
                        steps = 254,
                        valueDisplay = "${initBright.toInt()}"
                    )

                    GlassDivider(modifier = Modifier.fillMaxWidth().height(1.dp))

                    GlassSliderRow(
                        label = "熄屏时 AOD 自动亮度倍率",
                        value = runningMultiplier,
                        onValueChange = {
                            runningMultiplier = ((it * 10).toInt().coerceIn(10, 20) / 10f)
                        },
                        valueRange = 1.0f..2.0f,
                        steps = 9,
                        valueDisplay = "× $runningMultiplier"
                    )
                }
            }
        }

        // 精准数字输入卡片
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = "精准数值设定",
                        color = GlassTheme.TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    GlassInputField(
                        label = "暗光环境初始亮度 (0 ~ 255)",
                        value = initDark.toInt().toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { v -> initDark = v.coerceIn(0, 255).toFloat() }
                        }
                    )

                    GlassInputField(
                        label = "亮光环境初始亮度 (0 ~ 255)",
                        value = initBright.toInt().toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { v -> initBright = v.coerceIn(0, 255).toFloat() }
                        }
                    )

                    GlassInputField(
                        label = "运行亮度倍率 (1.0 ~ 2.0)",
                        value = runningMultiplier.toString(),
                        onValueChange = {
                            it.toFloatOrNull()?.let { v -> runningMultiplier = v.coerceIn(1.0f, 2.0f) }
                        }
                    )
                }
            }
        }

        // 底部说明
        item {
            Text(
                text = "💡 提示：熄屏时系统默认会削减 AOD 亮度（乘以约 0.6），倍率设为 1.6 可抵消削减，使 AOD 保持正常亮度。",
                color = GlassTheme.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
