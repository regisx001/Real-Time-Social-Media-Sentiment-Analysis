<script lang="ts">
  import { onMount } from "svelte";
  import * as Table from "$lib/components/ui/table/index.js";
  import * as Card from "$lib/components/ui/card/index.js";
  import { Badge } from "$lib/components/ui/badge/index.js";
  import { Button } from "$lib/components/ui/button/index.js";
  import { ChevronLeft, ChevronRight, RefreshCw } from "@lucide/svelte";

  const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8090";

  // ---- Types -------------------------------------------------------
  type TweetRow = {
    id: number;
    text: string;
    sentiment: "Positive" | "Negative" | "Neutral";
    confidence: number;
    timestamp: string;
  };

  type PageMeta = { totalPages: number; number: number; totalElements: number };

  // ---- State -------------------------------------------------------
  let rows: TweetRow[] = $state([]);
  let page: number = $state(0);
  let meta: PageMeta | null = $state(null);
  let loading: boolean = $state(false);
  let error: string | null = $state(null);

  const PAGE_SIZE = 20;

  // ---- Helpers -----------------------------------------------------
  function capitalize(s: string): "Positive" | "Negative" | "Neutral" {
    const lower = s.toLowerCase();
    return (lower.charAt(0).toUpperCase() + lower.slice(1)) as
      | "Positive"
      | "Negative"
      | "Neutral";
  }

  function fmtDate(iso: string): string {
    try {
      return new Date(iso).toLocaleString();
    } catch {
      return iso;
    }
  }

  // ---- Fetch -------------------------------------------------------
  async function fetchPage(p: number) {
    loading = true;
    error = null;
    try {
      const res = await fetch(
        `${API_BASE}/api/analytics/tweets?page=${p}&size=${PAGE_SIZE}`,
      );
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const json = await res.json();

      // Spring Data Page<Tweet> response shape
      const content: Array<Record<string, unknown>> = json.content ?? [];
      rows = content.map((t) => {
        const raw = (t.rawData ?? {}) as Record<string, unknown>;
        const processed = (t.processedData ?? {}) as Record<string, unknown>;
        const sentiment = String(processed["sentiment"] ?? "neutral");
        const score = Number(processed["score"] ?? 0);
        const text = String(raw["text"] ?? raw["body"] ?? "—");
        const timestamp = String(t.processedAt ?? t.ingestedAt ?? "");
        return {
          id: Number(t.id),
          text,
          sentiment: capitalize(sentiment),
          confidence: Math.round(score * 100),
          timestamp,
        };
      });

      meta = {
        number: json.number ?? 0,
        totalPages: json.totalPages ?? 0,
        totalElements: json.totalElements ?? 0,
      };
      page = json.number ?? 0;
    } catch (e) {
      error = e instanceof Error ? e.message : "Failed to load tweets";
    } finally {
      loading = false;
    }
  }

  onMount(() => fetchPage(0));
</script>

<Card.Root>
  <Card.Header class="flex flex-row items-center justify-between pb-2">
    <div>
      <Card.Title>Recent Tweets</Card.Title>
      <Card.Description>
        {#if meta}
          {meta.totalElements.toLocaleString()} tweets processed
        {:else}
          Latest ingested &amp; analysed tweets
        {/if}
      </Card.Description>
    </div>
    <Button
      variant="ghost"
      size="icon"
      onclick={() => fetchPage(page)}
      disabled={loading}
      aria-label="Refresh"
    >
      <RefreshCw class="size-4 {loading ? 'animate-spin' : ''}" />
    </Button>
  </Card.Header>

  <Card.Content class="p-0">
    {#if error}
      <div class="px-6 py-8 text-center text-sm text-destructive">{error}</div>
    {:else if rows.length === 0 && !loading}
      <div class="px-6 py-8 text-center text-sm text-muted-foreground">
        Waiting for data…
      </div>
    {:else}
      <div class="overflow-x-auto">
        <Table.Root>
          <Table.Header>
            <Table.Row>
              <Table.Head class="w-[60%]">Text</Table.Head>
              <Table.Head>Sentiment</Table.Head>
              <Table.Head class="text-right">Confidence</Table.Head>
              <Table.Head class="text-right">Processed At</Table.Head>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {#if loading}
              {#each { length: 5 } as _, i (i)}
                <Table.Row>
                  <Table.Cell colspan={4}>
                    <div
                      class="h-4 w-full animate-pulse rounded bg-muted"
                    ></div>
                  </Table.Cell>
                </Table.Row>
              {/each}
            {:else}
              {#each rows as row (row.id)}
                <Table.Row>
                  <Table.Cell class="max-w-[320px] truncate text-sm">
                    {row.text}
                  </Table.Cell>
                  <Table.Cell>
                    {#if row.sentiment === "Positive"}
                      <Badge
                        class="bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/20"
                      >
                        Positive
                      </Badge>
                    {:else if row.sentiment === "Negative"}
                      <Badge
                        class="bg-red-500/15 text-red-600 dark:text-red-400 border-red-500/20"
                      >
                        Negative
                      </Badge>
                    {:else}
                      <Badge
                        class="bg-slate-500/15 text-slate-600 dark:text-slate-400 border-slate-500/20"
                      >
                        Neutral
                      </Badge>
                    {/if}
                  </Table.Cell>
                  <Table.Cell class="text-right tabular-nums text-sm">
                    {row.confidence}%
                  </Table.Cell>
                  <Table.Cell
                    class="text-right text-xs text-muted-foreground whitespace-nowrap"
                  >
                    {fmtDate(row.timestamp)}
                  </Table.Cell>
                </Table.Row>
              {/each}
            {/if}
          </Table.Body>
        </Table.Root>
      </div>
    {/if}
  </Card.Content>

  {#if meta && meta.totalPages > 1}
    <Card.Footer class="flex items-center justify-between pt-4">
      <span class="text-xs text-muted-foreground">
        Page {meta.number + 1} of {meta.totalPages}
      </span>
      <div class="flex gap-1">
        <Button
          variant="outline"
          size="icon"
          onclick={() => fetchPage(page - 1)}
          disabled={loading || page === 0}
          aria-label="Previous page"
        >
          <ChevronLeft class="size-4" />
        </Button>
        <Button
          variant="outline"
          size="icon"
          onclick={() => fetchPage(page + 1)}
          disabled={loading || !meta || page >= meta.totalPages - 1}
          aria-label="Next page"
        >
          <ChevronRight class="size-4" />
        </Button>
      </div>
    </Card.Footer>
  {/if}
</Card.Root>
