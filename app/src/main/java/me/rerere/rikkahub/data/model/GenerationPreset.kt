package me.rerere.rikkahub.data.model

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * 酒馆式预设：一组可保存、可切换的生成参数配置。
 * 与角色卡分离，切换预设只改参数，不改角色本身。
 */
@Serializable
data class GenerationPreset(
    val id: Uuid = Uuid.random(),
    val name: String = "预设",
    val temperature: Float? = null,
    val topP: Float? = null,
    val maxTokens: Int? = null,
    val streamOutput: Boolean = true,
    val contextMessageLimit: Int = 0,
    val systemPrompt: String = "",
)
