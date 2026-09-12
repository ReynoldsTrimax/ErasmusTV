"use client";

import * as React from "react";

import { watchlistKey } from "@/lib/watchlist/keys";

interface WatchlistContextValue {
  has: (mediaType: "movie" | "tv", id: string) => boolean;
  setSaved: (mediaType: "movie" | "tv", id: string, saved: boolean) => void;
}

const WatchlistContext = React.createContext<WatchlistContextValue>({
  has: () => false,
  setSaved: () => {},
});

export function WatchlistProvider({
  initialKeys,
  children,
}: {
  initialKeys: string[];
  children: React.ReactNode;
}) {
  const [keys, setKeys] = React.useState(() => new Set(initialKeys));

  React.useEffect(() => {
    setKeys(new Set(initialKeys));
  }, [initialKeys]);

  const value = React.useMemo<WatchlistContextValue>(
    () => ({
      has: (mediaType, id) => keys.has(watchlistKey(mediaType, id)),
      setSaved: (mediaType, id, saved) => {
        setKeys((prev) => {
          const next = new Set(prev);
          const key = watchlistKey(mediaType, id);
          if (saved) next.add(key);
          else next.delete(key);
          return next;
        });
      },
    }),
    [keys],
  );

  return (
    <WatchlistContext.Provider value={value}>{children}</WatchlistContext.Provider>
  );
}

export function useWatchlist() {
  return React.useContext(WatchlistContext);
}
