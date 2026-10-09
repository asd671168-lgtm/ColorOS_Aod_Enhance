package com.op.aod.enhance.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 液态玻璃 (Liquid Glass) 容器组件。
 *
 * 使用 Android 12+ 原生渐变叠加实现毛玻璃质感，
 * 不依赖任何第三方库，使用 AndroidX Compose 内置 API。
 *
 * @param modifier 外部修饰符
 * @param cornerRadius 圆角半径，默认 24dp
 * @param backgroundAlpha 背景透明度 0.0–1.0，默认 0.18
 * @param tintColor 玻璃色调，默认白色
 * @param content 内容
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    backgroundAlpha: Float = 0.18f,
    tintColor: Color = Color.White,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            // 深色半透明底色
            .background(
                color = Color.Black.copy(alpha = backgroundAlpha),
                shape = shape
            )
            // 内部高光渐变（顶部→透明）
            .drawWithContent {
                drawContent()
                // 内发光：顶部白色渐变高光
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            tintColor.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = size.height * 0.6f
                    )
                )
                // 左侧光泽边
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            tintColor.copy(alpha = 0.07f),
                            Color.Transparent
                        ),
                        startX = 0f,
                        endX = size.width * 0.4f
                    )
                )
                // 底部阴影（深色）
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.12f)
                        ),
                        startY = size.height * 0.7f,
                        endY = size.height
                    )
                )
            }
            // 外边框：白色光泽边框
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        tintColor.copy(alpha = 0.55f),
                        tintColor.copy(alpha = 0.08f),
                        Color.Black.copy(alpha = 0.25f)
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite
                ),
                shape = shape
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        content = content
    )
}

/**
 * 液态玻璃分割线。
 */
@Composable
fun GlassDivider(
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.12f)
) {
    Box(
        modifier = modifier
            .background(color)
            .padding(horizontal = 16.dp)
    )
}
