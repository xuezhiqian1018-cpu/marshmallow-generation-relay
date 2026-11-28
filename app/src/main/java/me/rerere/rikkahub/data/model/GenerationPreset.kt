package me.rerere.rikkahub.data.model

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * 酒馆式预设：可直接导入 SillyTavern 导出的 Chat Completion 预设 JSON。
 * 采样字段与 ST 对齐，rawJson 保留原文件方便再次导出。
 */
@Serializable
data class GenerationPreset(
    val id: Uuid = Uuid.random(),
    val name: String = "预设",
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val minP: Float? = null,
    val repetitionPenalty: Float? = null,
    val frequencyPenalty: Float? = null,
    val presencePenalty: Float? = null,
    val maxTokens: Int? = null,
    val contextSize: Int? = null,
    val streamOutput: Boolean = true,
    val contextMessageLimit: Int = 0,
    val systemPrompt: String = "",
    val rawJson: String = "",
    val imported: Boolean = false,
)
