import { writable, derived } from 'svelte/store';
import { browser } from '$app/environment';

// ---------------------------------------------------------------
// Types (mirrors Java DTOs)
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

export interface KafkaTopicInfo {
  name: string;
  partitions: number;
  replicationFactor: number;
}

export interface KafkaMetrics {
  status: 'UP' | 'DOWN';
  latencyMs?: number;
  message?: string;
  brokerCount: number;
  topicCount: number;
  topics: KafkaTopicInfo[];
}

export interface SparkWorkerInfo {
  id: string;
  host: string;
  port: number;
  cores: number;
  coresUsed: number;
  memoryMb: number;
  memoryUsedMb: number;
  state: string;
}

export interface SparkMetrics {
  status: 'UP' | 'DOWN';
  latencyMs?: number;
  message?: string;
  masterUrl?: string;
  aliveWorkers: number;
  totalCores: number;
  usedCores: number;
  totalMemoryMb: number;
  usedMemoryMb: number;
  activeApps: number;
  completedApps: number;
  workers: SparkWorkerInfo[];
}

export interface PostgresMetrics {
  status: 'UP' | 'DOWN';
  latencyMs?: number;
  message?: string;
  version?: string;
  activeConnections: number;
  maxConnections: number;
  dbSizeBytes: number;
  dbSizeHuman?: string;
  uptimeSeconds: number;
}

export interface DetailedHealthReport {
  overall: 'UP' | 'DEGRADED';
  timestamp: string;
  postgres: PostgresMetrics;
  kafka: KafkaMetrics;
  spark: SparkMetrics;
}

type ConnectionState = 'connecting' | 'connected' | 'error' | 'closed';

// ---------------------------------------------------------------
// Internal writable stores
// ---------------------------------------------------------------
const _report = writable<HealthReport | null>(null);
const _detailed = writable<DetailedHealthReport | null>(null);
const _connectionState = writable<ConnectionState>('connecting');
const _lastUpdated = writable<Date | null>(null);

// ---------------------------------------------------------------
// Public derived stores
// ---------------------------------------------------------------
export const healthReport = derived(_report, ($r) => $r);
export const detailedReport = derived(_detailed, ($r) => $r);
export const connectionState = derived(_connectionState, ($s) => $s);
export const lastUpdated = derived(_lastUpdated, ($d) => $d);

export const serviceMap = derived(_report, ($r): Record<string, ServiceHealth> => {
  if (!$r) return {};
  return Object.fromEntries($r.services.map((s) => [s.service, s]));
});

// ---------------------------------------------------------------
// SSE connections
// ---------------------------------------------------------------
const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8090';

let simpleEs: EventSource | null = null;
let detailEs: EventSource | null = null;
let simpleRetry: ReturnType<typeof setTimeout> | null = null;
let detailRetry: ReturnType<typeof setTimeout> | null = null;
let simpleDelay = 3_000;
let detailDelay = 3_000;

function connectSimple() {
  if (!browser) return;
  _connectionState.set('connecting');
  simpleEs = new EventSource(`${API_BASE}/api/health/stream`);

  simpleEs.addEventListener('health', (e: MessageEvent) => {
    try {
      _report.set(JSON.parse(e.data));
      _lastUpdated.set(new Date());
      _connectionState.set('connected');
      simpleDelay = 3_000;
    } catch { /* ignore parse errors */ }
  });

  simpleEs.onerror = () => {
    _connectionState.set('error');
    simpleEs?.close();
    simpleEs = null;
    simpleRetry = setTimeout(() => {
      simpleDelay = Math.min(simpleDelay * 2, 30_000);
      connectSimple();
    }, simpleDelay);
  };
}

function connectDetails() {
  if (!browser) return;
  detailEs = new EventSource(`${API_BASE}/api/health/details/stream`);

  detailEs.addEventListener('health-details', (e: MessageEvent) => {
    try {
      _detailed.set(JSON.parse(e.data));
      detailDelay = 3_000;
    } catch { /* ignore parse errors */ }
  });

  detailEs.onerror = () => {
    detailEs?.close();
    detailEs = null;
    detailRetry = setTimeout(() => {
      detailDelay = Math.min(detailDelay * 2, 30_000);
      connectDetails();
    }, detailDelay);
  };
}

function disconnect() {
  if (simpleRetry) clearTimeout(simpleRetry);
  if (detailRetry) clearTimeout(detailRetry);
  simpleEs?.close();
  detailEs?.close();
  simpleEs = detailEs = null;
  _connectionState.set('closed');
}

if (browser) {
  connectSimple();
  connectDetails();
}

export const healthStore = { disconnect };
