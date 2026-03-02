<script lang="ts">
  import * as Chart from "$lib/components/ui/chart/index.js";
  import * as Card from "$lib/components/ui/card/index.js";
  import * as Select from "$lib/components/ui/select/index.js";
  import * as ToggleGroup from "$lib/components/ui/toggle-group/index.js";
  import { scaleUtc } from "d3-scale";
  import { Area, AreaChart } from "layerchart";
  import { curveNatural } from "d3-shape";

  // Sentiment data spanning 90 days (sampled daily)
  const allChartData = [
    { date: new Date("2024-04-01"), positive: 33, negative: 28, neutral: 39 },
    { date: new Date("2024-04-03"), positive: 38, negative: 31, neutral: 31 },
    { date: new Date("2024-04-05"), positive: 45, negative: 25, neutral: 30 },
    { date: new Date("2024-04-07"), positive: 42, negative: 30, neutral: 28 },
    { date: new Date("2024-04-09"), positive: 36, negative: 34, neutral: 30 },
    { date: new Date("2024-04-11"), positive: 50, negative: 22, neutral: 28 },
    { date: new Date("2024-04-13"), positive: 47, negative: 26, neutral: 27 },
    { date: new Date("2024-04-15"), positive: 40, negative: 29, neutral: 31 },
    { date: new Date("2024-04-17"), positive: 44, negative: 27, neutral: 29 },
    { date: new Date("2024-04-19"), positive: 51, negative: 21, neutral: 28 },
    { date: new Date("2024-04-21"), positive: 35, negative: 33, neutral: 32 },
    { date: new Date("2024-04-23"), positive: 48, negative: 24, neutral: 28 },
    { date: new Date("2024-04-25"), positive: 55, negative: 18, neutral: 27 },
    { date: new Date("2024-04-27"), positive: 43, negative: 28, neutral: 29 },
    { date: new Date("2024-04-29"), positive: 39, negative: 32, neutral: 29 },
    { date: new Date("2024-05-01"), positive: 46, negative: 26, neutral: 28 },
    { date: new Date("2024-05-03"), positive: 52, negative: 20, neutral: 28 },
    { date: new Date("2024-05-05"), positive: 58, negative: 16, neutral: 26 },
    { date: new Date("2024-05-07"), positive: 41, negative: 30, neutral: 29 },
    { date: new Date("2024-05-09"), positive: 37, negative: 34, neutral: 29 },
    { date: new Date("2024-05-11"), positive: 49, negative: 23, neutral: 28 },
    { date: new Date("2024-05-13"), positive: 54, negative: 19, neutral: 27 },
    { date: new Date("2024-05-15"), positive: 61, negative: 14, neutral: 25 },
    { date: new Date("2024-05-17"), positive: 57, negative: 17, neutral: 26 },
    { date: new Date("2024-05-19"), positive: 44, negative: 28, neutral: 28 },
    { date: new Date("2024-05-21"), positive: 38, negative: 33, neutral: 29 },
    { date: new Date("2024-05-23"), positive: 50, negative: 22, neutral: 28 },
    { date: new Date("2024-05-25"), positive: 56, negative: 17, neutral: 27 },
    { date: new Date("2024-05-27"), positive: 62, negative: 13, neutral: 25 },
    { date: new Date("2024-05-29"), positive: 45, negative: 27, neutral: 28 },
    { date: new Date("2024-05-31"), positive: 40, negative: 31, neutral: 29 },
    { date: new Date("2024-06-02"), positive: 53, negative: 20, neutral: 27 },
    { date: new Date("2024-06-04"), positive: 59, negative: 15, neutral: 26 },
    { date: new Date("2024-06-06"), positive: 48, negative: 25, neutral: 27 },
    { date: new Date("2024-06-08"), positive: 65, negative: 11, neutral: 24 },
    { date: new Date("2024-06-10"), positive: 43, negative: 29, neutral: 28 },
    { date: new Date("2024-06-12"), positive: 36, negative: 35, neutral: 29 },
    { date: new Date("2024-06-14"), positive: 57, negative: 17, neutral: 26 },
    { date: new Date("2024-06-16"), positive: 63, negative: 12, neutral: 25 },
    { date: new Date("2024-06-18"), positive: 52, negative: 21, neutral: 27 },
    { date: new Date("2024-06-20"), positive: 47, negative: 26, neutral: 27 },
    { date: new Date("2024-06-22"), positive: 66, negative: 10, neutral: 24 },
    { date: new Date("2024-06-24"), positive: 39, negative: 33, neutral: 28 },
    { date: new Date("2024-06-26"), positive: 54, negative: 19, neutral: 27 },
    { date: new Date("2024-06-28"), positive: 60, negative: 14, neutral: 26 },
    { date: new Date("2024-06-30"), positive: 36, negative: 34, neutral: 30 },
  ];

  let timeRange = $state("90d");

  const selectedLabel = $derived.by(() => {
    switch (timeRange) {
      case "90d":
        return "Last 3 months";
      case "30d":
        return "Last 30 days";
      case "7d":
        return "Last 7 days";
      default:
        return "Last 3 months";
    }
  });

  const filteredData = $derived(
    allChartData.filter((item) => {
      // eslint-disable-next-line svelte/prefer-svelte-reactivity
      const referenceDate = new Date("2024-06-30");
      let daysToSubtract = 90;
      if (timeRange === "30d") {
        daysToSubtract = 30;
      } else if (timeRange === "7d") {
        daysToSubtract = 7;
      }
      referenceDate.setDate(referenceDate.getDate() - daysToSubtract);
      return item.date >= referenceDate;
    }),
  );

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
        bind:value={timeRange}
        variant="outline"
        class="hidden *:data-[slot=toggle-group-item]:!px-4 @[767px]/card:flex"
      >
        <ToggleGroup.Item value="90d">Last 3 months</ToggleGroup.Item>
        <ToggleGroup.Item value="30d">Last 30 days</ToggleGroup.Item>
        <ToggleGroup.Item value="7d">Last 7 days</ToggleGroup.Item>
      </ToggleGroup.Root>
      <Select.Root type="single" bind:value={timeRange}>
        <Select.Trigger
          size="sm"
          class="flex w-40 **:data-[slot=select-value]:block **:data-[slot=select-value]:truncate @[767px]/card:hidden"
          aria-label="Select a value"
        >
          <span data-slot="select-value">{selectedLabel}</span>
        </Select.Trigger>
        <Select.Content class="rounded-xl">
          <Select.Item value="90d" class="rounded-lg">Last 3 months</Select.Item
          >
          <Select.Item value="30d" class="rounded-lg">Last 30 days</Select.Item>
          <Select.Item value="7d" class="rounded-lg">Last 7 days</Select.Item>
        </Select.Content>
      </Select.Root>
    </Card.Action>
  </Card.Header>
  <Card.Content class="px-2 pt-4 sm:px-6 sm:pt-6">
    <Chart.Container config={chartConfig} class="aspect-auto h-[250px] w-full">
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
            "fill-opacity": 0.4,
            line: { class: "stroke-2" },
          },
          xAxis: {
            format: (v: Date) =>
              v.toLocaleDateString("en-US", { month: "short", day: "numeric" }),
            classes: { tickLabel: "fill-muted-foreground text-xs" },
          },
          yAxis: {
            ticks: 4,
            format: (v: number) => `${v}%`,
            classes: { tickLabel: "fill-muted-foreground text-xs" },
          },
          grid: { y: true, x: false },
        }}
      >
        {#snippet tooltip()}
          <Chart.Tooltip
            labelFormatter={(v: Date) =>
              v.toLocaleDateString("en-US", { month: "short", day: "numeric" })}
            indicator="line"
          />
        {/snippet}
      </AreaChart>
    </Chart.Container>
  </Card.Content>
</Card.Root>
