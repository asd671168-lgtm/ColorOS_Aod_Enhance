package com.op.aod.enhance.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 液态玻璃主题统一色彩系统与背景容器
 */
object GlassTheme {
    // 沉浸式流体动态背景底色
    val BackgroundStart = Color(0xFF0D1117)
    val BackgroundMid = Color(0xFF131A29)
    val BackgroundEnd = Color(0xFF090D14)

    // 光晕色
    val CyanGlow = Color(0xFF00C6FF).copy(alpha = 0.15f)
    val PurpleGlow = Color(0xFF7F00FF).copy(alpha = 0.12f)
    val BlueGlow = Color(0xFF0072FF).copy(alpha = 0.18f)

    // 文字色彩
    val TextPrimary = Color(0xFFF0F6FC)
    val TextSecondary = Color(0xFF8B949E)
    val TextAccent = Color(0xFF58A6FF)

    // 交互主色
    val AccentActive = Color(0xFF388BFD)
    val AccentGradient = Brush.horizontalGradient(
        listOf(Color(0xFF2F80ED), Color(0xFF00C6FF))
    )
}

/**
 * 液态玻璃风格主页面脚手架 (Scaffold)
 *
 * 带有流体光晕效果背景 + 磨砂玻璃顶部栏 + 沉浸式安全区适配
 */
@Composable
fun GlassScaffold(
    title: String,
    onBackClick: (() -> Unit)? = null,
    content: @Composable BoxScope.(PaddingValues) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GlassTheme.BackgroundStart,
                        GlassTheme.BackgroundMid,
                        GlassTheme.BackgroundEnd
                    )
                )
            )
    ) {
        // 背景流体光晕球 (Fluid Aura)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(GlassTheme.BlueGlow, Color.Transparent),
                        center = Offset(200f, 300f),
                        radius = 800f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(GlassTheme.PurpleGlow, Color.Transparent),
                        center = Offset(800f, 1600f),
                        radius = 900f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 顶部玻璃导航栏 (Liquid Glass Top Bar)
            GlassTopBar(
                title = title,
                onBackClick = onBackClick
            )

            // 页面内容
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                content(PaddingValues(horizontal = 16.dp, vertical = 8.dp))
            }
        }
    }
}

/**
 * 液态玻璃风格顶部应用栏
 */
@Composable
fun GlassTopBar(
    title: String,
    onBackClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBackClick != null) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBackClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "‹",
                    color = GlassTheme.TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
        }

        Text(
            text = title,
            color = GlassTheme.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}
