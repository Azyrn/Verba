import { cloudflareTest } from "@cloudflare/vitest-plugin";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [
    cloudflareTest({
      wrangler: { configPath: "./wrangler.jsonc" },
      // Fixed stand-ins so tests never read .dev.vars or reach a paid upstream.
      miniflare: {
        bindings: { APP_TOKEN: "test-token", FIREWORKS_API_KEY: "unused", XAI_API_KEY: "unused" },
      },
    }),
  ],
});
