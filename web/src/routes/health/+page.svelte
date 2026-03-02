<script lang="ts">
  import * as Sidebar from "$lib/components/ui/sidebar/index.js";
  import * as Card from "$lib/components/ui/card/index.js";
  import { Separator } from "$lib/components/ui/separator/index.js";
  import { Badge } from "$lib/components/ui/badge/index.js";
  import * as Table from "$lib/components/ui/table/index.js";
  import { ScrollArea } from "$lib/components/ui/scroll-area/index.js";
  import * as Collapsible from "$lib/components/ui/collapsible/index.js";

  import SentimentAppSidebar from "$lib/components/sentiment-app-sidebar.svelte";

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
    CheckCircle2,
    XCircle,
    Network,
    MemoryStick,
    BarChart3,
    ChevronDown,
    RefreshCw,
    Wifi,
    WifiOff,
    AlertCircle,
    Circle,
  } from "@lucide/svelte";

  import { TrendingUp, TrendingDown } from "@lucide/svelte";

  import {
    detailedReport,
    connectionState,
    lastUpdated,
  } from "$lib/stores/health";

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
    const parts: string[] = [];
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

  function latencyColor(ms: number | undefined): string {
    if (ms == null) return "text-muted-foreground";
    if (ms < 10) return "text-emerald-500";
    if (ms < 50) return "text-amber-500";
    return "text-red-500";
  }

  function usageColor(p: number): string {
    if (p < 60) return "bg-emerald-500";
    if (p < 85) return "bg-amber-500";
    return "bg-red-500";
  }

  let sparkOpen = $state(true);
  let kafkaOpen = $state(true);
</script>

<Sidebar.Provider
  style="--sidebar-width: calc(var(--spacing) * 72); --header-height: calc(var(--spacing) * 12);"
>
  <SentimentAppSidebar variant="inset" />
  <Sidebar.Inset>

    <!-- ══ Header ═══════════════════════════════════════════════ -->
    <header
      class="sticky top-0 z-20 flex h-(--header-height) shrink-0 items-center gap-2 border-b bg-background/95 backdrop-blur-sm transition-[width,height] ease-linear group-has-data-[collapsible=icon]/sidebar-wrapper:h-(--header-height)"
    >
      <div class="flex w-full items-center gap-1 px-4 lg:gap-2 lg:px-6">
        <Sidebar.Trigger class="-ms-1" />
        <Separator orientation="vertical" class="mx-2 data-[orientation=vertical]:h-4" />

        <div class="flex items-center gap-2.5">
          <div class="flex h-7 w-7 items-center justify-center rounded-lg bg-primary/10 ring-1 ring-primary/20">
            <Server class="h-3.5 w-3.5 text-primary" />
          </div>
          <div>
            <h1 class="text-sm font-semibold leading-none tracking-tight">Infrastructure Health</h1>
            <p class="text-[11px] text-muted-foreground leading-none mt-0.5">Real-time service monitoring</p>
          </div>
        </div>

        <div class="ms-auto flex items-center gap-2">
          {#if $lastUpdated}
            <div class="hidden items-center gap-1.5 rounded-md border bg-muted/50 px-2.5 py-1 text-[11px] text-muted-foreground sm:flex">
              <RefreshCw class="h-3 w-3" />
              <span>Updated {$lastUpdated.toLocaleTimeString()}</span>
            </div>
          {/if}

          {#if $connectionState === "connected"}
            <div class="flex items-center gap-1.5 rounded-full border border-emerald-500/30 bg-emerald-500/10 px-3 py-1 text-xs font-medium text-emerald-600 dark:text-emerald-400">
              <span class="relative flex h-1.5 w-1.5">
                <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                <span class="relative inline-flex h-1.5 w-1.5 rounded-full bg-emerald-500"></span>
              </span>
              Live
            </div>
          {:else if $connectionState === "error"}
            <div class="flex items-center gap-1.5 rounded-full border border-red-500/30 bg-red-500/10 px-3 py-1 text-xs font-medium text-red-500">
              <WifiOff class="h-3 w-3" />
              Reconnecting
            </div>
          {:else}
            <div class="flex items-center gap-1.5 rounded-full border border-amber-500/30 bg-amber-500/10 px-3 py-1 text-xs font-medium text-amber-600 dark:text-amber-400">
              <Wifi class="h-3 w-3 animate-pulse" />
              Connecting
            </div>
          {/if}
        </div>
      </div>
    </header>

    <!-- ══ Body ══════════════════════════════════════════════════ -->
    <div class="@container/main flex flex-1 flex-col overflow-auto">
      <div class="flex flex-col gap-6 py-6 px-4 lg:px-6">

        <!-- ── Loading state ──────────────────────────────────── -->
        {#if !$detailedReport}
          <div class="h-20 rounded-xl border bg-muted/30 animate-pulse"></div>
          <div class="grid grid-cols-1 gap-4 @xl/main:grid-cols-2 @5xl/main:grid-cols-4">
            {#each [1, 2, 3, 4] as _}
              <div class="h-36 rounded-xl border bg-muted/30 animate-pulse"></div>
            {/each}
          </div>
          <div class="grid grid-cols-1 gap-4 @xl/main:grid-cols-3">
            {#each [1, 2, 3] as _}
              <div class="h-72 rounded-xl border bg-muted/30 animate-pulse"></div>
            {/each}
          </div>

        {:else}
          {@const r = $detailedReport}

          <!-- ── Status banner ───────────────────────────────── -->
          <div
            class="relative flex items-center justify-between overflow-hidden rounded-xl border px-5 py-4
                   {r.overall === 'UP' ? 'border-emerald-500/20 bg-emerald-500/5' : 'border-red-500/20 bg-red-500/5'}"
          >
            <div
              class="pointer-events-none absolute inset-0 {r.overall === 'UP'
                ? 'bg-gradient-to-r from-emerald-500/8 via-transparent to-transparent'
                : 'bg-gradient-to-r from-red-500/8 via-transparent to-transparent'}"
            ></div>

            <div class="relative flex items-center gap-4">
              <div
                class="flex h-11 w-11 shrink-0 items-center justify-center rounded-full ring-1
                       {r.overall === 'UP'
                  ? 'bg-emerald-500/15 ring-emerald-500/20'
                  : 'bg-red-500/15 ring-red-500/20'}"
              >
                {#if r.overall === "UP"}
                  <CheckCircle2 class="h-5 w-5 text-emerald-500" />
                {:else}
                  <AlertCircle class="h-5 w-5 text-red-500" />
                {/if}
              </div>
              <div>
                <p class="font-semibold leading-snug {r.overall === 'UP' ? 'text-emerald-600 dark:text-emerald-400' : 'text-red-600 dark:text-red-400'}">
                  {r.overall === "UP" ? "All Systems Operational" : "System Degraded"}
                </p>
                <p class="mt-0.5 text-xs text-muted-foreground">
                  {r.overall === "UP"
                    ? "PostgreSQL · Kafka · Spark are healthy and responding normally."
                    : "One or more services are experiencing issues — check cards below."}
                </p>
              </div>
            </div>

            <div class="relative hidden items-center gap-4 sm:flex">
              {#each [
                { label: "PostgreSQL", status: r.postgres.status },
                { label: "Kafka", status: r.kafka.status },
                { label: "Spark", status: r.spark.status },
              ] as svc}
                <div class="flex flex-col items-center gap-1">
                  <Circle class="h-2.5 w-2.5 {svc.status === 'UP' ? 'fill-emerald-500 text-emerald-500' : 'fill-red-500 text-red-500'}" />
                  <span class="text-[10px] font-medium text-muted-foreground">{svc.label}</span>
                </div>
              {/each}
            </div>
          </div>

          <!-- ── KPI strip ───────────────────────────────────── -->
          <div
            class="*:data-[slot=card]:from-primary/5 *:data-[slot=card]:to-card dark:*:data-[slot=card]:bg-card grid grid-cols-1 gap-4 *:data-[slot=card]:bg-gradient-to-t *:data-[slot=card]:shadow-xs @xl/main:grid-cols-2 @5xl/main:grid-cols-4"
          >
            <!-- Services UP -->
            <Card.Root class="@container/card">
              <Card.Header>
                <Card.Description>Services UP</Card.Description>
                <Card.Title class="text-2xl font-semibold tabular-nums @[250px]/card:text-3xl">
                  {[r.postgres.status, r.kafka.status, r.spark.status].filter(s => s === "UP").length} / 3
                </Card.Title>
                <Card.Action>
                  <Badge variant="outline">
                    {#if [r.postgres.status, r.kafka.status, r.spark.status].every(s => s === "UP")}
                      <TrendingUp class="size-4" />
                      All healthy
                    {:else}
                      <TrendingDown class="size-4" />
                      Degraded
                    {/if}
                  </Badge>
                </Card.Action>
              </Card.Header>
              <Card.Footer class="flex-col items-start gap-1.5 text-sm">
                <div class="line-clamp-1 flex gap-2 font-medium">
                  {#if [r.postgres.status, r.kafka.status, r.spark.status].every(s => s === "UP")}
                    All systems operational <TrendingUp class="size-4" />
                  {:else}
                    Service degradation detected <TrendingDown class="size-4" />
                  {/if}
                </div>
                <div class="text-muted-foreground">PostgreSQL · Kafka · Spark</div>
              </Card.Footer>
            </Card.Root>

            <!-- Avg Latency -->
            <Card.Root class="@container/card">
              <Card.Header>
                <Card.Description>Avg Latency</Card.Description>
                <Card.Title class="text-2xl font-semibold tabular-nums @[250px]/card:text-3xl">
                  {#if r.postgres.latencyMs != null && r.kafka.latencyMs != null && r.spark.latencyMs != null}
                    {Math.round((r.postgres.latencyMs + r.kafka.latencyMs + r.spark.latencyMs) / 3)} ms
                  {:else}
                    — ms
                  {/if}
                </Card.Title>
                <Card.Action>
                  <Badge variant="outline">
                    <TrendingUp class="size-4" />
                    Real-time
                  </Badge>
                </Card.Action>
              </Card.Header>
              <Card.Footer class="flex-col items-start gap-1.5 text-sm">
                <div class="line-clamp-1 flex gap-2 font-medium">
                  Averaged across all services <TrendingUp class="size-4" />
                </div>
                <div class="text-muted-foreground">PG · Kafka · Spark response times</div>
              </Card.Footer>
            </Card.Root>

            <!-- Spark Workers -->
            <Card.Root class="@container/card">
              <Card.Header>
                <Card.Description>Spark Workers</Card.Description>
                <Card.Title class="text-2xl font-semibold tabular-nums @[250px]/card:text-3xl">
                  {r.spark.status === "UP" ? r.spark.aliveWorkers : "—"}
                  {#if r.spark.status === "UP"}
                    <span class="text-base font-normal text-muted-foreground"> alive</span>
                  {/if}
                </Card.Title>
                <Card.Action>
                  <Badge variant="outline">
                    {#if r.spark.status === "UP"}
                      <TrendingUp class="size-4" />
                      {pct(r.spark.usedCores, r.spark.totalCores)}% CPU
                    {:else}
                      <TrendingDown class="size-4" />
                      Offline
                    {/if}
                  </Badge>
                </Card.Action>
              </Card.Header>
              <Card.Footer class="flex-col items-start gap-1.5 text-sm">
                <div class="line-clamp-1 flex gap-2 font-medium">
                  {#if r.spark.status === "UP"}
                    {r.spark.usedCores}/{r.spark.totalCores} cores · {r.spark.usedMemoryMb}/{r.spark.totalMemoryMb} MB
                  {:else}
                    Spark master unreachable <TrendingDown class="size-4" />
                  {/if}
                </div>
                <div class="text-muted-foreground">Structured Streaming workers</div>
              </Card.Footer>
            </Card.Root>

            <!-- Kafka Topics -->
            <Card.Root class="@container/card">
              <Card.Header>
                <Card.Description>Kafka Topics</Card.Description>
                <Card.Title class="text-2xl font-semibold tabular-nums @[250px]/card:text-3xl">
                  {r.kafka.status === "UP" ? r.kafka.topicCount : "—"}
                  {#if r.kafka.status === "UP"}
                    <span class="text-base font-normal text-muted-foreground"> topics</span>
                  {/if}
                </Card.Title>
                <Card.Action>
                  <Badge variant="outline">
                    {#if r.kafka.status === "UP"}
                      <TrendingUp class="size-4" />
                      {r.kafka.brokerCount} broker{r.kafka.brokerCount !== 1 ? "s" : ""}
                    {:else}
                      <TrendingDown class="size-4" />
                      Offline
                    {/if}
                  </Badge>
                </Card.Action>
              </Card.Header>
              <Card.Footer class="flex-col items-start gap-1.5 text-sm">
                <div class="line-clamp-1 flex gap-2 font-medium">
                  {#if r.kafka.status === "UP"}
                    {r.kafka.brokerCount} broker{r.kafka.brokerCount !== 1 ? "s" : ""} active <TrendingUp class="size-4" />
                  {:else}
                    Kafka broker unreachable <TrendingDown class="size-4" />
                  {/if}
                </div>
                <div class="text-muted-foreground">Message streaming pipeline</div>
              </Card.Footer>
            </Card.Root>
          </div>

          <!-- ── Service detail cards ────────────────────────── -->
          <div class="*:data-[slot=card]:from-primary/5 *:data-[slot=card]:to-card dark:*:data-[slot=card]:bg-card grid grid-cols-1 gap-4 *:data-[slot=card]:bg-gradient-to-t *:data-[slot=card]:shadow-xs @xl/main:grid-cols-3">

            <!-- PostgreSQL ─────────────────────────────────── -->
            <Card.Root class="@container/card flex flex-col">
              <Card.Header>
                <Card.Description class="flex items-center justify-between">
                  <span class="flex items-center gap-1.5"><Database class="size-3.5" />PostgreSQL 16</span>
                  <Badge variant={statusVariant(r.postgres.status)}>{r.postgres.status}</Badge>
                </Card.Description>
                <Card.Title class="text-2xl font-semibold tabular-nums @[250px]/card:text-3xl">
                  {#if r.postgres.status === "UP"}{fmtLatency(r.postgres.latencyMs)}{:else}—{/if}
                </Card.Title>
                <Card.Action>
                  <Badge variant="outline">
                    {#if r.postgres.status === "UP"}<TrendingUp class="size-4" />Primary DB{:else}<TrendingDown class="size-4" />Offline{/if}
                  </Badge>
                </Card.Action>
              </Card.Header>

              <Card.Footer class="flex-col items-start gap-3 text-sm">
                {#if r.postgres.status === "DOWN"}
                  <div class="flex items-center gap-2 font-medium text-destructive">
                    <XCircle class="size-4" /> Service unavailable
                  </div>
                  <p class="text-muted-foreground text-xs break-all">{r.postgres.message}</p>
                {:else}
                  <div class="w-full space-y-2">
                    <div class="flex items-center justify-between text-xs">
                      <span class="flex items-center gap-1.5 text-muted-foreground"><Users class="size-3" />Connections</span>
                      <span class="font-mono font-medium">{r.postgres.activeConnections} / {r.postgres.maxConnections}</span>
                    </div>
                    <div class="relative h-1 w-full overflow-hidden rounded-full bg-secondary">
                      <div class="h-full bg-foreground/70 transition-all duration-700" style="width:{pct(r.postgres.activeConnections, r.postgres.maxConnections)}%"></div>
                    </div>
                  </div>
                  <Separator />
                  <div class="flex w-full items-center justify-between gap-2 font-medium">
                    <span class="flex items-center gap-1.5 text-muted-foreground text-xs"><HardDrive class="size-3" />DB Size</span>
                    <span class="font-mono text-sm">{r.postgres.dbSizeHuman ?? fmtBytes(r.postgres.dbSizeBytes)}</span>
                  </div>
                  <div class="flex w-full items-center justify-between gap-2">
                    <span class="flex items-center gap-1.5 text-muted-foreground text-xs"><Clock class="size-3" />Uptime</span>
                    <span class="font-mono text-sm font-medium">{fmtUptime(r.postgres.uptimeSeconds)}</span>
                  </div>
                  {#if r.postgres.version}
                    <p class="text-muted-foreground text-xs font-mono break-all leading-relaxed">{r.postgres.version}</p>
                  {/if}
                {/if}
              </Card.Footer>
            </Card.Root>

            <!-- Apache Kafka ────────────────────────────────── -->
            <Card.Root class="@container/card flex flex-col">
              <Card.Header>
                <Card.Description class="flex items-center justify-between">
                  <span class="flex items-center gap-1.5"><Network class="size-3.5" />Apache Kafka</span>
                  <Badge variant={statusVariant(r.kafka.status)}>{r.kafka.status}</Badge>
                </Card.Description>
                <Card.Title class="text-2xl font-semibold tabular-nums @[250px]/card:text-3xl">
                  {#if r.kafka.status === "UP"}{fmtLatency(r.kafka.latencyMs)}{:else}—{/if}
                </Card.Title>
                <Card.Action>
                  <Badge variant="outline">
                    {#if r.kafka.status === "UP"}<TrendingUp class="size-4" />{r.kafka.brokerCount} broker{r.kafka.brokerCount !== 1 ? "s" : ""}{:else}<TrendingDown class="size-4" />Offline{/if}
                  </Badge>
                </Card.Action>
              </Card.Header>

              <Card.Footer class="flex-col items-start gap-3 text-sm">
                {#if r.kafka.status === "DOWN"}
                  <div class="flex items-center gap-2 font-medium text-destructive">
                    <XCircle class="size-4" /> Service unavailable
                  </div>
                  <p class="text-muted-foreground text-xs break-all">{r.kafka.message}</p>
                {:else}
                  <div class="line-clamp-1 flex gap-2 font-medium">
                    {r.kafka.brokerCount} broker{r.kafka.brokerCount !== 1 ? "s" : ""} · {r.kafka.topicCount} topics
                  </div>
                  <Separator />
                  <div class="w-full space-y-1.5">
                    {#each r.kafka.topics as t}
                      <div class="flex items-center justify-between text-xs">
                        <span class="font-mono truncate max-w-[140px] text-muted-foreground" title={t.name}>{t.name}</span>
                        <span class="shrink-0 font-medium">{t.partitions}p · RF{t.replicationFactor}</span>
                      </div>
                    {/each}
                  </div>
                {/if}
              </Card.Footer>
            </Card.Root>

            <!-- Apache Spark ────────────────────────────────── -->
            <Card.Root class="@container/card flex flex-col">
              <Card.Header>
                <Card.Description class="flex items-center justify-between">
                  <span class="flex items-center gap-1.5"><Cpu class="size-3.5" />Apache Spark</span>
                  <Badge variant={statusVariant(r.spark.status)}>{r.spark.status}</Badge>
                </Card.Description>
                <Card.Title class="text-2xl font-semibold tabular-nums @[250px]/card:text-3xl">
                  {#if r.spark.status === "UP"}{fmtLatency(r.spark.latencyMs)}{:else}—{/if}
                </Card.Title>
                <Card.Action>
                  <Badge variant="outline">
                    {#if r.spark.status === "UP"}<TrendingUp class="size-4" />{r.spark.aliveWorkers} worker{r.spark.aliveWorkers !== 1 ? "s" : ""}{:else}<TrendingDown class="size-4" />Offline{/if}
                  </Badge>
                </Card.Action>
              </Card.Header>

              <Card.Footer class="flex-col items-start gap-3 text-sm">
                {#if r.spark.status === "DOWN"}
                  <div class="flex items-center gap-2 font-medium text-destructive">
                    <XCircle class="size-4" /> Service unavailable
                  </div>
                  <p class="text-muted-foreground text-xs break-all">{r.spark.message}</p>
                {:else}
                  <div class="line-clamp-1 flex gap-2 font-medium">
                    {r.spark.activeApps} active · {r.spark.completedApps} completed
                  </div>
                  <Separator />
                  <div class="w-full space-y-3">
                    <div class="space-y-1.5">
                      <div class="flex items-center justify-between text-xs">
                        <span class="flex items-center gap-1.5 text-muted-foreground"><Cpu class="size-3" />CPU Cores</span>
                        <span class="font-mono font-medium">{r.spark.usedCores} / {r.spark.totalCores}</span>
                      </div>
                      <div class="relative h-1 w-full overflow-hidden rounded-full bg-secondary">
                        <div class="h-full bg-foreground/70 transition-all duration-700" style="width:{pct(r.spark.usedCores, r.spark.totalCores)}%"></div>
                      </div>
                    </div>
                    <div class="space-y-1.5">
                      <div class="flex items-center justify-between text-xs">
                        <span class="flex items-center gap-1.5 text-muted-foreground"><MemoryStick class="size-3" />Memory</span>
                        <span class="font-mono font-medium">{r.spark.usedMemoryMb} / {r.spark.totalMemoryMb} MB</span>
                      </div>
                      <div class="relative h-1 w-full overflow-hidden rounded-full bg-secondary">
                        <div class="h-full bg-foreground/70 transition-all duration-700" style="width:{pct(r.spark.usedMemoryMb, r.spark.totalMemoryMb)}%"></div>
                      </div>
                    </div>
                  </div>
                {/if}
              </Card.Footer>
            </Card.Root>

          </div>

          <!-- ── Spark Workers table ─────────────────────────── -->
          {#if r.spark.status === "UP" && r.spark.workers.length > 0}
            <Collapsible.Root bind:open={sparkOpen}>
              <Card.Root class="gap-0 overflow-hidden py-0">
                <Collapsible.Trigger class="w-full text-left">
                  <div class="flex items-center justify-between px-5 py-4 hover:bg-muted/30 transition-colors cursor-pointer">
                    <div>
                      <p class="text-sm font-semibold">Spark Workers</p>
                      <p class="text-xs text-muted-foreground">{r.spark.aliveWorkers} worker{r.spark.aliveWorkers !== 1 ? "s" : ""} registered to master</p>
                    </div>
                    <ChevronDown class="h-4 w-4 text-muted-foreground transition-transform duration-200 {sparkOpen ? 'rotate-180' : ''}" />
                  </div>
                </Collapsible.Trigger>
                <Collapsible.Content>
                  <Separator />
                  <ScrollArea class="w-full">
                    <Table.Root>
                      <Table.Header>
                        <Table.Row class="bg-muted/20 hover:bg-muted/20">
                          <Table.Head class="ps-5 text-xs font-medium">Host</Table.Head>
                          <Table.Head class="text-xs font-medium text-right">Port</Table.Head>
                          <Table.Head class="text-xs font-medium min-w-40">CPU Usage</Table.Head>
                          <Table.Head class="text-xs font-medium min-w-44">Memory Usage</Table.Head>
                          <Table.Head class="pe-5 text-xs font-medium text-right">State</Table.Head>
                        </Table.Row>
                      </Table.Header>
                      <Table.Body>
                        {#each r.spark.workers as w}
                          <Table.Row class="hover:bg-muted/20">
                            <Table.Cell class="ps-5 font-mono text-xs">{w.host}</Table.Cell>
                            <Table.Cell class="text-right font-mono text-xs">{w.port}</Table.Cell>
                            <Table.Cell class="text-xs">
                              <div class="flex items-center gap-2">
                                <div class="relative h-1.5 flex-1 overflow-hidden rounded-full bg-muted">
                                  <div class="h-full rounded-full transition-all {usageColor(pct(w.coresUsed, w.cores))}" style="width:{pct(w.coresUsed, w.cores)}%"></div>
                                </div>
                                <span class="w-14 shrink-0 text-right text-[11px] font-mono text-muted-foreground">{w.coresUsed}/{w.cores}</span>
                              </div>
                            </Table.Cell>
                            <Table.Cell class="text-xs">
                              <div class="flex items-center gap-2">
                                <div class="relative h-1.5 flex-1 overflow-hidden rounded-full bg-muted">
                                  <div class="h-full rounded-full transition-all {usageColor(pct(w.memoryUsedMb, w.memoryMb))}" style="width:{pct(w.memoryUsedMb, w.memoryMb)}%"></div>
                                </div>
                                <span class="w-24 shrink-0 text-right text-[11px] font-mono text-muted-foreground">{w.memoryUsedMb}/{w.memoryMb} MB</span>
                              </div>
                            </Table.Cell>
                            <Table.Cell class="pe-5 text-right">
                              <Badge variant={w.state === "ALIVE" ? "default" : "destructive"} class="text-[11px] px-2 py-0">{w.state}</Badge>
                            </Table.Cell>
                          </Table.Row>
                        {/each}
                      </Table.Body>
                    </Table.Root>
                  </ScrollArea>
                </Collapsible.Content>
              </Card.Root>
            </Collapsible.Root>
          {/if}

          <!-- ── Kafka Topics table ──────────────────────────── -->
          {#if r.kafka.status === "UP" && r.kafka.topics.length > 0}
            <Collapsible.Root bind:open={kafkaOpen}>
              <Card.Root class="gap-0 overflow-hidden py-0">
                <Collapsible.Trigger class="w-full text-left">
                  <div class="flex items-center justify-between px-5 py-4 hover:bg-muted/30 transition-colors cursor-pointer">
                    <div>
                      <p class="text-sm font-semibold">Kafka Topics</p>
                      <p class="text-xs text-muted-foreground">{r.kafka.topicCount} topics across {r.kafka.brokerCount} broker{r.kafka.brokerCount !== 1 ? "s" : ""}</p>
                    </div>
                    <ChevronDown class="h-4 w-4 text-muted-foreground transition-transform duration-200 {kafkaOpen ? 'rotate-180' : ''}" />
                  </div>
                </Collapsible.Trigger>
                <Collapsible.Content>
                  <Separator />
                  <Table.Root>
                    <Table.Header>
                      <Table.Row class="bg-muted/20 hover:bg-muted/20">
                        <Table.Head class="ps-5 text-xs font-medium">Topic Name</Table.Head>
                        <Table.Head class="text-xs font-medium text-right">Partitions</Table.Head>
                        <Table.Head class="pe-5 text-xs font-medium text-right">Replication Factor</Table.Head>
                      </Table.Row>
                    </Table.Header>
                    <Table.Body>
                      {#each r.kafka.topics as t}
                        <Table.Row class="hover:bg-muted/20">
                          <Table.Cell class="ps-5 font-mono text-xs font-medium">{t.name}</Table.Cell>
                          <Table.Cell class="text-right text-xs tabular-nums">{t.partitions}</Table.Cell>
                          <Table.Cell class="pe-5 text-right text-xs tabular-nums">{t.replicationFactor}</Table.Cell>
                        </Table.Row>
                      {/each}
                    </Table.Body>
                  </Table.Root>
                </Collapsible.Content>
              </Card.Root>
            </Collapsible.Root>
          {/if}

        {/if}
      </div>
    </div>

  </Sidebar.Inset>
</Sidebar.Provider>
