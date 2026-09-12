/**
 * Process-level TTL cache with in-flight request sharing.
 */

interface CacheEntry<T> {
  value: T;
  expiresAt: number;
}

const DEFAULT_MAX_ENTRIES = 500;

export class TtlCache {
  private readonly entries = new Map<string, CacheEntry<unknown>>();
  private readonly inFlight = new Map<string, Promise<unknown>>();

  constructor(private readonly maxEntries: number = DEFAULT_MAX_ENTRIES) {}

  async resolve<T>(key: string, ttlMs: number, load: () => Promise<T>): Promise<T> {
    const hit = this.entries.get(key);
    if (hit && hit.expiresAt > Date.now()) {
      this.entries.delete(key);
      this.entries.set(key, hit);
      return hit.value as T;
    }

    const pending = this.inFlight.get(key);
    if (pending) return pending as Promise<T>;

    const promise = load()
      .then((value) => {
        this.set(key, value, ttlMs);
        return value;
      })
      .finally(() => {
        this.inFlight.delete(key);
      });

    this.inFlight.set(key, promise);
    return promise;
  }

  private set<T>(key: string, value: T, ttlMs: number): void {
    if (this.entries.size >= this.maxEntries) {
      const oldest = this.entries.keys().next();
      if (!oldest.done) this.entries.delete(oldest.value);
    }
    this.entries.set(key, { value, expiresAt: Date.now() + ttlMs });
  }

  size(): number {
    return this.entries.size;
  }

  clear(): void {
    this.entries.clear();
    this.inFlight.clear();
  }
}
