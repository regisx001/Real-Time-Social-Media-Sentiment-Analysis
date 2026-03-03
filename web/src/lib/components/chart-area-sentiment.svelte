<script lang="ts">
  import * as Chart from "$lib/components/ui/chart/index.js";
  import * as Card from "$lib/components/ui/card/index.js";
  import * as Select from "$lib/components/ui/select/index.js";
  import * as ToggleGroup from "$lib/components/ui/toggle-group/index.js";
  import { scaleUtc } from "d3-scale";
  import { Area, AreaChart } from "layerchart";
  import { curveNatural } from "d3-shape";
  import type { SentimentTimePoint } from "$lib/stores/analytics.js";
  import { reconnectAnalytics } from "$lib/stores/analytics.js";

  let { timeSeries = [] }: { timeSeries: SentimentTimePoint[] } = $props();

  // ── Range config ────────────────────────────────────────────────
  type RangeKey = "10m" | "30m" | "1h" | "12h" | "24h" | "7d" | "30d";

  const RANGES: Record<
    RangeKey,
    { label: string; bucket: string; minutes: number }
  > = {
    "10m": { label: "Last 10 minutes", bucket: "minute", minutes: 10 },
    "30m": { label: "Last 30 minutes", bucket: "minute", minutes: 30 },
    "1h": { label: "Last hour", bucket: "minute", minutes: 60 },
    "12h": { label: "Last 12 hours", bucket: "hour", minutes: 720 },
    "24h": { label: "Last 24 hours", bucket: "hour", minutes: 1440 },
    "7d": { label: "Last 7 days", bucket: "day", minutes: 10080 },
    "30d": { label: "Last 30 days", bucket: "day", minutes: 43200 },
  };

  let timeRange = $state<RangeKey>("10m");

  const gradientIds: Record<string, string> = {
    positive: "fillPositive",
    negative: "fillNegative",
    neutral: "fillNeutral",
  };

  const selectedLabel = $derived(RANGES[timeRange].label);

  // When the user picks a range, reconnect SSE with correct bucket+minutes
  function onRangeChange(value: string) {
    timeRange = value as RangeKey;
    const r = RANGES[timeRange];
    reconnectAnalytics(r.bucket, r.minutes);
  }

  // ── Chart data ──────────────────────────────────────────────────
  // Convert ISO strings to Date objects — backend already applied the time
  // window filter so we just pass the data straight to the chart.
  // For "1m" we additionally trim to the last 60 s client-side on top of
  // what the backend returned (bucket=minute, hours=1).
  const filteredData = $derived(
    timeSeries
      .map((p) => ({
        date: new Date(p.time),
        positive: p.positive,
        negative: p.negative,
        neutral: p.neutral,
      }))
      .filter((d) => {
        if (timeRange !== "10m") return true;
        return d.date >= new Date(Date.now() - 10 * 60 * 1000);
      }),
  );

  // ── X-axis / tooltip formatting ─────────────────────────────────
  function xFormatter(v: Date): string {
    if (timeRange === "1m" || timeRange === "1h") {
      return v.toLocaleTimeString("en-US", {
        hour: "2-digit",
        minute: "2-digit",
      });
    }
    if (timeRange === "12h" || timeRange === "24h") {
      return v.toLocaleTimeString("en-US", {
        hour: "2-digit",
        minute: "2-digit",
      });
    }
    return v.toLocaleDateString("en-US", { month: "short", day: "numeric" });
  }

  function tooltipFormatter(v: Date): string {
    if (timeRange === "1m" || timeRange === "1h") {
      return v.toLocaleTimeString("en-US", {
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
      });
    }
    if (timeRange === "12h" || timeRange === "24h") {
      return v.toLocaleString("en-US", {
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
    }
    return v.toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  }

  const chartConfig = {
    positive: { label: "Positive", color: "var(--chart-2)" },
    negative: { label: "Negative", color: "var(--chart-1)" },
    neutral: { label: "Neutral", color: "var(--chart-3)" },
  } satisfies Chart.ChartConfig;
</script>

<Card.Root class="@container/card">
  <Card.Header>
    <Card.Title>Sentiment Over Time</Card.Title>
    <Card.Description>
      <span class="hidden @[540px]/card:block"
        >Positive / Negative / Neutral breakdown</span
      >
      <span class="@[540px]/card:hidden">Sentiment trend</span>
    </Card.Description>
    <Card.Action>
      <ToggleGroup.Root
        type="single"
        value={timeRange}
        onValueChange={(v) => v && onRangeChange(v)}
        variant="outline"
        class="hidden *:data-[slot=toggle-group-item]:!px-4 @[767px]/card:flex"
      >
        <ToggleGroup.Item value="10m">10 min</ToggleGroup.Item>
        <ToggleGroup.Item value="30m">30 min</ToggleGroup.Item>
        <ToggleGroup.Item value="1h">1 hour</ToggleGroup.Item>
        <ToggleGroup.Item value="12h">12 hours</ToggleGroup.Item>
        <ToggleGroup.Item value="24h">24 hours</ToggleGroup.Item>
        <ToggleGroup.Item value="7d">7 days</ToggleGroup.Item>
        <ToggleGroup.Item value="30d">30 days</ToggleGroup.Item>
      </ToggleGroup.Root>
      <Select.Root
        type="single"
        value={timeRange}
        onValueChange={(v) => v && onRangeChange(v)}
      >
        <Select.Trigger
          size="sm"
          class="flex w-40 **:data-[slot=select-value]:block **:data-[slot=select-value]:truncate @[767px]/card:hidden"
          aria-label="Select a value"
        >
          <span data-slot="select-value">{selectedLabel}</span>
        </Select.Trigger>
        <Select.Content class="rounded-xl">
          <Select.Item value="10m" class="rounded-lg"
            >Last 10 minutes</Select.Item
          >
          <Select.Item value="30m" class="rounded-lg"
            >Last 30 minutes</Select.Item
          >
          <Select.Item value="1h" class="rounded-lg">Last hour</Select.Item>
          <Select.Item value="12h" class="rounded-lg">Last 12 hours</Select.Item
          >
          <Select.Item value="24h" class="rounded-lg">Last 24 hours</Select.Item
          >
          <Select.Item value="7d" class="rounded-lg">Last 7 days</Select.Item>
          <Select.Item value="30d" class="rounded-lg">Last 30 days</Select.Item>
        </Select.Content>
      </Select.Root>
    </Card.Action>
  </Card.Header>
  <Card.Content class="px-2 pt-4 sm:px-6 sm:pt-6">
    {#if filteredData.length === 0}
      <div
        class="flex aspect-auto h-[250px] w-full items-center justify-center text-sm text-muted-foreground"
      >
        Waiting for data…
      </div>
    {:else}
      <Chart.Container
        config={chartConfig}
        class="aspect-auto h-[250px] w-full"
      >
        <AreaChart
          legend
          data={filteredData}
          x="date"
          xScale={scaleUtc()}
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
          seriesLayout="stack"
          props={{
            area: {
              curve: curveNatural,
              line: { class: "stroke-1" },
              motion: "tween",
            },
            xAxis: {
              format: xFormatter,
            },
            yAxis: { format: () => "" },
          }}
        >
          {#snippet marks({ series, getAreaProps })}
            <defs>
              {#each series as s}
                <linearGradient
                  id={gradientIds[s.key]}
                  x1="0"
                  y1="0"
                  x2="0"
                  y2="1"
                >
                  <stop
                    offset="5%"
                    stop-color={s.color}
                    stop-opacity={s.key === "neutral" ? 0.8 : 1.0}
                  />
                  <stop offset="95%" stop-color={s.color} stop-opacity={0.1} />
                </linearGradient>
              {/each}
            </defs>
            {#each series as s, i (s.key)}
              <Area {...getAreaProps(s, i)} fill="url(#{gradientIds[s.key]})" />
            {/each}
          {/snippet}
          {#snippet tooltip()}
            <Chart.Tooltip labelFormatter={tooltipFormatter} indicator="line" />
          {/snippet}
        </AreaChart>
      </Chart.Container>
    {/if}
  </Card.Content>
</Card.Root>
