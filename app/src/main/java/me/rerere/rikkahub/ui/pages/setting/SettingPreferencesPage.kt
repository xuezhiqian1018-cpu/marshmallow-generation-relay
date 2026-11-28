package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Brain02
import me.rerere.hugeicons.stroke.Code
import me.rerere.hugeicons.stroke.Image01
import me.rerere.hugeicons.stroke.LanguageCircle
import me.rerere.hugeicons.stroke.Message01
import me.rerere.hugeicons.stroke.Notification01
import me.rerere.hugeicons.stroke.PaintBoard
import me.rerere.hugeicons.stroke.Refresh
import me.rerere.hugeicons.stroke.Settings03
import me.rerere.hugeicons.stroke.Sun01
import me.rerere.hugeicons.stroke.ScanText
import me.rerere.rikkahub.R
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.components.ui.CardGroupIcon
import me.rerere.rikkahub.ui.components.ui.CardGroupValue
import me.rerere.rikkahub.ui.components.ui.CardGroupScope
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 简星偏好设置：按参考图做成一整页平铺的长列表。
 * 每一行 = 左侧细描线图标 + 名称 + 右侧当前值 + 箭头；没有副标题。
 * 所有项目都接到简星真实存在的设置页或开关，不造点不动的假项。
 */
@Composable
fun SettingPreferencesPage(vm: SettingVM = koinViewModel()) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navController = LocalNavController.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val display = settings.displaySetting

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(stringResource(R.string.setting_page_preferences))
                },
                navigationIcon = {
                    BackButton()
                },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding + PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CardGroup {
                    // 主题设置 -> 主题页
                    prefRow(
                        icon = HugeIcons.Sun01,
                        label = "主题设置",
                        value = "默认",
                    ) { navController.navigate(Screen.SettingPreferencesTheme) }

                    // 通知 -> 通知页
                    prefRow(
                        icon = HugeIcons.Notification01,
                        label = "通知",
                    ) { navController.navigate(Screen.SettingPreferencesNotification) }

                    // 聊天项显示 -> 界面偏好页（消息显示那一段）
                    prefRow(
                        icon = HugeIcons.Message01,
                        label = "聊天项显示",
                    ) { navController.navigate(Screen.SettingPreferencesUI) }

                    // 渲染设置 -> 界面偏好页（LaTeX/引用高亮）
                    prefRow(
                        icon = HugeIcons.PaintBoard,
                        label = "渲染设置",
                    ) { navController.navigate(Screen.SettingPreferencesUI) }

                    // 行为与启动 -> 常规页
                    prefRow(
                        icon = HugeIcons.Settings03,
                        label = "行为与启动",
                    ) { navController.navigate(Screen.SettingPreferencesGeneral) }

                    // 图片处理 -> 常规页（跳过裁剪）
                    prefRow(
                        icon = HugeIcons.Image01,
                        label = "图片处理",
                    ) { navController.navigate(Screen.SettingPreferencesGeneral) }

                    // 消息样式 -> 界面偏好页
                    prefRow(
                        icon = HugeIcons.Brain02,
                        label = "消息样式",
                    ) { navController.navigate(Screen.SettingPreferencesUI) }

                    // 自动重试 -> 就地开关，真实 networkSetting.enableAutoRetry
                    prefRow(
                        icon = HugeIcons.Refresh,
                        label = "自动重试",
                        trailing = {
                            Switch(
                                checked = settings.networkSetting.enableAutoRetry,
                                onCheckedChange = { checked ->
                                    vm.updateSettings {
                                        it.copy(
                                            networkSetting = it.networkSetting.copy(enableAutoRetry = checked)
                                        )
                                    }
                                }
                            )
                        },
                    )

                    // 触觉反馈 -> 就地开关，真实 displaySetting.enableMessageGenerationHapticEffect
                    prefRow(
                        icon = HugeIcons.Message01,
                        label = "触觉反馈",
                        trailing = {
                            Switch(
                                checked = display.enableMessageGenerationHapticEffect,
                                onCheckedChange = { checked ->
                                    vm.updateSettings {
                                        it.copy(
                                            displaySetting = it.displaySetting.copy(
                                                enableMessageGenerationHapticEffect = checked
                                            )
                                        )
                                    }
                                }
                            )
                        },
                    )

                    // 应用字体 -> 界面偏好页（字体）
                    prefRow(
                        icon = HugeIcons.LanguageCircle,
                        label = "应用字体",
                        value = "系统默认",
                    ) { navController.navigate(Screen.SettingPreferencesUI) }

                    // 代码字体 -> 界面偏好页（代码块）
                    prefRow(
                        icon = HugeIcons.Code,
                        label = "代码字体",
                        value = "系统默认",
                    ) { navController.navigate(Screen.SettingPreferencesUI) }

                    // 聊天字体大小 -> 界面偏好页（字号），显示真实百分比
                    prefRow(
                        icon = HugeIcons.ScanText,
                        label = "聊天字体大小",
                        value = "${(display.fontSizeRatio * 100).toInt()}%",
                    ) { navController.navigate(Screen.SettingPreferencesUI) }

                    // 网络 -> 网络页
                    prefRow(
                        icon = HugeIcons.LanguageCircle,
                        label = "网络",
                    ) { navController.navigate(Screen.SettingPreferencesNetwork) }
                }
            }
        }
    }
}

/** 偏好设置的一行：图标 + 名称 + 右侧值/开关 + 箭头（无副标题），和参考图一致。 */
private fun CardGroupScope.prefRow(
    icon: ImageVector,
    label: String,
    value: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    item(
        onClick = onClick,
        leadingContent = { CardGroupIcon(icon) },
        trailingContent = {
            if (trailing != null) {
                trailing()
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (value != null) {
                        CardGroupValue(value)
                    }
                    Text(
                        text = "›",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        headlineContent = { Text(label) },
    )
}
