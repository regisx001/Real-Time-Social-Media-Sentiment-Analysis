<script lang="ts">
  import { TrendingDown, TrendingUp } from "@lucide/svelte";
  import { Badge } from "$lib/components/ui/badge/index.js";
  import * as Card from "$lib/components/ui/card/index.js";
  import type { AnalyticsSummary } from "$lib/stores/analytics.js";

  let { summary }: { summary: AnalyticsSummary | null | undefined } = $props();

  const total = $derived(summary?.totalProcessed ?? 0);
  const posRate = $derived(
    total > 0 ? ((summary!.positive / total) * 100).toFixed(1) : "—",
  );
  const negRate = $derived(
    total > 0 ? ((summary!.negative / total) * 100).toFixed(1) : "—",
  );
  const throughput = $derived(
    summary != null ? summary.throughputPerSec.toFixed(1) : "—",
  );
</script>

<div
  class="*:data-[slot=card]:from-primary/5 *:data-[slot=card]:to-card dark:*:data-[slot=card]:bg-card grid grid-cols-1 gap-4 px-4 *:data-[slot=card]:bg-gradient-to-t *:data-[slot=card]:shadow-xs lg:px-6 @xl/main:grid-cols-2 @5xl/main:grid-cols-4"
>
  <!-- Total Tweets Processed -->
  <Card.Root class="@container/card">
    <Card.Header>
      <Card.Description>Total Tweets Processed</Card.Description>
      <Card.Title
        class="text-2xl font-semibold tabular-nums @[250px]/card:text-3xl"
      >
        {total.toLocaleString()}
      </Card.Title>
      <Card.Action>
        <Badge variant="outline">
          <TrendingUp class="size-4" />
          Live
        </Badge>
      </Card.Action>
    </Card.Header>
    <Card.Footer class="flex-col items-start gap-1.5 text-sm">
      <div class="line-clamp-1 flex gap-2 font-medium">
        Cumulative total <TrendingUp class="size-4" />
      </div>
      <div class="text-muted-foreground">Total messages ingested via Kafka</div>
    </Card.Footer>
  </Card.Root>

  <!-- Processing Rate -->
  <Card.Root class="@container/card">
    <Card.Header>
      <Card.Description>Processing Rate</Card.Description>
      <Card.Title
        class="text-2xl font-semibold tabular-nums @[250px]/card:text-3xl"
      >
        {throughput} msg/s
      </Card.Title>
      <Card.Action>
        <Badge variant="outline">
          <TrendingUp class="size-4" />
          60 s window
        </Badge>
      </Card.Action>
    </Card.Header>
    <Card.Footer class="flex-col items-start gap-1.5 text-sm">
      <div class="line-clamp-1 flex gap-2 font-medium">
        Rolling 60-second average <TrendingUp class="size-4" />
      </div>
      <div class="text-muted-foreground">
        Spark Structured Streaming pipeline
      </div>
    </Card.Footer>
  </Card.Root>

  <!-- Positive Sentiment -->
  <Card.Root class="@container/card">
    <Card.Header>
      <Card.Description>Positive Sentiment</Card.Description>
      <Card.Title
        class="text-2xl font-semibold tabular-nums @[250px]/card:text-3xl"
      >
        {posRate}{total > 0 ? "%" : ""}
      </Card.Title>
      <Card.Action>
        <Badge variant="outline">
          <TrendingUp class="size-4" />
          {summary?.positive.toLocaleString() ?? 0} tweets
        </Badge>
      </Card.Action>
    </Card.Header>
    <Card.Footer class="flex-col items-start gap-1.5 text-sm">
      <div class="line-clamp-1 flex gap-2 font-medium">
        Of all processed tweets <TrendingUp class="size-4" />
      </div>
      <div class="text-muted-foreground">Logistic regression model output</div>
    </Card.Footer>
  </Card.Root>

  <!-- Negative Sentiment -->
  <Card.Root class="@container/card">
    <Card.Header>
      <Card.Description>Negative Sentiment</Card.Description>
      <Card.Title
        class="text-2xl font-semibold tabular-nums @[250px]/card:text-3xl"
      >
        {negRate}{total > 0 ? "%" : ""}
      </Card.Title>
      <Card.Action>
        <Badge variant="outline">
          <TrendingDown class="size-4" />
          {summary?.negative.toLocaleString() ?? 0} tweets
        </Badge>
      </Card.Action>
    </Card.Header>
    <Card.Footer class="flex-col items-start gap-1.5 text-sm">
      <div class="line-clamp-1 flex gap-2 font-medium">
        Of all processed tweets <TrendingDown class="size-4" />
      </div>
      <div class="text-muted-foreground">Compared to total processed</div>
    </Card.Footer>
  </Card.Root>
</div>
