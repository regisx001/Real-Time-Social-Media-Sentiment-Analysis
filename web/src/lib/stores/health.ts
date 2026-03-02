import { writable, derived } from 'svelte/store';
import { browser } from '$app/environment';

// ---------------------------------------------------------------
// Types (mirrors the Java DTOs)
// ---------------------------------------------------------------
export interface ServiceHealth {
  service: string;
  status: 'UP' | 'DOWN';
  message?: string;
  latencyMs?: number;
}

export interface HealthReport {
  overall: 'UP' | 'DEGRADED';
  timestamp: string;
  services: ServiceHealth[];
}

type ConnectionState = 'connecting' | 'connected' | 'error' | 'closed';

// ---------------------------------------------------------------
// Internal writable stores
// ---------------------------------------------------------------
const _report = writable<HealthReport | null>(null);
const _connectionState = writable<ConnectionState>('connecting');
const _lastUpdated = writable<Date | null>(null);

// ---------------------------------------------------------------
// Public read-only derived stores
// ---------------------------------------------------------------
export const healthReport = derived(_report, ($r) => $r);
export const connectionState = derived(_connectionState, ($s) => $s);
export const lastUpdated = derived(_lastUpdated, ($d) => $d);

/** Map of service name → ServiceHealth for easy lookup in the template */
export const serviceMap = derived(_report, ($r): Record<string, ServiceHealth> => {
  if (!$r) return {};
  return Object.fromEntries($r.services.map((s) => [s.service, s]));
});

// ---------------------------------------------------------------
// SSE connection
// ---------------------------------------------------------------
const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8090';
const SSE_URL = `${API_BASE}/api/health/stream`;

let es: EventSource | null = null;
let retryTimer: ReturnType<typeof setTimeout> | null = null;
let retryDelay = 3_000; // starts at 3 s, caps at 30 s

function connect() {
  if (!browser) return;

  _connectionState.set('connecting');
  es = new EventSource(SSE_URL);

  es.addEventListener('health', (e: MessageEvent) => {
    try {
      const report: HealthReport = JSON.parse(e.data);
      _report.set(report);
      _lastUpdated.set(new Date());
      _connectionState.set('connected');
      retryDelay = 3_000; // reset back-off on success
    } catch {
      console.error('[health-store] Failed to parse SSE payload', e.data);
    }
  });

  es.onerror = () => {
    _connectionState.set('error');
    es?.close();
    es = null;
    // Exponential back-off, max 30 s
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
  _connectionState.set('closed');
}

// Auto-connect when the module is first imported in the browser
if (browser) {
  connect();
}

export const healthStore = { connect, disconnect };
