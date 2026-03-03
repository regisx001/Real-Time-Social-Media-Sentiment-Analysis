<script lang="ts">
  import SentimentSectionCards from "$lib/components/sentiment-section-cards.svelte";
  import ChartAreaSentiment from "$lib/components/chart-area-sentiment.svelte";
  import SentimentSiteHeader from "$lib/components/sentiment-site-header.svelte";
</script>

<SentimentSiteHeader />

<div class="flex flex-col gap-4 py-4 md:gap-6 md:py-6">
  <SentimentSectionCards />
  <div class="px-4 lg:px-6">
    <ChartAreaSentiment />
  </div>
</div>
