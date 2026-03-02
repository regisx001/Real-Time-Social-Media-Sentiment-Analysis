<script lang="ts">
  import * as Card from "$lib/components/ui/card/index.js";
  import { Progress } from "$lib/components/ui/progress/index.js";
  import { Separator } from "$lib/components/ui/separator/index.js";
  import { Badge } from "$lib/components/ui/badge/index.js";
  import * as Table from "$lib/components/ui/table/index.js";
  import { ScrollArea } from "$lib/components/ui/scroll-area/index.js";

  import {
    Activity,
    Server,
    Database,
    Cpu,
    Layers,
    HardDrive,
    Users,
    Zap,
    Clock,
    CheckCircle,
    XCircle,
    RefreshCw,
    Network,
    MemoryStick,
    BarChart3,
    ArrowLeft,
  } from "@lucide/svelte";

  import {
    detailedReport,
    connectionState,
    lastUpdated,
  } from "$lib/stores/health";

  // ---------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------
  function fmtBytes(bytes: number): string {
    if (!bytes) return "0 B";
    const units = ["B", "KB", "MB", "GB", "TB"];
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return `${(bytes / Math.pow(1024, i)).toFixed(1)} ${units[i]}`;
  }

  function fmtUptime(seconds: number): string {
    if (!seconds) return "—";
    const d = Math.floor(seconds / 86400);
    const h = Math.floor((seconds % 86400) / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const parts = [];
    if (d) parts.push(`${d}d`);
    if (h) parts.push(`${h}h`);
    if (m) parts.push(`${m}m`);
    return parts.join(" ") || "<1m";
  }

  function fmtLatency(ms: number | undefined): string {
    return ms != null ? `${ms} ms` : "—";
  }

  function pct(used: number, total: number): number {
    if (!total) return 0;
    return Math.round((used / total) * 100);
  }

  function statusVariant(
    status: string,
  ): "default" | "destructive" | "secondary" | "outline" {
    return status === "UP"
      ? "default"
      : status === "DOWN"
        ? "destructive"
        : "secondary";
  }
</script>

<div class="min-h-screen bg-background text-foreground">
  <!-- Header -->
  <header
    class="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60"
  >
    <div class="flex h-14 items-center mx-auto max-w-7xl px-4 md:px-8 gap-4">
      <a
        href="/"
        class="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors"
      >
        <ArrowLeft class="h-4 w-4" />
        Dashboard
      </a>
      <Separator orientation="vertical" class="h-5" />
      <div class="flex items-center gap-2 font-semibold">
        <Server class="h-5 w-5" />
        Infrastructure Health
      </div>
      <div class="flex-1" />
      <div class="flex items-center gap-3">
        {#if $lastUpdated}
          <span class="text-xs text-muted-foreground hidden sm:inline">
            Updated {$lastUpdated.toLocaleTimeString()}
          </span>
        {/if}
        <div
          class="flex items-center gap-2 border px-2.5 py-1 rounded-full bg-muted/50"
        >
          {#if $connectionState === "connected"}
            <span class="relative flex h-2 w-2">
              <span
                class="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"
              ></span>
              <span
                class="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"
              ></span>
            </span>
            <span
              class="text-xs font-medium text-emerald-600 dark:text-emerald-400"
              >Live</span
            >
          {:else if $connectionState === "error"}
            <span class="relative inline-flex rounded-full h-2 w-2 bg-red-500"
            ></span>
            <span class="text-xs font-medium text-red-500">Reconnecting…</span>
          {:else}
            <RefreshCw class="h-3 w-3 animate-spin text-muted-foreground" />
            <span class="text-xs font-medium text-muted-foreground"
              >Connecting…</span
            >
          {/if}
        </div>
      </div>
    </div>
  </header>

  <main class="mx-auto max-w-7xl px-4 md:px-8 py-8 space-y-8">
    {#if !$detailedReport}
      <!-- Loading skeleton -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        {#each [1, 2, 3] as _}
          <Card.Root class="animate-pulse">
            <Card.Header
              ><div class="h-5 w-32 bg-muted rounded"></div></Card.Header
            >
            <Card.Content class="space-y-3">
              <div class="h-4 w-full bg-muted rounded"></div>
              <div class="h-4 w-3/4 bg-muted rounded"></div>
              <div class="h-4 w-1/2 bg-muted rounded"></div>
            </Card.Content>
          </Card.Root>
        {/each}
      </div>
    {:else}
      {@const r = $detailedReport}

      <!-- ─── Overall Status Banner ─────────────────────────────── -->
      <div
        class="flex items-center justify-between rounded-lg border px-5 py-4
               {r.overall === 'UP'
          ? 'bg-emerald-500/5 border-emerald-500/20'
          : 'bg-destructive/5 border-destructive/20'}"
      >
        <div class="flex items-center gap-3">
          {#if r.overall === "UP"}
            <CheckCircle class="h-6 w-6 text-emerald-500" />
            <div>
              <p class="font-semibold text-emerald-600 dark:text-emerald-400">
                All Systems Operational
              </p>
              <p class="text-xs text-muted-foreground">
                Every service is healthy and responding normally.
              </p>
            </div>
          {:else}
            <XCircle class="h-6 w-6 text-destructive" />
            <div>
              <p class="font-semibold text-destructive">System Degraded</p>
              <p class="text-xs text-muted-foreground">
                One or more services are experiencing issues.
              </p>
            </div>
          {/if}
        </div>
        <Badge variant={statusVariant(r.overall)} class="text-sm px-3 py-1"
          >{r.overall}</Badge
        >
      </div>

      <!-- ─── Three service cards (top row) ─────────────────────── -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <!-- PostgreSQL Card -->
        <Card.Root class="flex flex-col">
          <Card.Header class="flex flex-row items-center justify-between pb-3">
            <div class="flex items-center gap-2">
              <Database class="h-5 w-5 text-muted-foreground" />
              <Card.Title class="text-base">PostgreSQL 16</Card.Title>
            </div>
            <Badge variant={statusVariant(r.postgres.status)}
              >{r.postgres.status}</Badge
            >
          </Card.Header>
          <Card.Content class="flex-1 space-y-4 text-sm">
            {#if r.postgres.status === "DOWN"}
              <p class="text-destructive text-xs break-all">
                {r.postgres.message}
              </p>
            {:else}
              <!-- Latency -->
              <div class="flex justify-between">
                <span class="text-muted-foreground flex items-center gap-1.5"
                  ><Zap class="h-3.5 w-3.5" /> Latency</span
                >
                <span class="font-mono font-medium"
                  >{fmtLatency(r.postgres.latencyMs)}</span
                >
              </div>
              <!-- Connections -->
              <div class="space-y-1.5">
                <div class="flex justify-between">
                  <span class="text-muted-foreground flex items-center gap-1.5"
                    ><Users class="h-3.5 w-3.5" /> Connections</span
                  >
                  <span class="font-mono font-medium"
                    >{r.postgres.activeConnections} / {r.postgres
                      .maxConnections}</span
                  >
                </div>
                <Progress
                  value={pct(
                    r.postgres.activeConnections,
                    r.postgres.maxConnections,
                  )}
                  class="h-1.5"
                />
              </div>
              <!-- DB Size -->
              <div class="flex justify-between">
                <span class="text-muted-foreground flex items-center gap-1.5"
                  ><HardDrive class="h-3.5 w-3.5" /> DB Size</span
                >
                <span class="font-mono font-medium"
                  >{r.postgres.dbSizeHuman ??
                    fmtBytes(r.postgres.dbSizeBytes)}</span
                >
              </div>
              <!-- Uptime -->
              <div class="flex justify-between">
                <span class="text-muted-foreground flex items-center gap-1.5"
                  ><Clock class="h-3.5 w-3.5" /> Uptime</span
                >
                <span class="font-mono font-medium"
                  >{fmtUptime(r.postgres.uptimeSeconds)}</span
                >
              </div>
              <Separator />
              <!-- Version -->
              <p
                class="text-xs text-muted-foreground break-all leading-relaxed"
              >
                {r.postgres.version ?? "—"}
              </p>
            {/if}
          </Card.Content>
        </Card.Root>

        <!-- Kafka Card -->
        <Card.Root class="flex flex-col">
          <Card.Header class="flex flex-row items-center justify-between pb-3">
            <div class="flex items-center gap-2">
              <Network class="h-5 w-5 text-muted-foreground" />
              <Card.Title class="text-base">Apache Kafka</Card.Title>
            </div>
            <Badge variant={statusVariant(r.kafka.status)}
              >{r.kafka.status}</Badge
            >
          </Card.Header>
          <Card.Content class="flex-1 space-y-4 text-sm">
            {#if r.kafka.status === "DOWN"}
              <p class="text-destructive text-xs break-all">
                {r.kafka.message}
              </p>
            {:else}
              <div class="flex justify-between">
                <span class="text-muted-foreground flex items-center gap-1.5"
                  ><Zap class="h-3.5 w-3.5" /> Latency</span
                >
                <span class="font-mono font-medium"
                  >{fmtLatency(r.kafka.latencyMs)}</span
                >
              </div>
              <div class="flex justify-between">
                <span class="text-muted-foreground flex items-center gap-1.5"
                  ><Server class="h-3.5 w-3.5" /> Brokers</span
                >
                <span class="font-mono font-medium">{r.kafka.brokerCount}</span>
              </div>
              <div class="flex justify-between">
                <span class="text-muted-foreground flex items-center gap-1.5"
                  ><Layers class="h-3.5 w-3.5" /> Topics</span
                >
                <span class="font-mono font-medium">{r.kafka.topicCount}</span>
              </div>
              <Separator />
              <!-- Topics table -->
              <p
                class="text-xs font-semibold text-muted-foreground uppercase tracking-wide"
              >
                Topic Details
              </p>
              <Table.Root>
                <Table.Header>
                  <Table.Row>
                    <Table.Head class="text-xs">Name</Table.Head>
                    <Table.Head class="text-xs text-right"
                      >Partitions</Table.Head
                    >
                    <Table.Head class="text-xs text-right">RF</Table.Head>
                  </Table.Row>
                </Table.Header>
                <Table.Body>
                  {#each r.kafka.topics as t}
                    <Table.Row>
                      <Table.Cell class="font-mono text-xs">{t.name}</Table.Cell
                      >
                      <Table.Cell class="text-right text-xs"
                        >{t.partitions}</Table.Cell
                      >
                      <Table.Cell class="text-right text-xs"
                        >{t.replicationFactor}</Table.Cell
                      >
                    </Table.Row>
                  {/each}
                </Table.Body>
              </Table.Root>
            {/if}
          </Card.Content>
        </Card.Root>

        <!-- Spark Card -->
        <Card.Root class="flex flex-col">
          <Card.Header class="flex flex-row items-center justify-between pb-3">
            <div class="flex items-center gap-2">
              <Cpu class="h-5 w-5 text-muted-foreground" />
              <Card.Title class="text-base">Apache Spark</Card.Title>
            </div>
            <Badge variant={statusVariant(r.spark.status)}
              >{r.spark.status}</Badge
            >
          </Card.Header>
          <Card.Content class="flex-1 space-y-4 text-sm">
            {#if r.spark.status === "DOWN"}
              <p class="text-destructive text-xs break-all">
                {r.spark.message}
              </p>
            {:else}
              <div class="flex justify-between">
                <span class="text-muted-foreground flex items-center gap-1.5"
                  ><Zap class="h-3.5 w-3.5" /> Latency</span
                >
                <span class="font-mono font-medium"
                  >{fmtLatency(r.spark.latencyMs)}</span
                >
              </div>
              <div class="flex justify-between">
                <span class="text-muted-foreground flex items-center gap-1.5"
                  ><Server class="h-3.5 w-3.5" /> Workers</span
                >
                <span class="font-mono font-medium">{r.spark.aliveWorkers}</span
                >
              </div>
              <!-- CPU -->
              <div class="space-y-1.5">
                <div class="flex justify-between">
                  <span class="text-muted-foreground flex items-center gap-1.5"
                    ><Cpu class="h-3.5 w-3.5" /> CPU Cores</span
                  >
                  <span class="font-mono font-medium"
                    >{r.spark.usedCores} / {r.spark.totalCores}</span
                  >
                </div>
                <Progress
                  value={pct(r.spark.usedCores, r.spark.totalCores)}
                  class="h-1.5"
                />
              </div>
              <!-- Memory -->
              <div class="space-y-1.5">
                <div class="flex justify-between">
                  <span class="text-muted-foreground flex items-center gap-1.5"
                    ><MemoryStick class="h-3.5 w-3.5" /> Memory</span
                  >
                  <span class="font-mono font-medium"
                    >{r.spark.usedMemoryMb} / {r.spark.totalMemoryMb} MB</span
                  >
                </div>
                <Progress
                  value={pct(r.spark.usedMemoryMb, r.spark.totalMemoryMb)}
                  class="h-1.5"
                />
              </div>
              <div class="flex justify-between">
                <span class="text-muted-foreground flex items-center gap-1.5"
                  ><BarChart3 class="h-3.5 w-3.5" /> Active Apps</span
                >
                <span class="font-mono font-medium">{r.spark.activeApps}</span>
              </div>
              <div class="flex justify-between">
                <span class="text-muted-foreground flex items-center gap-1.5"
                  ><Activity class="h-3.5 w-3.5" /> Completed Apps</span
                >
                <span class="font-mono font-medium"
                  >{r.spark.completedApps}</span
                >
              </div>
            {/if}
          </Card.Content>
        </Card.Root>
      </div>

      <!-- ─── Spark Workers detail ────────────────────────────────── -->
      {#if r.spark.status === "UP" && r.spark.workers.length > 0}
        <Card.Root>
          <Card.Header>
            <Card.Title class="flex items-center gap-2">
              <Cpu class="h-5 w-5 text-muted-foreground" /> Spark Workers
            </Card.Title>
            <Card.Description
              >{r.spark.aliveWorkers} worker{r.spark.aliveWorkers !== 1
                ? "s"
                : ""} registered to master</Card.Description
            >
          </Card.Header>
          <Card.Content class="p-0">
            <ScrollArea class="w-full">
              <Table.Root>
                <Table.Header>
                  <Table.Row>
                    <Table.Head>Host</Table.Head>
                    <Table.Head class="text-right">Port</Table.Head>
                    <Table.Head class="text-right">Cores Used</Table.Head>
                    <Table.Head>CPU</Table.Head>
                    <Table.Head class="text-right">Mem Used (MB)</Table.Head>
                    <Table.Head>Memory</Table.Head>
                    <Table.Head class="text-right">State</Table.Head>
                  </Table.Row>
                </Table.Header>
                <Table.Body>
                  {#each r.spark.workers as w}
                    <Table.Row>
                      <Table.Cell class="font-mono text-sm">{w.host}</Table.Cell
                      >
                      <Table.Cell class="text-right font-mono text-sm"
                        >{w.port}</Table.Cell
                      >
                      <Table.Cell class="text-right font-mono text-sm"
                        >{w.coresUsed} / {w.cores}</Table.Cell
                      >
                      <Table.Cell class="min-w-24">
                        <Progress
                          value={pct(w.coresUsed, w.cores)}
                          class="h-1.5"
                        />
                      </Table.Cell>
                      <Table.Cell class="text-right font-mono text-sm"
                        >{w.memoryUsedMb} / {w.memoryMb}</Table.Cell
                      >
                      <Table.Cell class="min-w-24">
                        <Progress
                          value={pct(w.memoryUsedMb, w.memoryMb)}
                          class="h-1.5"
                        />
                      </Table.Cell>
                      <Table.Cell class="text-right">
                        <Badge
                          variant={w.state === "ALIVE"
                            ? "default"
                            : "destructive"}
                          class="text-xs"
                        >
                          {w.state}
                        </Badge>
                      </Table.Cell>
                    </Table.Row>
                  {/each}
                </Table.Body>
              </Table.Root>
            </ScrollArea>
          </Card.Content>
        </Card.Root>
      {/if}

      <!-- ─── Kafka topics detail (full list) ───────────────────── -->
      {#if r.kafka.status === "UP" && r.kafka.topics.length > 0}
        <Card.Root>
          <Card.Header>
            <Card.Title class="flex items-center gap-2">
              <Network class="h-5 w-5 text-muted-foreground" /> Kafka Topics
            </Card.Title>
            <Card.Description
              >{r.kafka.topicCount} topics across {r.kafka.brokerCount} broker{r
                .kafka.brokerCount !== 1
                ? "s"
                : ""}</Card.Description
            >
          </Card.Header>
          <Card.Content class="p-0">
            <Table.Root>
              <Table.Header>
                <Table.Row>
                  <Table.Head>Topic Name</Table.Head>
                  <Table.Head class="text-right">Partitions</Table.Head>
                  <Table.Head class="text-right">Replication Factor</Table.Head>
                </Table.Row>
              </Table.Header>
              <Table.Body>
                {#each r.kafka.topics as t}
                  <Table.Row>
                    <Table.Cell class="font-mono">{t.name}</Table.Cell>
                    <Table.Cell class="text-right">{t.partitions}</Table.Cell>
                    <Table.Cell class="text-right"
                      >{t.replicationFactor}</Table.Cell
                    >
                  </Table.Row>
                {/each}
              </Table.Body>
            </Table.Root>
          </Card.Content>
        </Card.Root>
      {/if}
    {/if}
  </main>
</div>
