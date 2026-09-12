import { describe, expect, it } from "vitest";

import { TtlCache } from "./ttl-cache";

describe("TtlCache", () => {
  it("calls the loader once for repeated hits inside the TTL", async () => {
    const cache = new TtlCache();
    let calls = 0;

    const load = async () => {
      calls += 1;
      return "value";
    };

    expect(await cache.resolve("k", 1000, load)).toBe("value");
    expect(await cache.resolve("k", 1000, load)).toBe("value");
    expect(calls).toBe(1);
  });

  it("shares one in-flight request between concurrent callers", async () => {
    const cache = new TtlCache();
    let calls = 0;

    const load = async () => {
      calls += 1;
      await new Promise((resolve) => setTimeout(resolve, 5));
      return calls;
    };

    const [a, b, c] = await Promise.all([
      cache.resolve("k", 1000, load),
      cache.resolve("k", 1000, load),
      cache.resolve("k", 1000, load),
    ]);

    expect(a).toBe(1);
    expect(b).toBe(1);
    expect(c).toBe(1);
    expect(calls).toBe(1);
  });
});
