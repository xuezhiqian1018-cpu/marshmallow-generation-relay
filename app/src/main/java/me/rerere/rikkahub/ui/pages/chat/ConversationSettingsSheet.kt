package me.rerere.rikkahub.ui.pages.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Book01
import me.rerere.hugeicons.stroke.Brain02
import me.rerere.hugeicons.stroke.Message02
import me.rerere.rikkahub.R
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.ui.pages.assistant.detail.BackgroundPicker
import me.rerere.rikkahub.ui.context.LocalNavController
import kotlin.math.roundToInt

/**
 * 简星「对话设置」面板：从聊天输入框工具栏的齿轮打开。
 *
 * 所有字段接真实数据、真实生效：
 * - 名称 -> vm.updateTitle
 * - 指令(对话级系统提示) -> vm.updateConversationSystemPrompt
 * - 提示词(助手系统提示) / 模型参数 / 背景 -> vm.updateAssistant(assistant.copy(...))
 * - 世界书 / 预设 / 记忆 -> 跳转已有页面（传助手 id）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationSettingsSheet(
    conversation: Conversation,
    assistant: Assistant,
    onDismiss: () -> Unit,
    onUpdateTitle: (String) -> Unit,
    onUpdateConversationSystemPrompt: (String?) -> Unit,
    onUpdateAssistant: (Assistant) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val navigator = LocalNavController.current

    // 名称：本地编辑，离开焦点/确认时写回，避免每敲一个字都落库
    var titleInput by remember(conversation.id) { mutableStateOf(conversation.title) }
    // 指令：对话级系统提示
    var instructionInput by remember(conversation.id) {
        mutableStateOf(conversation.customSystemPrompt ?: "")
    }
    // 提示词：助手级系统提示
    var promptInput by remember(assistant.id) { mutableStateOf(assistant.systemPrompt) }

    ModalBottomSheet(
        onDismissRequest = {
            // 关闭前把正在编辑的文本落库，保证真实生效
            onUpdateTitle(titleInput)
            onUpdateConversationSystemPrompt(instructionInput.ifBlank { null })
            onUpdateAssistant(assistant.copy(systemPrompt = promptInput))
            onDismiss()
        },
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

            // 名称
            SettingCard {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text(stringResource(R.string.assistant_page_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // 指令（对话级系统提示）
            SettingCard {
                Text(
                    text = stringResource(R.string.conversation_settings_instruction),
                    style = MaterialTheme.typography.titleSmall,
                )
                OutlinedTextField(
                    value = instructionInput,
                    onValueChange = { instructionInput = it },
                    label = { Text(stringResource(R.string.chat_page_conversation_system_prompt)) },
                    minLines = 3,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // 提示词（助手级系统提示）
            SettingCard {
                Text(
                    text = stringResource(R.string.conversation_settings_prompt),
                    style = MaterialTheme.typography.titleSmall,
                )
                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    minLines = 3,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // 世界书 / 预设 / 记忆：跳转已有页面，真实可用
            SettingCard {
                SettingRow(
                    icon = HugeIcons.Book01,
                    label = stringResource(R.string.conversation_settings_lorebook),
                ) {
                    navigator.navigate(Screen.AssistantInjections(assistant.id.toString()))
                }
                SettingRow(
                    icon = HugeIcons.Message02,
                    label = stringResource(R.string.conversation_settings_preset),
                ) {
                    navigator.navigate(Screen.AssistantPrompt(assistant.id.toString()))
                }
                SettingRow(
                    icon = HugeIcons.Brain02,
                    label = stringResource(R.string.conversation_settings_memory),
                ) {
                    navigator.navigate(Screen.AssistantMemory(assistant.id.toString()))
                }
            }

            // 模型参数
            SettingCard {
                Text(
                    text = stringResource(R.string.conversation_settings_model_params),
                    style = MaterialTheme.typography.titleSmall,
                )

                SwitchRow(
                    label = stringResource(R.string.assistant_page_stream_output),
                    checked = assistant.streamOutput,
                ) { onUpdateAssistant(assistant.copy(streamOutput = it)) }

                NumberParamRow(
                    label = stringResource(R.string.assistant_page_temperature),
                    enabled = assistant.temperature != null,
                    value = assistant.temperature ?: 1.0f,
                    range = 0f..2f,
                    onToggle = { on -> onUpdateAssistant(assistant.copy(temperature = if (on) 1.0f else null)) },
                    onValue = { onUpdateAssistant(assistant.copy(temperature = it)) },
                )

                NumberParamRow(
                    label = stringResource(R.string.assistant_page_top_p),
                    enabled = assistant.topP != null,
                    value = assistant.topP ?: 1.0f,
                    range = 0f..1f,
                    onToggle = { on -> onUpdateAssistant(assistant.copy(topP = if (on) 1.0f else null)) },
                    onValue = { onUpdateAssistant(assistant.copy(topP = it)) },
                )

                NumberParamRow(
                    label = stringResource(R.string.assistant_page_max_tokens),
                    enabled = assistant.maxTokens != null,
                    value = (assistant.maxTokens ?: 2048).toFloat(),
                    range = 0f..128000f,
                    onToggle = { on -> onUpdateAssistant(assistant.copy(maxTokens = if (on) 2048 else null)) },
                    onValue = { onUpdateAssistant(assistant.copy(maxTokens = it.roundToInt())) },
                )

                SwitchRow(
                    label = stringResource(R.string.assistant_page_context_message_limit),
                    checked = assistant.contextMessageLimit > 0,
                ) { on ->
                    onUpdateAssistant(assistant.copy(contextMessageLimit = if (on) 20 else 0))
                }
            }

            // 背景
            SettingCard {
                Text(
                    text = stringResource(R.string.conversation_settings_background),
                    style = MaterialTheme.typography.titleSmall,
                )
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
                        onValueChange = {
                            onUpdateAssistant(assistant.copy(backgroundOpacity = it))
                        },
                        valueRange = 0f..1f,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingCard(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun NumberParamRow(
    label: String,
    enabled: Boolean,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onToggle: (Boolean) -> Unit,
    onValue: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SwitchRow(label = label, checked = enabled, onCheckedChange = onToggle)
        if (enabled) {
            Text(
                text = if (range.endInclusive > 1000f) value.roundToInt().toString() else String.format("%.2f", value),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = value.coerceIn(range),
                onValueChange = onValue,
                valueRange = range,
            )
        }
    }
}
