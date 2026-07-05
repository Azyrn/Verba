package com.skeler.verba.model

/**
 * Where a model's requests go and whose key pays for them. Every provider
 * here speaks the OpenAI chat-completions dialect, so one client serves all;
 * only the URL and the Bearer token change.
 *
 * [keyHint] is the shape of a plausible key, shown as the field hint.
 * [probeModel] is the cheapest model to ping when validating a new key.
 *
 * [onDevice] marks a provider that never touches the network: it runs a local
 * engine, holds no key, and so is skipped by the API-key section and always
 * unlocked. Its network fields are empty and unused.
 */
enum class Provider(
    val displayName: String,
    val chatUrl: String,
    val keyHint: String,
    val probeModel: String,
    val onDevice: Boolean = false,
) {
    OPENROUTER(
        displayName = "OpenRouter",
        chatUrl = "https://openrouter.ai/api/v1/chat/completions",
        keyHint = "sk-or-v1-…",
        probeModel = "openrouter/free",
    ),
    OPENAI(
        displayName = "OpenAI",
        chatUrl = "https://api.openai.com/v1/chat/completions",
        keyHint = "sk-proj-…",
        probeModel = "gpt-5-mini",
    ),
    ANTHROPIC(
        displayName = "Anthropic",
        chatUrl = "https://api.anthropic.com/v1/chat/completions",
        keyHint = "sk-ant-…",
        probeModel = "claude-haiku-4-5",
    ),
    GOOGLE(
        displayName = "Google AI",
        chatUrl = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
        keyHint = "AIza…",
        probeModel = "gemini-2.5-flash",
    ),
    MISTRAL(
        displayName = "Mistral",
        chatUrl = "https://api.mistral.ai/v1/chat/completions",
        keyHint = "…",
        probeModel = "mistral-small-latest",
    ),
    DEEPSEEK(
        displayName = "DeepSeek",
        chatUrl = "https://api.deepseek.com/v1/chat/completions",
        keyHint = "sk-…",
        probeModel = "deepseek-v4-flash",
    ),
    MLKIT(
        displayName = "On-device",
        chatUrl = "",
        keyHint = "",
        probeModel = "",
        onDevice = true,
    ),
}
