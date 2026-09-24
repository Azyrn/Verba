package com.skeler.verba.model

/** A read-aloud voice. Every voice speaks every language the TTS model supports. */
data class Voice(
    val id: String,
    val name: String,
    val female: Boolean,
)

/** Read-aloud paces offered in Settings; xAI accepts 0.7–1.5. */
object SpeechSpeeds {
    const val NORMAL = 1f
    val all: List<Float> = listOf(0.75f, NORMAL, 1.25f, 1.5f)

    fun closest(value: Float?): Float = value?.let { v -> all.minBy { kotlin.math.abs(it - v) } } ?: NORMAL
}

object Voices {

    /** xAI's built-in voices (GET /v1/tts/voices), alphabetical. */
    val all: List<Voice> = listOf(
        Voice("altair", "Altair", female = false),
        Voice("ara", "Ara", female = true),
        Voice("atlas", "Atlas", female = false),
        Voice("aurora", "Aurora", female = true),
        Voice("carina", "Carina", female = true),
        Voice("castor", "Castor", female = false),
        Voice("celeste", "Celeste", female = true),
        Voice("cosmo", "Cosmo", female = false),
        Voice("eve", "Eve", female = true),
        Voice("helios", "Helios", female = false),
        Voice("helix", "Helix", female = false),
        Voice("iris", "Iris", female = true),
        Voice("kepler", "Kepler", female = false),
        Voice("leo", "Leo", female = false),
        Voice("liora", "Liora", female = true),
        Voice("lumen", "Lumen", female = false),
        Voice("luna", "Luna", female = true),
        Voice("lux", "Lux", female = false),
        Voice("naksh", "Naksh", female = false),
        Voice("orion", "Orion", female = false),
        Voice("perseus", "Perseus", female = false),
        Voice("rex", "Rex", female = false),
        Voice("rigel", "Rigel", female = false),
        Voice("sal", "Sal", female = false),
        Voice("sirius", "Sirius", female = false),
        Voice("ursa", "Ursa", female = true),
        Voice("zagan", "Zagan", female = false),
        Voice("zenith", "Zenith", female = false),
    )

    /** xAI's own default — warm and clear. */
    val default: Voice = all.first { it.id == "eve" }

    fun byId(id: String?): Voice = all.firstOrNull { it.id == id } ?: default
}
