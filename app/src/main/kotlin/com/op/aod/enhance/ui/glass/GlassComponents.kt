package com.op.aod.enhance.ui.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 液态玻璃风格 Switch 行（切换开关）。
 *
 * 视觉设计：
 * - 背景：半透明玻璃底色
 * - 开关：圆形药丸形状，开启时为活跃色渐变，关闭时为灰色玻璃
 * - 按下时有轻微缩放反馈动画
 *
 * @param title 主标题
 * @param summary 副标题/描述（可选）
 * @param checked 当前选中状态
 * @param onCheckedChange 状态变更回调
 * @param activeColor 开启状态的颜色，默认为蓝紫渐变
 */
@Composable
fun GlassSwitchRow(
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeColor: Color = Color(0xFF6C8FFF),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(100),
        label = "press_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 文字区域
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp
            )
            if (summary != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = summary,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // 液态玻璃开关
        GlassSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            activeColor = activeColor
        )
    }
}

/**
 * 液态玻璃风格 Toggle 开关控件。
 */
@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeColor: Color = Color(0xFF6C8FFF),
) {
    val trackAlpha by animateFloatAsState(
        targetValue = if (checked) 1f else 0.3f,
        animationSpec = tween(200),
        label = "track_alpha"
    )
    val thumbOffsetFraction by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(250),
        label = "thumb_offset"
    )

    val trackWidth = 48.dp
    val trackHeight = 28.dp
    val thumbSize = 22.dp
    val padding = 3.dp

    Box(
        modifier = Modifier
            .size(width = trackWidth, height = trackHeight)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
            .drawBehind {
                // 轨道背景渐变
                if (checked) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                activeColor.copy(alpha = trackAlpha),
                                activeColor.copy(alpha = trackAlpha * 0.7f)
                            )
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
                    )
                } else {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.15f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
                    )
                }
                // 轨道高光边框
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.05f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
    ) {
        // 滑块（Thumb）
        val thumbTravel = (trackWidth - thumbSize - padding * 2)
        Box(
            modifier = Modifier
                .padding(start = padding + thumbTravel * thumbOffsetFraction, top = padding)
                .size(thumbSize)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            Color.White.copy(alpha = 0.85f)
                        )
                    )
                )
                // 滑块内发光
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.9f),
                                Color.White.copy(alpha = 0.6f)
                            )
                        )
                    )
                }
        )
    }
}

/**
 * 液态玻璃风格箭头行（导航条目）。
 *
 * @param title 标题
 * @param summary 描述
 * @param onClick 点击回调
 */
@Composable
fun GlassArrowRow(
    title: String,
    summary: String? = null,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(100),
        label = "press_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp
            )
            if (summary != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = summary,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // 箭头图标（用 Text 模拟 >）
        Text(
            text = "›",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 22.sp,
            fontWeight = FontWeight.Light
        )
    }
}
