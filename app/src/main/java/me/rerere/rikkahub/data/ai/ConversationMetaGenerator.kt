package me.rerere.rikkahub.data.ai

import android.content.Context
import me.rerere.ai.provider.Model
import me.rerere.ai.provider.ProviderManager
import me.rerere.ai.provider.ProviderSetting
import me.rerere.ai.ui.UIMessage
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.data.datastore.findProvider
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.utils.applyPlaceholders
import java.util.Locale
import kotlin.uuid.Uuid

/** 用快速模型生成会话的标题和聊天建议，只负责请求模型，写回会话由调用方处理 */
class ConversationMetaGenerator(
    private val context: Context,
    private val providerManager: ProviderManager,
    private val usageRecorder: UsageRecorder,
) {
    /** 快速模型或它的提供商找不到时抛出异常，提示用户检查设置 */
    suspend fun generateTitle(settings: Settings, conversation: Conversation): String {
        val model = settings.findModelById(settings.fastModelId)
            ?: throw IllegalStateException(context.getString(R.string.error_fast_model_not_found))
        val provider = model.findProvider(settings.providers)
            ?: throw IllegalStateException(context.getString(R.string.error_fast_model_provider_not_found))
        return generate(
            settings,
            model,
            provider,
            conversation,
            settings.titlePrompt,
            recentMessages = 4,
            UsageSource.TITLE
        )
            .trim()
    }

    /** 没有配置快速模型时返回 null */
    suspend fun generateSuggestions(settings: Settings, conversation: Conversation): List<String>? {
        val model = settings.findModelById(settings.fastModelId) ?: return null
        val provider = model.findProvider(settings.providers) ?: return null
        return generate(
            settings,
            model,
            provider,
            conversation,
            settings.suggestionPrompt,
            recentMessages = 8,
            UsageSource.SUGGESTION,
        )
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(10)
    }


    /**
     * 简星：从用户输入里自动提取「用户设定句」。
     * 只提取描述 {{user}} 自身长期属性的句子（性格/身份/外貌/喜好/关系/称呼等），
     * 忽略剧情动作、场景推进、临时指令。没有设定返回 null。
     * 用快速模型、后台参数，失败由调用方吞掉。
     */
    suspend fun extractUserSetting(settings: Settings, userText: String): String? {
        val model = settings.findModelById(settings.fastModelId) ?: return null
        val provider = model.findProvider(settings.providers) ?: return null
        val prompt = """你是设定提取器。从下面这段用户说的话里，只挑出描述「用户本人」长期设定的句子，例如性格、身份、职业、外貌、喜好、习惯、和角色{char}的关系、希望被如何称呼等。

要求：
- 只输出这些设定句，每条一行，保留原意，用第三人称「用户」开头或者直接陈述。
- 剧情动作、场景描写、临时指令、对角色下的命令一律不要输出。
- 如果这段话里没有任何关于用户自己的设定，只输出两个字：无设定
- 不要解释，不要加引号，不要输出其它任何内容。

用户说的话：
{content}"""
        val result = usageRecorder.wrap(
            providerManager.getProviderByType(provider),
            UsageTag(UsageSource.TITLE, null, null),
        ).generateText(
            providerSetting = provider,
            messages = listOf(
                UIMessage.user(
                    prompt
                        .replace("{char}", "{{char}}")
                        .replace("{content}", userText)
                ),
            ),
            params = backgroundTextGenerationParams(model, Uuid.random(), settings.fastModelReasoningLevel),
        )
        val text = result.message.toText().trim()
        if (text.isEmpty() || text.contains("无设定")) return null
        // 去掉可能的多余空行与列表符号
        val lines = text.lineSequence()
            .map { it.trim().trimStart('-', '•', '*').trim() }
            .filter { it.isNotEmpty() && it != "无设定" }
            .toList()
        if (lines.isEmpty()) return null
        return lines.joinToString("\n")
    }

    private suspend fun generate(
        settings: Settings,
        model: Model,
        provider: ProviderSetting,
        conversation: Conversation,
        prompt: String,
        recentMessages: Int,
        source: UsageSource,
    ): String {
        val result = usageRecorder.wrap(
            providerManager.getProviderByType(provider),
            UsageTag(source, conversation.assistantId, conversation.id),
        ).generateText(
            providerSetting = provider,
            messages = listOf(
                UIMessage.user(
                    prompt.applyPlaceholders(
                        "locale" to Locale.getDefault().displayName,
                        "content" to conversation.currentMessages
                            .takeLast(recentMessages).joinToString("\n\n") { it.summaryAsText(maxLength = 500) },
                    )
                ),
            ),
            params = backgroundTextGenerationParams(model, conversation.id, settings.fastModelReasoningLevel),
        )
        return result.message.toText()
    }
}
