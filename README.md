# Verba

A simple AI translation app for Android, built with Material 3 Expressive design.

Verba translates text using large language models, so translations read naturally
instead of word-for-word. It ships with a free tier and works offline too.

## Features

- **Free tier** — a bundled set of OpenRouter free models, no key or sign-up needed.
- **Bring your own key** — plug in OpenAI, Anthropic, Google AI, or Mistral to
  unlock their models; the key stays on your device and is billed to you.
- **Offline translation** — on-device ML Kit engine works with no connection once
  a language pair is downloaded.
- **Model picker** — pick the model that fits, from a snappy small one to a large,
  more accurate one.
- **Themes** — system, light, dark, and true-black.
- **Clean, expressive UI** — Jetpack Compose + Material 3 Expressive.

## Install

Grab the latest signed APK from the [Releases](https://github.com/Azyrn/Verba/releases)
page and install it. `app-universal` runs on any device; the `arm64-v8a` and
`armeabi-v7a` builds are smaller if you know your device's architecture.

- Minimum Android: 7.0 (API 24)

## Build from source

```bash
git clone https://github.com/Azyrn/Verba.git
cd Verba
./gradlew assembleRelease
```

To use the bundled free tier during development, add your OpenRouter key to
`local.properties`:

```properties
openrouter.apiKey=sk-or-...
```

## Report bugs / request features

Open an issue here, or reach out on [Telegram](https://t.me/necotinx).

## License

Apache-2.0 — see [LICENSE](LICENSE).
