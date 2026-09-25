package com.skeler.verba.model

/**
 * Where a translation runs. [DEEPSEEK] goes through Verba's translation
 * Worker, which holds the key and the prompt; [MLKIT] runs on the device and
 * never touches the network ([onDevice]).
 */
enum class Provider(
    val displayName: String,
    val onDevice: Boolean = false,
) {
    DEEPSEEK(displayName = "DeepSeek"),
    MLKIT(displayName = "On-device", onDevice = true),
}
