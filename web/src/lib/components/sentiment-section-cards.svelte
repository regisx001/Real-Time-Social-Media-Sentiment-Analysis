<script lang="ts">
  import { TrendingDown, TrendingUp } from "@lucide/svelte";
  import { Badge } from "$lib/components/ui/badge/index.js";
  import * as Card from "$lib/components/ui/card/index.js";
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
        146,592
      </Card.Title>
      <Card.Action>
        <Badge variant="outline">
          <TrendingUp class="size-4" />
          +12.5%
        </Badge>
      </Card.Action>
    </Card.Header>
    <Card.Footer class="flex-col items-start gap-1.5 text-sm">
      <div class="line-clamp-1 flex gap-2 font-medium">
        Up 12.5% this hour <TrendingUp class="size-4" />
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
        716 msg/s
      </Card.Title>
      <Card.Action>
        <Badge variant="outline">
          <TrendingUp class="size-4" />
          +5.2%
        </Badge>
      </Card.Action>
    </Card.Header>
    <Card.Footer class="flex-col items-start gap-1.5 text-sm">
      <div class="line-clamp-1 flex gap-2 font-medium">
        Throughput increasing <TrendingUp class="size-4" />
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
        36.0%
      </Card.Title>
      <Card.Action>
        <Badge variant="outline">
          <TrendingUp class="size-4" />
          +2.1%
        </Badge>
      </Card.Action>
    </Card.Header>
    <Card.Footer class="flex-col items-start gap-1.5 text-sm">
      <div class="line-clamp-1 flex gap-2 font-medium">
        Positive trend this session <TrendingUp class="size-4" />
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
        28.8%
      </Card.Title>
      <Card.Action>
        <Badge variant="outline">
          <TrendingDown class="size-4" />
          -1.3%
        </Badge>
      </Card.Action>
    </Card.Header>
    <Card.Footer class="flex-col items-start gap-1.5 text-sm">
      <div class="line-clamp-1 flex gap-2 font-medium">
        Negative signals declining <TrendingDown class="size-4" />
      </div>
      <div class="text-muted-foreground">Compared to previous hour</div>
    </Card.Footer>
  </Card.Root>
</div>
