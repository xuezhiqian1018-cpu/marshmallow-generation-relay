package me.rerere.rikkahub.ui.pages.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Tick01
import me.rerere.hugeicons.stroke.Delete01
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.GenerationPreset

/**
 * 酒馆式预设管理：保存当前参数为预设、切换预设、删除预设。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetManagerSheet(
    presets: List<GenerationPreset>,
    assistant: Assistant,
    onDismiss: () -> Unit,
    onSavePresets: (List<GenerationPreset>) -> Unit,
    onApplyPreset: (GenerationPreset) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var newName by remember { mutableStateOf("预设 ${presets.size + 1}") }
    var pendingDelete by remember { mutableStateOf<GenerationPreset?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("预设", style = MaterialTheme.typography.headlineSmall)

            if (presets.isEmpty()) {
                Text(
                    text = "还没有预设。把当前参数保存下来，之后一键切换。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(presets, key = { it.id }) { preset ->
                        val summary = "温度 ${preset.temperature ?: 1.0f} · Top P ${preset.topP ?: 1.0f} · ${preset.maxTokens ?: 2048} token"
                        ListItem(
                            headlineContent = { Text(preset.name) },
                            supportingContent = { Text(summary) },
                            leadingContent = {
                                Icon(
                                    HugeIcons.Tick01,
                                    contentDescription = null,
                                    tint = if (preset.temperature == assistant.temperature &&
                                        preset.topP == assistant.topP &&
                                        preset.maxTokens == assistant.maxTokens
                                    ) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = { pendingDelete = preset }) {
                                    Icon(HugeIcons.Delete01, contentDescription = "删除预设")
                                }
                            },
                            modifier = Modifier.clickable {
                                onApplyPreset(preset)
                                onDismiss()
                            },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("预设名") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        val preset = GenerationPreset(
                            name = newName.ifBlank { "预设 ${presets.size + 1}" },
                            temperature = assistant.temperature,
                            topP = assistant.topP,
                            maxTokens = assistant.maxTokens,
                            streamOutput = assistant.streamOutput,
                            contextMessageLimit = assistant.contextMessageLimit,
                            systemPrompt = assistant.systemPrompt,
                        )
                        onSavePresets(presets + preset)
                        newName = "预设 ${presets.size + 2}"
                    },
                ) { Text("保存当前") }
            }
        }
    }

    pendingDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除预设") },
            text = { Text("确定删除「${preset.name}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    onSavePresets(presets.filterNot { it.id == preset.id })
                    pendingDelete = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }
}
