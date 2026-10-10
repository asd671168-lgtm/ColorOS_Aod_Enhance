package com.op.aod.enhance.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.op.aod.enhance.data.AodConfigStore
import com.op.aod.enhance.data.AodUiConfig
import com.op.aod.enhance.ui.glass.FloatingGlassBottomBar
import com.op.aod.enhance.ui.glass.GlassBottomTabItem
import com.op.aod.enhance.ui.glass.GlassScaffold

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val initialConfig = remember { AodConfigStore.read(contentResolver) }
            MainScreen(
                initial = initialConfig,
                onSave = { cfg -> AodConfigStore.write(contentResolver, cfg) }
            )
        }
    }
}

@Composable
private fun MainScreen(
    initial: AodUiConfig,
    onSave: (AodUiConfig) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val tabItems = remember {
        listOf(
            GlassBottomTabItem(title = "功能设置", iconEmoji = "⚙️"),
            GlassBottomTabItem(title = "亮度设置", iconEmoji = "☀️")
        )
    }

    val pageTitle = if (selectedTab == 0) "AOD 功能定制" else "AOD 亮度调节"

    GlassScaffold(
        title = pageTitle
    ) { paddingValues: PaddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 平滑切换二级页面内容
            Crossfade(
                targetState = selectedTab,
                animationSpec = tween(durationMillis = 240),
                label = "pageTransition"
            ) { tab ->
                val listPadding = PaddingValues(top = 4.dp, bottom = 96.dp)
                when (tab) {
                    0 -> FeaturesContent(
                        initial = initial,
                        onSave = onSave,
                        contentPadding = listPadding
                    )
                    1 -> BrightnessContent(
                        initial = initial,
                        onSave = onSave,
                        contentPadding = listPadding
                    )
                }
            }

            // 悬浮液态玻璃底栏 (Floating Liquid Glass Bottom Bar)
            FloatingGlassBottomBar(
                items = tabItems,
                selectedIndex = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp)
            )
        }
    }
}
