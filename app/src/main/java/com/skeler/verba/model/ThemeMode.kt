package com.skeler.verba.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    TRUE_BLACK;

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}
