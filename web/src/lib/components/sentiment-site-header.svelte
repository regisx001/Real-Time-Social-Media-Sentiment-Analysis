<script lang="ts">
  import { Separator } from "$lib/components/ui/separator/index.js";
  import * as Sidebar from "$lib/components/ui/sidebar/index.js";
  import { connectionState } from "$lib/stores/health";
</script>

<header
  class="flex h-(--header-height) shrink-0 items-center gap-2 border-b transition-[width,height] ease-linear group-has-data-[collapsible=icon]/sidebar-wrapper:h-(--header-height)"
>
  <div class="flex w-full items-center gap-1 px-4 lg:gap-2 lg:px-6">
    <Sidebar.Trigger class="-ms-1" />
    <Separator
      orientation="vertical"
      class="mx-2 data-[orientation=vertical]:h-4"
    />
    <h1 class="text-base font-medium">Sentiment Dashboard</h1>
    <div class="ms-auto flex items-center gap-2">
      <div
        class="flex items-center gap-2 border px-2.5 py-1 rounded-full bg-muted/50 text-sm"
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
            class="font-medium text-emerald-600 dark:text-emerald-400 hidden sm:inline"
            >Live</span
          >
        {:else if $connectionState === "error"}
          <span class="relative inline-flex rounded-full h-2 w-2 bg-red-500"
          ></span>
          <span class="font-medium text-red-500 hidden sm:inline"
            >Reconnecting</span
          >
        {:else}
          <span
            class="relative inline-flex rounded-full h-2 w-2 bg-amber-400 animate-pulse"
          ></span>
          <span class="font-medium text-amber-500 hidden sm:inline"
            >Connecting</span
          >
        {/if}
      </div>
    </div>
  </div>
</header>
