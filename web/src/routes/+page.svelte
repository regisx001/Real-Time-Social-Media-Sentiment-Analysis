<script lang="ts">
  import * as Card from "$lib/components/ui/card/index.js";
  import * as Chart from "$lib/components/ui/chart/index.js";
  import ChartContainer from "$lib/components/ui/chart/chart-container.svelte";
  import { Progress } from "$lib/components/ui/progress/index.js";
  import { Separator } from "$lib/components/ui/separator/index.js";
  import { Badge } from "$lib/components/ui/badge/index.js";
  import * as Table from "$lib/components/ui/table/index.js";
  import * as Avatar from "$lib/components/ui/avatar/index.js";
  import { ScrollArea } from "$lib/components/ui/scroll-area/index.js";

  import { scaleUtc } from "d3-scale";
  import { Area, AreaChart, ChartClipPath } from "layerchart";
  import { curveNatural } from "d3-shape";
  import { cubicInOut } from "svelte/easing";

  import {
    MessageSquare,
    Zap,
    Activity,
    TrendingUp,
    Server,
    Activity as ActivityIcon,
    Cpu,
    Box,
    Database,
    CircleCheck,
    AlertTriangle,
    Info,
  } from "@lucide/svelte";

  import { serviceMap, connectionState, lastUpdated } from "$lib/stores/health";

  // Helper: get status for a named service from the live store
  function svcStatus(name: string): "UP" | "DOWN" | "UNKNOWN" {
    const s = $serviceMap[name];
    return s ? s.status : "UNKNOWN";
  }
  function svcLatency(name: string): string {
    const s = $serviceMap[name];
    return s?.latencyMs != null ? `${s.latencyMs} ms` : "";
  }
  function svcLabel(name: string): string {
    const status = svcStatus(name);
    return status === "UP"
      ? "Operational"
      : status === "DOWN"
        ? "Down"
        : "Checking…";
  }

  // Dummy Chart Data
  const chartData = [
    {
      date: new Date("2024-06-01T21:04:28"),
      positive: 33,
      negative: 34,
      neutral: 47,
    },
    {
      date: new Date("2024-06-01T21:04:38"),
      positive: 36,
      negative: 39,
      neutral: 41,
    },
    {
      date: new Date("2024-06-01T21:04:48"),
      positive: 45,
      negative: 31,
      neutral: 30,
    },
    {
      date: new Date("2024-06-01T21:04:58"),
      positive: 50,
      negative: 29,
      neutral: 28,
    },
    {
      date: new Date("2024-06-01T21:05:08"),
      positive: 54,
      negative: 18,
      neutral: 28,
    },
    {
      date: new Date("2024-06-01T21:05:18"),
      positive: 57,
      negative: 26,
      neutral: 20,
    },
    {
      date: new Date("2024-06-01T21:05:28"),
      positive: 50,
      negative: 11,
      neutral: 38,
    },
    {
      date: new Date("2024-06-01T21:05:38"),
      positive: 65,
      negative: 31,
      neutral: 25,
    },
    {
      date: new Date("2024-06-01T21:05:48"),
      positive: 30,
      negative: 20,
      neutral: 52,
    },
    {
      date: new Date("2024-06-01T21:05:58"),
      positive: 26,
      negative: 14,
      neutral: 37,
    },
    {
      date: new Date("2024-06-01T21:06:03"),
      positive: 66,
      negative: 35,
      neutral: 17,
    },
  ];

  const chartConfig = {
    positive: { label: "Positive", color: "var(--chart-2)" },
    negative: { label: "Negative", color: "var(--chart-1)" },
    neutral: { label: "Neutral", color: "var(--chart-3)" },
  } satisfies Chart.ChartConfig;

  // Dummy Entities Data
  const topEntities = [
    { name: "Apple", mentions: "1,245", sentiment: "positive" },
    { name: "Tesla", mentions: "892", sentiment: "negative" },
    { name: "Google", mentions: "854", sentiment: "neutral" },
    { name: "Microsoft", mentions: "632", sentiment: "positive" },
    { name: "Amazon", mentions: "410", sentiment: "neutral" },
  ];

  // Dummy Live Stream Data
  const liveTweets = [
    {
      id: 1,
      user: "JohnDoe",
      text: "Just got the new iPhone, amazing camera! ✨",
      sentiment: "positive",
      time: "Just now",
    },
    {
      id: 2,
      user: "JaneSmith",
      text: "Tesla autopilot is scaring me sometimes 😨",
      sentiment: "negative",
      time: "2s ago",
    },
    {
      id: 3,
      user: "DevGuy",
      text: "Google Cloud pricing changed for my project.",
      sentiment: "neutral",
      time: "5s ago",
    },
    {
      id: 4,
      user: "TechEnthusiast",
      text: "Microsoft Copilot is a game changer for real!",
      sentiment: "positive",
      time: "10s ago",
    },
    {
      id: 5,
      user: "AngryCustomer",
      text: "Amazon delivery is late again for the 3rd time.",
      sentiment: "negative",
      time: "12s ago",
    },
    {
      id: 6,
      user: "Investor",
      text: "Apple stock looking very strong today 📈",
      sentiment: "positive",
      time: "15s ago",
    },
  ];
</script>

<div class="min-h-screen bg-background text-foreground">
  <header
    class="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60"
  >
    <div class="flex h-14 items-center mx-auto max-w-7xl px-4 md:px-8">
      <div class="mr-4 flex">
        <a href="/" class="mr-6 flex items-center space-x-2 text-emerald-500">
          <Activity class="h-6 w-6" />
          <span class="font-bold inline-block text-foreground tracking-tight"
            >PulseStream</span
          >
        </a>
      </div>
      <div
        class="flex flex-1 items-center justify-between space-x-2 sm:space-x-4 md:justify-end"
      >
        <div class="w-full flex-1 md:w-auto md:flex-none"></div>
        <nav class="flex items-center gap-2">
          <span
            class="text-sm font-medium text-muted-foreground hidden sm:inline-block"
            >Real-time Analysis</span
          >
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
                >Connected</span
              >
            {:else if $connectionState === "error"}
              <span class="relative inline-flex rounded-full h-2 w-2 bg-red-500"
              ></span>
              <span class="text-xs font-medium text-red-500">Reconnecting…</span
              >
            {:else}
              <span
                class="relative inline-flex rounded-full h-2 w-2 bg-amber-400 animate-pulse"
              ></span>
              <span class="text-xs font-medium text-amber-500">Connecting…</span
              >
            {/if}
          </div>
        </nav>
      </div>
    </div>
  </header>

  <main class="p-4 md:p-8">
    <div
      class="mx-auto max-w-7xl grid grid-cols-1 md:grid-cols-4 gap-4 md:gap-8"
    >
      <!-- Left Section: 3 Columns -->
      <div class="md:col-span-3 flex flex-col gap-4 md:gap-8">
        <!-- Top Scorecards -->
        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 md:gap-8">
          <!-- Total Tweets -->
          <Card.Root>
            <Card.Header
              class="flex flex-row items-center justify-between pb-2"
            >
              <Card.Title class="text-sm font-medium text-muted-foreground"
                >Total Tweets Processed</Card.Title
              >
              <MessageSquare class="h-4 w-4 text-muted-foreground" />
            </Card.Header>
            <Card.Content>
              <div class="text-2xl font-bold">146,592</div>
              <p
                class="text-xs text-muted-foreground mt-1 flex items-center gap-1 text-emerald-500"
              >
                <TrendingUp class="h-3 w-3" />
                +12.5% vs last hour
              </p>
            </Card.Content>
          </Card.Root>

          <!-- Processing Rate -->
          <Card.Root>
            <Card.Header
              class="flex flex-row items-center justify-between pb-2"
            >
              <Card.Title class="text-sm font-medium text-muted-foreground"
                >Processing Rate</Card.Title
              >
              <Zap class="h-4 w-4 text-muted-foreground" />
            </Card.Header>
            <Card.Content>
              <div class="text-2xl font-bold">716 msg/s</div>
              <p
                class="text-xs text-muted-foreground mt-1 flex items-center gap-1 text-emerald-500"
              >
                <TrendingUp class="h-3 w-3" />
                +5.2% vs last hour
              </p>
            </Card.Content>
          </Card.Root>

          <!-- Net Sentiment Score -->
          <Card.Root>
            <Card.Header
              class="flex flex-row items-center justify-between pb-2"
            >
              <Card.Title class="text-sm font-medium text-muted-foreground"
                >Net Sentiment Score</Card.Title
              >
              <Activity class="h-4 w-4 text-muted-foreground" />
            </Card.Header>
            <Card.Content>
              <div class="text-2xl font-bold">+7</div>
              <p
                class="text-xs text-muted-foreground mt-1 flex items-center gap-1"
              >
                Stable prediction
              </p>
            </Card.Content>
          </Card.Root>
        </div>

        <!-- Charts Row -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4 md:gap-8">
          <!-- Area Chart: Sentiment Over Time -->
          <Card.Root class="col-span-1 md:col-span-2">
            <Card.Header
              class="flex flex-col sm:flex-row sm:items-center justify-between pb-2 gap-4"
            >
              <Card.Title>Sentiment Over Time</Card.Title>
              <div
                class="flex items-center gap-4 text-sm font-medium text-muted-foreground"
              >
                <div class="flex items-center gap-1.5 whitespace-nowrap">
                  <span class="w-3 h-3 rounded-full bg-[var(--chart-2)]"></span>
                  Positive
                </div>
                <div class="flex items-center gap-1.5 whitespace-nowrap">
                  <span class="w-3 h-3 rounded-full bg-[var(--chart-1)]"></span>
                  Negative
                </div>
                <div class="flex items-center gap-1.5 whitespace-nowrap">
                  <span class="w-3 h-3 rounded-full bg-[var(--chart-3)]"></span>
                  Neutral
                </div>
              </div>
            </Card.Header>
            <Card.Content>
              <ChartContainer config={chartConfig} class="h-[250px] w-full">
                <AreaChart
                  data={chartData}
                  x="date"
                  xScale={scaleUtc()}
                  yDomain={[0, 80]}
                  series={[
                    {
                      key: "neutral",
                      label: "Neutral",
                      color: chartConfig.neutral.color,
                    },
                    {
                      key: "negative",
                      label: "Negative",
                      color: chartConfig.negative.color,
                    },
                    {
                      key: "positive",
                      label: "Positive",
                      color: chartConfig.positive.color,
                    },
                  ]}
                  props={{
                    area: {
                      curve: curveNatural,
                      "fill-opacity": 0.2,
                      line: { class: "stroke-2" },
                      motion: "tween",
                    },
                    xAxis: {
                      format: (v) =>
                        v.toLocaleTimeString("en-US", { hour12: false }),
                      classes: { tickLabel: "fill-muted-foreground text-xs" },
                    },
                    yAxis: {
                      ticks: 4,
                      format: (v) => `${v}`,
                      classes: { tickLabel: "fill-muted-foreground text-xs" },
                    },
                    grid: { y: true, x: false },
                  }}
                >
                  {#snippet marks({ series, getAreaProps })}
                    <defs>
                      <linearGradient
                        id="fillPositive"
                        x1="0"
                        y1="0"
                        x2="0"
                        y2="1"
                      >
                        <stop
                          offset="5%"
                          stop-color="var(--chart-2)"
                          stop-opacity={0.3}
                        />
                        <stop
                          offset="95%"
                          stop-color="var(--chart-2)"
                          stop-opacity={0.0}
                        />
                      </linearGradient>
                      <linearGradient
                        id="fillNegative"
                        x1="0"
                        y1="0"
                        x2="0"
                        y2="1"
                      >
                        <stop
                          offset="5%"
                          stop-color="var(--chart-1)"
                          stop-opacity={0.3}
                        />
                        <stop
                          offset="95%"
                          stop-color="var(--chart-1)"
                          stop-opacity={0.0}
                        />
                      </linearGradient>
                      <linearGradient
                        id="fillNeutral"
                        x1="0"
                        y1="0"
                        x2="0"
                        y2="1"
                      >
                        <stop
                          offset="5%"
                          stop-color="var(--chart-3)"
                          stop-opacity={0.3}
                        />
                        <stop
                          offset="95%"
                          stop-color="var(--chart-3)"
                          stop-opacity={0.0}
                        />
                      </linearGradient>
                    </defs>

                    <ChartClipPath
                      initialWidth={0}
                      motion={{
                        width: {
                          type: "tween",
                          duration: 1000,
                          easing: cubicInOut,
                        },
                      }}
                    >
                      {#each series as s, i (s.key)}
                        <Area
                          {...getAreaProps(s, i)}
                          fill={s.key === "positive"
                            ? "url(#fillPositive)"
                            : s.key === "negative"
                              ? "url(#fillNegative)"
                              : "url(#fillNeutral)"}
                          stroke={s.color}
                        />
                      {/each}
                    </ChartClipPath>
                  {/snippet}
                  {#snippet tooltip()}
                    <Chart.Tooltip
                      labelFormatter={(v) =>
                        v.toLocaleTimeString("en-US", { hour12: false })}
                      indicator="line"
                    />
                  {/snippet}
                </AreaChart>
              </ChartContainer>
            </Card.Content>
          </Card.Root>

          <!-- Donut Chart: Overall Distribution -->
          <Card.Root class="flex flex-col">
            <Card.Header class="pb-2">
              <Card.Title class="text-center md:text-left"
                >Overall Distribution</Card.Title
              >
            </Card.Header>
            <Card.Content
              class="flex flex-col items-center justify-center flex-1 gap-6"
            >
              <!-- SVG Donut Chart representing approx 36% Green, 28% Red, 36% Gray -->
              <div class="relative w-40 h-40 mt-4">
                <svg
                  viewBox="0 0 100 100"
                  class="w-full h-full transform -rotate-90"
                >
                  <!-- Neutral (Gray) -->
                  <circle
                    cx="50"
                    cy="50"
                    r="40"
                    fill="transparent"
                    stroke="var(--chart-3)"
                    stroke-width="14"
                  />

                  <!-- Negative (Red) -->
                  <circle
                    cx="50"
                    cy="50"
                    r="40"
                    fill="transparent"
                    stroke="var(--chart-1)"
                    stroke-width="14"
                    stroke-dasharray="251.2"
                    stroke-dashoffset="180"
                    class="transform origin-center rotate-[130deg]"
                  />

                  <!-- Positive (Green) 36% -->
                  <circle
                    cx="50"
                    cy="50"
                    r="40"
                    fill="transparent"
                    stroke="var(--chart-2)"
                    stroke-width="14"
                    stroke-dasharray="251.2"
                    stroke-dashoffset="160.7"
                    class="transform origin-center rotate-[200deg]"
                  />
                </svg>
                <!-- Center Text -->
                <div
                  class="absolute inset-0 flex flex-col items-center justify-center pointer-events-none"
                >
                  <span class="text-3xl font-bold tracking-tight">36%</span>
                  <span
                    class="text-xs font-medium text-muted-foreground uppercase"
                    >Positive</span
                  >
                </div>
              </div>

              <!-- Stats -->
              <div class="grid grid-cols-3 gap-4 w-full text-center">
                <div>
                  <div class="text-sm font-bold">2,463</div>
                  <div class="text-xs text-muted-foreground">Positive</div>
                </div>
                <div>
                  <div class="text-sm font-bold">1,955</div>
                  <div class="text-xs text-muted-foreground">Negative</div>
                </div>
                <div>
                  <div class="text-sm font-bold">2,385</div>
                  <div class="text-xs text-muted-foreground">Neutral</div>
                </div>
              </div>
            </Card.Content>
          </Card.Root>
        </div>

        <!-- Third Row (Top Entities and Live Stream) -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 md:gap-8">
          <Card.Root class="flex flex-col h-[400px]">
            <Card.Header>
              <Card.Title>Top Entities</Card.Title>
              <Card.Description
                >Entities most frequently mentioned</Card.Description
              >
            </Card.Header>
            <Card.Content class="p-0">
              <ScrollArea class="h-full px-6 pb-6">
                <Table.Root>
                  <Table.Header>
                    <Table.Row>
                      <Table.Head>Entity</Table.Head>
                      <Table.Head class="text-right">Mentions</Table.Head>
                      <Table.Head class="text-right">Dominant Tone</Table.Head>
                    </Table.Row>
                  </Table.Header>
                  <Table.Body>
                    {#each topEntities as entity}
                      <Table.Row>
                        <Table.Cell class="font-medium"
                          >{entity.name}</Table.Cell
                        >
                        <Table.Cell class="text-right"
                          >{entity.mentions}</Table.Cell
                        >
                        <Table.Cell class="text-right">
                          {#if entity.sentiment === "positive"}
                            <Badge
                              variant="outline"
                              class="bg-emerald-500/10 text-emerald-500 border-emerald-500/20 font-normal"
                              >Positive</Badge
                            >
                          {:else if entity.sentiment === "negative"}
                            <Badge
                              variant="outline"
                              class="bg-red-500/10 text-red-500 border-red-500/20 font-normal"
                              >Negative</Badge
                            >
                          {:else}
                            <Badge
                              variant="outline"
                              class="bg-slate-500/10 text-slate-500 border-slate-500/20 font-normal"
                              >Neutral</Badge
                            >
                          {/if}
                        </Table.Cell>
                      </Table.Row>
                    {/each}
                  </Table.Body>
                </Table.Root>
              </ScrollArea>
            </Card.Content>
          </Card.Root>

          <Card.Root class="flex flex-col h-[400px]">
            <Card.Header
              class="flex flex-row justify-between items-center bg-card z-10 rounded-t-xl pb-4"
            >
              <div class="space-y-1.5">
                <Card.Title>Live Stream</Card.Title>
                <Card.Description>Recent incoming data stream</Card.Description>
              </div>
              <div
                class="text-sm text-emerald-500 font-medium flex items-center gap-1.5 bg-emerald-500/10 px-2 py-1 rounded-md"
              >
                <ActivityIcon class="w-3.5 h-3.5 animate-pulse" /> Live
              </div>
            </Card.Header>
            <Card.Content class="flex-1 overflow-hidden p-0">
              <ScrollArea class="h-full px-6 pb-6 pt-2">
                <div class="space-y-6">
                  {#each liveTweets as tweet}
                    <div class="flex items-start gap-4">
                      <Avatar.Root class="w-9 h-9 mt-0.5 shadow-sm border">
                        <Avatar.Fallback
                          class="bg-secondary text-secondary-foreground text-xs font-semibold"
                          >{tweet.user
                            .substring(0, 2)
                            .toUpperCase()}</Avatar.Fallback
                        >
                      </Avatar.Root>
                      <div class="space-y-1.5 flex-1 w-0">
                        <div class="flex items-center justify-between gap-2">
                          <p class="text-sm font-semibold leading-none">
                            {tweet.user}
                          </p>
                          <span
                            class="text-[10px] text-muted-foreground whitespace-nowrap"
                            >{tweet.time}</span
                          >
                        </div>
                        <p
                          class="text-sm text-muted-foreground line-clamp-2 leading-snug"
                        >
                          {tweet.text}
                        </p>
                      </div>
                      <div class="pt-0.5">
                        {#if tweet.sentiment === "positive"}
                          <CircleCheck class="w-4 h-4 text-emerald-500" />
                        {:else if tweet.sentiment === "negative"}
                          <AlertTriangle class="w-4 h-4 text-red-500" />
                        {:else}
                          <Info class="w-4 h-4 text-slate-400" />
                        {/if}
                      </div>
                    </div>
                  {/each}
                </div>
              </ScrollArea>
            </Card.Content>
          </Card.Root>
        </div>
      </div>

      <!-- Right Panel -->
      <Card.Root class="col-span-1 h-fit">
        <Card.Content class="pt-6">
          <!-- Infrastructure Status -->
          <div class="flex items-center gap-2 mb-6 font-semibold">
            <Server class="w-5 h-5" /> Infrastructure Status
          </div>

          {#if $lastUpdated}
            <p class="text-xs text-muted-foreground mb-4">
              Last checked: {$lastUpdated.toLocaleTimeString()}
            </p>
          {/if}

          <div class="space-y-6 mb-8">
            {#each [{ label: "Apache Kafka", key: "Kafka", Icon: ActivityIcon }, { label: "Spark Master", key: "Spark", Icon: Cpu }, { label: "Spring Boot API", key: "SpringBoot", Icon: Box }, { label: "PostgreSQL 16", key: "PostgreSQL", Icon: Database }] as svc}
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-3">
                  <svelte:component
                    this={svc.Icon}
                    class="w-4 h-4 text-muted-foreground"
                  />
                  <div class="flex flex-col">
                    <span class="text-sm font-medium">{svc.label}</span>
                    {#if svcLatency(svc.key)}
                      <span class="text-xs text-muted-foreground"
                        >{svcLatency(svc.key)}</span
                      >
                    {/if}
                  </div>
                </div>
                <div class="flex items-center gap-2 text-sm">
                  {#if svcStatus(svc.key) === "UP"}
                    <span class="w-2.5 h-2.5 rounded-full bg-[var(--chart-2)]"
                    ></span>
                    <span class="text-muted-foreground">Operational</span>
                  {:else if svcStatus(svc.key) === "DOWN"}
                    <span class="w-2.5 h-2.5 rounded-full bg-destructive"
                    ></span>
                    <span class="text-destructive">Down</span>
                  {:else}
                    <span
                      class="w-2.5 h-2.5 rounded-full bg-muted-foreground animate-pulse"
                    ></span>
                    <span class="text-muted-foreground">Checking…</span>
                  {/if}
                </div>
              </div>
            {/each}
          </div>

          <Separator class="my-6" />

          <!-- KAFKA TOPICS -->
          <h3
            class="text-xs font-semibold text-muted-foreground mb-6 uppercase tracking-wider"
          >
            Kafka Topics
          </h3>
          <div class="space-y-6 mb-8">
            <div>
              <div class="flex items-center justify-between mb-3">
                <span class="text-sm font-medium">tweets.raw</span>
                <span class="text-xs text-muted-foreground font-mono"
                  >716 msg/s</span
                >
              </div>
              <Progress value={85} class="h-2" />
            </div>
            <div>
              <div class="flex items-center justify-between mb-3">
                <span class="text-sm font-medium">tweets.processed</span>
                <span class="text-xs text-muted-foreground font-mono"
                  >701 msg/s</span
                >
              </div>
              <Progress value={80} class="h-2 [&>div]:bg-[var(--chart-2)]" />
            </div>
          </div>

          <Separator class="my-6" />

          <!-- ML MODEL INFO -->
          <h3
            class="text-xs font-semibold text-muted-foreground mb-6 uppercase tracking-wider"
          >
            ML Model Info
          </h3>
          <div class="space-y-4 text-sm">
            <div class="flex justify-between items-center">
              <span class="text-muted-foreground">Algorithm</span>
              <span class="font-medium">Logistic Regression</span>
            </div>
            <div class="flex justify-between items-center">
              <span class="text-muted-foreground">Pipeline</span>
              <span class="font-medium">PySpark MLlib</span>
            </div>
            <div class="flex justify-between items-center">
              <span class="text-muted-foreground">Last Trained</span>
              <span class="font-medium">2 hours ago</span>
            </div>
            <div class="flex justify-between items-center">
              <span class="text-muted-foreground">Accuracy</span>
              <span class="font-semibold text-[var(--chart-2)]">89.4%</span>
            </div>
          </div>
        </Card.Content>
      </Card.Root>
    </div>
  </main>
</div>
