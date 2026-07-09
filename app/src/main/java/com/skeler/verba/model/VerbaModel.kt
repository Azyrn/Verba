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

    /** Bring-your-own-key models, listed only once their provider has a key. */
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
     * One flat list: the free tier, the preset models the user's keys unlock,
     * then any model ids they typed in themselves for an unlocked provider.
     */
    fun available(
        unlocked: Set<Provider>,
        customModels: Map<Provider, String> = emptyMap(),
    ): List<VerbaModel> =
        all + byok.filter { it.provider in unlocked } +
            customModels.entries.mapNotNull { (provider, id) ->
                id.takeIf { it.isNotBlank() && provider.isUnlocked(unlocked) }
                    ?.let { VerbaModel.custom(provider, it) }
            }

    private fun Provider.isUnlocked(unlocked: Set<Provider>): Boolean =
        this == Provider.OPENROUTER || onDevice || this in unlocked

    /** How many rows a key for [provider] adds to the model list. */
    fun countFor(provider: Provider): Int = byok.count { it.provider == provider }

    /** The preset model with [id], or null if it isn't one of ours. */
    fun preset(id: String): VerbaModel? = (all + byok).firstOrNull { it.id == id }

    fun byId(id: String): VerbaModel = preset(id) ?: default
}
