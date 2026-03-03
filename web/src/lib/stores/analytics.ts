import { writable, derived } from 'svelte/store';
import { browser } from '$app/environment';

// ---------------------------------------------------------------
// Types (mirrors Java DTOs)
// ---------------------------------------------------------------
export interface AnalyticsSummary {
  totalProcessed: number;
  positive: number;
  negative: number;
  neutral: number;
  throughputPerSec: number;
}

export interface SentimentTimePoint {
  time: string; // ISO-8601 local date-time
  positive: number;
  negative: number;
  neutral: number;
}

export interface AnalyticsReport {
  summary: AnalyticsSummary;
  timeSeries: SentimentTimePoint[];
}

type ConnectionState = 'connecting' | 'connected' | 'error' | 'closed';

// ---------------------------------------------------------------
// Internal writables
// ---------------------------------------------------------------
const _report = writable<AnalyticsReport | null>(null);
const _connectionState = writable<ConnectionState>('connecting');
const _lastUpdated = writable<Date | null>(null);

// ---------------------------------------------------------------
// Public derived stores
// ---------------------------------------------------------------
export const analyticsReport = derived(_report, ($r) => $r);
export const analyticsConnectionState = derived(_connectionState, ($s) => $s);
export const analyticsLastUpdated = derived(_lastUpdated, ($d) => $d);

// ---------------------------------------------------------------
// SSE connection
// ---------------------------------------------------------------
const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8090';

let es: EventSource | null = null;
let retryTimer: ReturnType<typeof setTimeout> | null = null;
let retryDelay = 3_000;

// Track current params so reconnect can reuse them
let _currentBucket = 'minute';
let _currentMinutes = 60;

function connect(bucket = _currentBucket, minutes = _currentMinutes) {
  if (!browser) return;
  _currentBucket = bucket;
  _currentMinutes = minutes;
  _connectionState.set('connecting');
  es = new EventSource(`${API_BASE}/api/analytics/stream?bucket=${bucket}&minutes=${minutes}`);

  es.addEventListener('analytics', (e: MessageEvent) => {
    try {
      _report.set(JSON.parse(e.data) as AnalyticsReport);
      _lastUpdated.set(new Date());
      _connectionState.set('connected');
      retryDelay = 3_000;
    } catch {
      /* ignore parse errors */
    }
  });

  es.onerror = () => {
    _connectionState.set('error');
    es?.close();
    es = null;
    retryTimer = setTimeout(() => {
      retryDelay = Math.min(retryDelay * 2, 30_000);
      connect(bucket, minutes);
    }, retryDelay);
  };
}

function disconnect() {
  if (retryTimer) clearTimeout(retryTimer);
  es?.close();
  es = null;
  _connectionState.set('closed');
}

export function reconnectAnalytics(bucket: string, minutes: number) {
  disconnect();
  retryDelay = 3_000;
  connect(bucket, minutes);
}

if (browser) {
  connect();
}

export const analyticsStore = { disconnect };
