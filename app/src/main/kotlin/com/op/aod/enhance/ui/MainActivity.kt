package com.op.aod.enhance.ui

import android.content.Intent
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.op.aod.enhance.ui.glass.GlassArrowRow
import com.op.aod.enhance.ui.glass.GlassDivider
import com.op.aod.enhance.ui.glass.GlassScaffold
import com.op.aod.enhance.ui.glass.GlassTheme
import com.op.aod.enhance.ui.glass.LiquidGlassCard

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen(
                onOpenBrightness = { startActivity(Intent(this, BrightnessActivity::class.java)) },
                onOpenFeatures = { startActivity(Intent(this, FeaturesActivity::class.java)) }
            )
        }
    }
}

@Composable
private fun MainScreen(
    onOpenBrightness: () -> Unit,
    onOpenFeatures: () -> Unit,
) {
    GlassScaffold(
        title = "ColorOS AOD 增强"
    ) { paddingValues: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 模块状态横幅卡片
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundAlpha = 0.22f
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "✨ 模块运行就绪",
                        color = Color(0xFF7EE787),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "支持 ColorOS 15 / 16 / 17 全版本 AOD 特性定制与单击唤醒防误触",
                        color = GlassTheme.TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }

            // 功能导航卡片
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundAlpha = 0.18f
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    GlassArrowRow(
                        title = "AOD 亮度设置",
                        summary = "调整初始进入亮度与运行时动态倍率",
                        onClick = onOpenBrightness
                    )

                    GlassDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                    )

                    GlassArrowRow(
                        title = "AOD 功能设置",
                        summary = "系统界面全景、息屏开关、防误触与隐藏图标",
                        onClick = onOpenFeatures
                    )
                }
            }
        }
    }
}
