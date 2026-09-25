package com.skeler.verba.model

/** Everything that can go wrong between hitting send and getting a translation back. */
enum class TranslationError {
    /** This build has no translation Worker URL or token configured. */
    NOT_CONFIGURED,

    /** The Worker rejected the app's token — this build is too old or tampered with. */
    UNAUTHORIZED,

    /** The request never reached the Worker — offline, DNS, timeout. */
    NETWORK,

    /** 429: too many requests from this device, or the upstream is throttling. */
    RATE_LIMITED,

    /** The Worker reached DeepSeek but it failed or didn't answer in time. */
    MODEL_UNAVAILABLE,

    /** 413: more text than one request may carry. */
    TEXT_TOO_LONG,

    /** A 200 with no usable text in it. */
    EMPTY_RESPONSE,

    /** Offline engine can't handle this language pair — ML Kit covers a fixed set. */
    LANGUAGE_UNSUPPORTED,

    /** Anything else. */
    UNKNOWN,
    ;

    companion object {
        /** Status codes as worker/src/index.ts sends them. */
        fun fromStatus(code: Int): TranslationError = when (code) {
            401 -> UNAUTHORIZED
            413 -> TEXT_TOO_LONG
            429 -> RATE_LIMITED
            502, 504 -> MODEL_UNAVAILABLE
            else -> UNKNOWN
        }
    }
}
