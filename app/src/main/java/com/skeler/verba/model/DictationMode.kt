package com.skeler.verba.model

/** How speech becomes text: all at once after you stop, or word by word while you talk. */
enum class DictationMode {
    BATCH,
    LIVE;

    companion object {
        val default = BATCH

        fun fromName(name: String?): DictationMode =
            entries.firstOrNull { it.name == name } ?: default
    }
}
