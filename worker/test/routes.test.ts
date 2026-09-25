import { exports } from "cloudflare:workers";
import { describe, expect, it } from "vitest";

const auth = { Authorization: "Bearer test-token" };

function post(path: string, body: unknown, headers: Record<string, string> = auth) {
  return exports.default.fetch(`https://verba.test${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...headers },
    body: typeof body === "string" ? body : JSON.stringify(body),
  });
}

describe("existing routes", () => {
  it("rejects a missing or wrong app token", async () => {
    expect((await post("/", { text: "hi", target: "French" }, {})).status).toBe(401);
    const wrong = await post("/", { text: "hi", target: "French" }, { Authorization: "Bearer nope-token" });
    expect(wrong.status).toBe(401);
    expect(await wrong.json()).toEqual({ error: "unauthorized" });
  });

  it("rejects the wrong method", async () => {
    const res = await exports.default.fetch("https://verba.test/", { headers: auth });
    expect(res.status).toBe(405);
  });

  it("validates translate bodies before calling the model", async () => {
    expect((await post("/", "not json")).status).toBe(400);
    expect((await post("/", { text: "hi", target: "Fr3nch!" })).status).toBe(400);
    expect((await post("/", { text: "x".repeat(10_001), target: "French" })).status).toBe(413);
  });

  it("validates tts bodies before calling xAI", async () => {
    expect((await post("/tts", { text: "hi", voice: "Eve!" })).status).toBe(400);
    expect((await post("/tts", { text: "hi", voice: "eve", speed: 3 })).status).toBe(400);
    expect((await post("/tts", { text: "x".repeat(5_001), voice: "eve" })).status).toBe(413);
  });

  it("answers unknown paths with 404", async () => {
    expect((await post("/nope", {})).status).toBe(404);
  });
});
