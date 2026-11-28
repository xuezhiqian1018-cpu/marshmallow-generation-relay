package me.rerere.rikkahub.ui.pages.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Book01
import me.rerere.hugeicons.stroke.Brain02
import me.rerere.hugeicons.stroke.Message02
import me.rerere.rikkahub.R
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.GenerationPreset
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.pages.assistant.detail.BackgroundPicker
import kotlin.math.roundToInt

/**
 * 简星「对话设置」面板：从聊天输入框工具栏的齿轮打开。
 *
 * 布局按用户给的参考图：基本（名称 / 指令）、简星（指令注入 / 世界书 / 预设 / 记忆）、
 * 模型参数（左标题＋右控件，数值在右，滑块在下）、背景 / 前景、底部完成。
 * 所有字段接真实数据、真实生效。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationSettingsSheet(
    conversation: Conversation,
    assistant: Assistant,
    settings: Settings,
    onDismiss: () -> Unit,
    onUpdateTitle: (String) -> Unit,
    onUpdateConversationSystemPrompt: (String?) -> Unit,
    onUpdateAssistant: (Assistant) -> Unit,
    onSavePresets: (List<GenerationPreset>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val navigator = LocalNavController.current

    var titleInput by remember(conversation.id) { mutableStateOf(conversation.title) }
    var instructionInput by remember(conversation.id) {
        mutableStateOf(conversation.customSystemPrompt ?: "")
    }
    var promptInput by remember(assistant.id) { mutableStateOf(assistant.systemPrompt) }
    var showPresetSheet by remember { mutableStateOf(false) }

    fun commitAndClose() {
        onUpdateTitle(titleInput)
        onUpdateConversationSystemPrompt(instructionInput.ifBlank { null })
        onUpdateAssistant(assistant.copy(systemPrompt = promptInput))
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = { commitAndClose() },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.conversation_settings_title),
                style = MaterialTheme.typography.headlineSmall,
            )

            // 基本
            SectionTitle("基本")
            SettingCard {
                LabeledField("名称", titleInput) { titleInput = it }
                LabeledField("指令", instructionInput, minLines = 3, maxLines = 8) { instructionInput = it }
                LabeledField("提示词", promptInput, minLines = 3, maxLines = 10) { promptInput = it }
            }

            // 简星：指令注入 / 世界书 / 预设 / 记忆
            SectionTitle("简星")
            SettingCard {
                CountRow(
                    icon = HugeIcons.Message02,
                    label = "指令注入",
                    count = "${assistant.modeInjectionIds.size}/${settings.modeInjections.size}",
                ) { navigator.navigate(Screen.AssistantInjections(assistant.id.toString())) }
                CountRow(
                    icon = HugeIcons.Book01,
                    label = "世界书",
                    count = "${assistant.lorebookIds.size}/${settings.lorebooks.size}",
                ) { navigator.navigate(Screen.AssistantInjections(assistant.id.toString())) }
                SettingRow(
                    icon = HugeIcons.Message02,
                    label = stringResource(R.string.conversation_settings_preset),
                ) { showPresetSheet = true }
                SettingRow(
                    icon = HugeIcons.Brain02,
                    label = stringResource(R.string.conversation_settings_memory),
                ) { navigator.navigate(Screen.AssistantMemory(assistant.id.toString())) }
            }

            // 模型参数：Kelivo 格式（左标题＋右控件，数值在右，滑块在下）
            SectionTitle("模型参数")
            SettingCard {
                SettingRow(
                    icon = HugeIcons.Brain02,
                    label = "模型",
                    value = assistant.name.ifBlank { "默认模型" },
                ) { navigator.navigate(Screen.SettingModels) }

                SliderRow(
                    label = stringResource(R.string.assistant_page_temperature),
                    value = assistant.temperature ?: 1.0f,
                    range = 0f..2f,
                ) { onUpdateAssistant(assistant.copy(temperature = it)) }

                SliderRow(
                    label = stringResource(R.string.assistant_page_top_p),
                    value = assistant.topP ?: 1.0f,
                    range = 0f..1f,
                ) { onUpdateAssistant(assistant.copy(topP = it)) }

                SliderRow(
                    label = stringResource(R.string.assistant_page_max_tokens),
                    value = (assistant.maxTokens ?: 2048).toFloat(),
                    range = 0f..128000f,
                    integer = true,
                ) { onUpdateAssistant(assistant.copy(maxTokens = it.roundToInt())) }

                SwitchRow(
                    label = stringResource(R.string.assistant_page_stream_output),
                    checked = assistant.streamOutput,
                ) { onUpdateAssistant(assistant.copy(streamOutput = it)) }

                SwitchRow(
                    label = stringResource(R.string.assistant_page_context_message_limit),
                    checked = assistant.contextMessageLimit > 0,
                ) { onUpdateAssistant(assistant.copy(contextMessageLimit = if (it) 20 else 0)) }
            }

            // 背景 / 前景
            SectionTitle("背景")
            SettingCard {
                BackgroundPicker(
                    background = assistant.background,
                    backgroundOpacity = assistant.backgroundOpacity,
                    onUpdate = { background -> onUpdateAssistant(assistant.copy(background = background)) },
                )
                if (assistant.background != null) {
                    Text(
                        text = stringResource(R.string.conversation_settings_blur),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Slider(
                        value = assistant.backgroundOpacity.coerceIn(0f, 1f),
                        onValueChange = { onUpdateAssistant(assistant.copy(backgroundOpacity = it)) },
                        valueRange = 0f..1f,
                    )
                }
            }

            Button(
                onClick = { commitAndClose() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("完成")
            }
        }
    }

    if (showPresetSheet) {
        PresetManagerSheet(
            presets = settings.generationPresets,
            assistant = assistant,
            onDismiss = { showPresetSheet = false },
            onSavePresets = onSavePresets,
            // 窗口里只切换启用，不在窗口添加/删除
            manageOnly = false,
            onApplyPreset = { preset ->
                onUpdateAssistant(
                    assistant.copy(
                        temperature = preset.temperature,
                        topP = preset.topP,
                        maxTokens = preset.maxTokens,
                        streamOutput = preset.streamOutput,
                        contextMessageLimit = preset.contextMessageLimit,
                        systemPrompt = preset.systemPrompt,
                    )
                )
                promptInput = preset.systemPrompt
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SettingCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    minLines: Int = 1,
    maxLines: Int = 1,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            minLines = minLines,
            maxLines = maxLines,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    label: String,
    value: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CountRow(
    icon: ImageVector,
    label: String,
    count: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(count, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    integer: Boolean = false,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                text = if (integer) value.roundToInt().toString() else String.format("%.2f", value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value.coerceIn(range),
            onValueChange = onValueChange,
            valueRange = range,
        )
    }
}
