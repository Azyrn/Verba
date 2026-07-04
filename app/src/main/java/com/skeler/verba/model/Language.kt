package com.skeler.verba.model

/**
 * A language Verba can translate between. [name] is the English exonym used in
 * the pair chip and prompts; [nativeName] is the endonym shown prominently in
 * the picker so speakers can find their own language at a glance.
 */
data class Language(
    val code: String,
    val name: String,
    val nativeName: String,
) {
    val isAuto: Boolean get() = code == AUTO_CODE

    companion object {
        const val AUTO_CODE = "auto"

        val Auto = Language(AUTO_CODE, "Detected language", "Detect language")
    }
}

enum class LanguageSide { SOURCE, TARGET }

data class LanguagePair(
    val source: Language,
    val target: Language,
) {
    fun swapped(): LanguagePair = LanguagePair(source = target, target = source)

    companion object {
        val Default = LanguagePair(
            source = Languages.byCode("en"),
            target = Languages.byCode("es"),
        )
    }
}

object Languages {

    val all: List<Language> = listOf(
        Language("ar", "Arabic", "العربية"),
        Language("bn", "Bengali", "বাংলা"),
        Language("bg", "Bulgarian", "Български"),
        Language("ca", "Catalan", "Català"),
        Language("zh", "Chinese", "中文（简体）"),
        Language("zh-TW", "Chinese, Traditional", "中文（繁體）"),
        Language("hr", "Croatian", "Hrvatski"),
        Language("cs", "Czech", "Čeština"),
        Language("da", "Danish", "Dansk"),
        Language("nl", "Dutch", "Nederlands"),
        Language("en", "English", "English"),
        Language("et", "Estonian", "Eesti"),
        Language("fi", "Finnish", "Suomi"),
        Language("fr", "French", "Français"),
        Language("de", "German", "Deutsch"),
        Language("el", "Greek", "Ελληνικά"),
        Language("he", "Hebrew", "עברית"),
        Language("hi", "Hindi", "हिन्दी"),
        Language("hu", "Hungarian", "Magyar"),
        Language("id", "Indonesian", "Bahasa Indonesia"),
        Language("it", "Italian", "Italiano"),
        Language("ja", "Japanese", "日本語"),
        Language("ko", "Korean", "한국어"),
        Language("lv", "Latvian", "Latviešu"),
        Language("lt", "Lithuanian", "Lietuvių"),
        Language("ms", "Malay", "Bahasa Melayu"),
        Language("no", "Norwegian", "Norsk"),
        Language("fa", "Persian", "فارسی"),
        Language("pl", "Polish", "Polski"),
        Language("pt", "Portuguese", "Português"),
        Language("ro", "Romanian", "Română"),
        Language("ru", "Russian", "Русский"),
        Language("sr", "Serbian", "Српски"),
        Language("sk", "Slovak", "Slovenčina"),
        Language("sl", "Slovenian", "Slovenščina"),
        Language("es", "Spanish", "Español"),
        Language("sw", "Swahili", "Kiswahili"),
        Language("sv", "Swedish", "Svenska"),
        Language("th", "Thai", "ไทย"),
        Language("tr", "Turkish", "Türkçe"),
        Language("uk", "Ukrainian", "Українська"),
        Language("ur", "Urdu", "اردو"),
        Language("vi", "Vietnamese", "Tiếng Việt"),
    )

    fun byCode(code: String): Language =
        if (code == Language.AUTO_CODE) Language.Auto
        else all.firstOrNull { it.code == code } ?: all.first { it.code == "en" }

    fun search(query: String): List<Language> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return all
        return all.filter {
            it.name.contains(trimmed, ignoreCase = true) ||
                it.nativeName.contains(trimmed, ignoreCase = true)
        }
    }
}
