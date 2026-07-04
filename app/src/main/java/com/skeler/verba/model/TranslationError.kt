package com.skeler.verba.model

/** Everything that can go wrong between hitting send and getting a translation back. */
enum class TranslationError {
    /** No key in local.properties — the app can't talk to OpenRouter at all. */
    MISSING_KEY,

    /** OpenRouter answered 401/403: the configured key is wrong or revoked. */
    INVALID_KEY,

    /** The request never reached OpenRouter — offline, DNS, timeout. */
    NETWORK,

    /** 402/429: the free tier's per-minute or per-day cap was hit. */
    RATE_LIMITED,

    /** The chosen model is down, overloaded upstream, or was delisted. */
    MODEL_UNAVAILABLE,

    /** A 200 with no usable text in it — free providers do this under load. */
    EMPTY_RESPONSE,

    /** Offline engine can't handle this language pair — ML Kit covers a fixed set. */
    LANGUAGE_UNSUPPORTED,

    /** Anything else. */
    UNKNOWN,
}
