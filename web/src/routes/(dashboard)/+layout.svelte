<script lang="ts">
  import * as Sidebar from "$lib/components/ui/sidebar/index.js";
  import SentimentAppSidebar from "$lib/components/sentiment-app-sidebar.svelte";
  import SentimentSectionCards from "$lib/components/sentiment-section-cards.svelte";
  import ChartAreaSentiment from "$lib/components/chart-area-sentiment.svelte";
  let { children } = $props();
</script>

<Sidebar.Provider
  style="--sidebar-width: calc(var(--spacing) * 72); --header-height: calc(var(--spacing) * 12);"
>
  <SentimentAppSidebar variant="inset" />
  <Sidebar.Inset>
    <div class="flex flex-1 flex-col">
      <div class="@container/main flex flex-1 flex-col gap-2">
        <!-- <div class="flex flex-col gap-4 py-4 md:gap-6 md:py-6">
          <SentimentSectionCards />
          <div class="px-4 lg:px-6">
            <ChartAreaSentiment />
          </div>
        </div> -->

        {@render children()}
      </div>
    </div>
  </Sidebar.Inset>
</Sidebar.Provider>
