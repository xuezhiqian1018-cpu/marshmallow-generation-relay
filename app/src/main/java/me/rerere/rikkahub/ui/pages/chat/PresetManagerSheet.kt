package me.rerere.rikkahub.ui.pages.chat

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Tick01
import me.rerere.hugeicons.stroke.Delete01
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.GenerationPreset
import me.rerere.rikkahub.utils.JsonInstant
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
    val context: Context = LocalContext.current
    var newName by remember { mutableStateOf("预设 ${presets.size + 1}") }
    var pendingDelete by remember { mutableStateOf<GenerationPreset?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("无法读取文件")
            val preset = parseTavernPreset(text)
            onSavePresets(presets + preset)
            importError = null
        } catch (e: Exception) {
            importError = "导入失败：${e.message ?: "文件格式不对"}"
        }
    }

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

            TextButton(
                onClick = { importLauncher.launch("application/json") },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("导入酒馆预设 JSON") }

            if (importError != null) {
                Text(importError!!, color = MaterialTheme.colorScheme.error)
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

private fun parseTavernPreset(text: String): GenerationPreset {
    val root = JsonInstant.parseToJsonElement(text).jsonObject
    fun num(name: String): Float? = root[name]?.runCatching { jsonPrimitive.content.toFloatOrNull() }?.getOrNull()
    fun int(name: String): Int? = num(name)?.toInt()
    val prompts = runCatching {
        root["prompts"]?.toString()
    }.getOrNull().orEmpty()
    val systemPrompt = runCatching {
        val arr = JsonInstant.parseToJsonElement(prompts).toString()
        // 尽量取第一个 system_prompt 条目的正文；取不到就留空，不猜
        if (arr.contains("\"system_prompt\":true")) {
            root["prompts"].toString().substringAfter("\"content\":\"").substringBefore("\"")
        } else ""
    }.getOrDefault("")
    return GenerationPreset(
        name = root["name"]?.runCatching { jsonPrimitive.content }?.getOrNull()
            ?: "酒馆预设",
        temperature = num("temperature"),
        topP = num("top_p"),
        topK = int("top_k"),
        minP = num("min_p"),
        repetitionPenalty = num("repetition_penalty"),
        frequencyPenalty = num("frequency_penalty"),
        presencePenalty = num("presence_penalty"),
        maxTokens = int("openai_max_tokens") ?: int("max_tokens"),
        contextSize = int("openai_max_context"),
        streamOutput = root["stream_openai"]?.runCatching { jsonPrimitive.content.toBooleanStrictOrNull() }?.getOrNull()
            ?: root["stream"]?.runCatching { jsonPrimitive.content.toBooleanStrictOrNull() }?.getOrNull()
            ?: true,
        contextMessageLimit = int("openai_max_context") ?: 0,
        systemPrompt = systemPrompt,
        rawJson = text,
        imported = true,
    )
}
