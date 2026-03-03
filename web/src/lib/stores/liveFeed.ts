import { writable, derived } from 'svelte/store';
import { browser } from '$app/environment';

// ---------------------------------------------------------------
// Types
// ---------------------------------------------------------------
export interface LiveTweet {
  id: number;
  text: string;
  sentiment: 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL' | 'UNKNOWN';
  score: number;
  processedAt: string; // ISO-8601
}

// ---------------------------------------------------------------
// Internal writables
// ---------------------------------------------------------------
const _feed = writable<LiveTweet[]>([]);

// ---------------------------------------------------------------
// Public derived store
// ---------------------------------------------------------------
export const liveFeed = derived(_feed, ($f) => $f);

// ---------------------------------------------------------------
// SSE connection
// ---------------------------------------------------------------
const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8090';

let es: EventSource | null = null;
let retryTimer: ReturnType<typeof setTimeout> | null = null;
let retryDelay = 3_000;

function connect() {
  if (!browser) return;
  es = new EventSource(`${API_BASE}/api/analytics/live-feed/stream`);

  es.addEventListener('live-feed', (e: MessageEvent) => {
    try {
      const tweets = JSON.parse(e.data) as LiveTweet[];
      _feed.set(tweets);
      retryDelay = 3_000;
    } catch {
      /* ignore parse errors */
    }
  });

  es.onerror = () => {
    es?.close();
    es = null;
    retryTimer = setTimeout(() => {
      retryDelay = Math.min(retryDelay * 2, 30_000);
      connect();
    }, retryDelay);
  };
}

function disconnect() {
  if (retryTimer) clearTimeout(retryTimer);
  es?.close();
  es = null;
}

if (browser) {
  connect();
}

export const liveFeedStore = { disconnect };
