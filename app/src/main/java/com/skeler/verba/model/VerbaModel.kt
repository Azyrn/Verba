package com.skeler.verba.model

import androidx.annotation.StringRes
import com.skeler.verba.R

/**
 * A translation engine Verba can use. [description] is an honest one-line
 * account of the trade-off, not marketing.
 */
data class VerbaModel(
    val id: String,
    val name: String,
    @param:StringRes val description: Int,
    val provider: Provider,
)

object VerbaModels {

    val online = VerbaModel(
        id = "deepseek-flash",
        name = "Online",
        description = R.string.model_online,
        provider = Provider.DEEPSEEK,
    )

    val offline = VerbaModel(
        id = "mlkit/on-device",
        name = "Offline",
        description = R.string.model_offline,
        provider = Provider.MLKIT,
    )

    val all: List<VerbaModel> = listOf(online, offline)

    val default: VerbaModel = online

    /** The engine with this id; anything unknown (e.g. a retired model) is the default. */
    fun byId(id: String?): VerbaModel = all.firstOrNull { it.id == id } ?: default
}
