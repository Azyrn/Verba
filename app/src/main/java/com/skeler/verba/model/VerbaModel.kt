package com.skeler.verba.model

import androidx.annotation.StringRes
import com.skeler.verba.R

/**
 * A model Verba can route translations through. [description] is an honest
 * one-line account of the speed/quality trade-off, not marketing — it is null
 * for a [custom] model the user typed in, since only they know what it is. The
 * bundled list is free for everyone — mostly OpenRouter's free tier, plus one
 * Gemini model backed by an app-supplied key; the rest unlock when the user
 * adds their own key for that [provider].
 */
data class VerbaModel(
    val id: String,
    val name: String,
    @param:StringRes val description: Int? = null,
    val provider: Provider = Provider.OPENROUTER,
    val custom: Boolean = false,
) {
    companion object {
        /** A model id the user typed by hand for [provider]. */
        fun custom(provider: Provider, id: String): VerbaModel =
            VerbaModel(id = id, name = id, provider = provider, custom = true)
    }
}

object VerbaModels {

    /**
     * The bundled free tier, available without any personal key: two OpenRouter
     * free models and one Gemini model paid for by the developer's own key, so
     * it's shared across everyone running the app. [R.string.model_free_tier_note]
     * on the Model screen is the one place that says so and points at API keys
     * for a dedicated quota — keep that note if this list ever grows again.
     */
    val all: List<VerbaModel> = listOf(
        VerbaModel(
            id = "nvidia/nemotron-3-super-120b-a12b:free",
            name = "Nemotron 3 Super",
            description = R.string.model_nemotron_super,
        ),
        VerbaModel(
            id = "openai/gpt-oss-20b:free",
            name = "GPT-OSS 20B",
            description = R.string.model_gpt_oss_20b,
        ),
        VerbaModel(
            id = "gemini-3.1-flash-lite",
            name = "Gemini 3.1 Flash Lite",
            description = R.string.model_gemini_flash_lite,
            provider = Provider.GOOGLE,
        ),
        VerbaModel(
            id = "mlkit/on-device",
            name = "Offline",
            description = R.string.model_offline,
            provider = Provider.MLKIT,
        ),
    )

    /**
     * Known bring-your-own-key model ids, kept only to recognize a typed id
     * as one of these (for [preset] and [byId]) — never shown in
     * [available] itself, which lists just the exact model each provider's
     * key was tested with.
     */
    val byok: List<VerbaModel> = listOf(
        VerbaModel(
            id = "gpt-5.1",
            name = "GPT-5.1",
            description = R.string.model_gpt51,
            provider = Provider.OPENAI,
        ),
        VerbaModel(
            id = "gpt-5-mini",
            name = "GPT-5 Mini",
            description = R.string.model_gpt5_mini,
            provider = Provider.OPENAI,
        ),
        VerbaModel(
            id = "claude-sonnet-5",
            name = "Claude Sonnet 5",
            description = R.string.model_sonnet5,
            provider = Provider.ANTHROPIC,
        ),
        VerbaModel(
            id = "claude-haiku-4-5",
            name = "Claude Haiku 4.5",
            description = R.string.model_haiku45,
            provider = Provider.ANTHROPIC,
        ),
        VerbaModel(
            id = "gemini-3-pro-preview",
            name = "Gemini 3 Pro",
            description = R.string.model_gemini3_pro,
            provider = Provider.GOOGLE,
        ),
        VerbaModel(
            id = "gemini-2.5-flash",
            name = "Gemini 2.5 Flash",
            description = R.string.model_gemini_flash,
            provider = Provider.GOOGLE,
        ),
    )

    val default: VerbaModel = all.first()

    /**
     * One flat list: the free tier, then only the exact model id each unlocked
     * provider's key was tested with — never the rest of that provider's
     * catalog. A typed id that already matches a preset *for that same
     * provider* is skipped, so retyping a known model doesn't duplicate its
     * row; one that happens to match some other provider's preset id is a
     * distinct model and stays listed.
     */
    fun available(
        unlocked: Set<Provider>,
        customModels: Map<Provider, String> = emptyMap(),
    ): List<VerbaModel> =
        all + customModels.entries.mapNotNull { (provider, id) ->
            id.takeIf { it.isNotBlank() && provider.isUnlocked(unlocked) && preset(provider, id) == null }
                ?.let { VerbaModel.custom(provider, it) }
        }

    private fun Provider.isUnlocked(unlocked: Set<Provider>): Boolean =
        this == Provider.OPENROUTER || onDevice || this in unlocked

    /** How many rows a key for [provider] adds to the model list. */
    fun countFor(provider: Provider): Int = byok.count { it.provider == provider }

    /**
     * The preset model with this exact [provider] and [id], or null if it isn't
     * one of ours. Both must match — a typed id that coincidentally equals
     * another provider's preset id is not that preset.
     */
    fun preset(provider: Provider, id: String): VerbaModel? =
        (all + byok).firstOrNull { it.provider == provider && it.id == id }

    fun byId(id: String): VerbaModel = (all + byok).firstOrNull { it.id == id } ?: default

    /**
     * True when a translation with this model draws on the developer's shared
     * key rather than the user's own — the bundled tier, and only while its
     * provider still has no personal key stored. A personal key always wins
     * the moment one exists, even for a bundled id (see
     * TranslationRepository.keyFor), so this is a request-time question, not
     * a fixed property of the model by itself.
     */
    fun usesSharedKey(model: VerbaModel, keyedProviders: Set<Provider>): Boolean =
        all.any { it.id == model.id } && model.provider !in keyedProviders
}
