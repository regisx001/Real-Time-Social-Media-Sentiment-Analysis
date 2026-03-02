<script lang="ts">
  import {
    Activity,
    LayoutDashboard,
    BarChart3,
    Twitter,
    Brain,
    Server,
    Settings,
    HelpCircle,
  } from "@lucide/svelte";
  import * as Avatar from "$lib/components/ui/avatar/index.js";
  import * as DropdownMenu from "$lib/components/ui/dropdown-menu/index.js";
  import * as Sidebar from "$lib/components/ui/sidebar/index.js";
  import type { ComponentProps } from "svelte";

  const navMain = [
    { title: "Dashboard", url: "/dashboard", Icon: LayoutDashboard },
    // { title: "Sentiment Analysis", url: "#", Icon: Activity },
    // { title: "Analytics", url: "#", Icon: BarChart3 },
    // { title: "Live Stream", url: "#", Icon: Twitter },
    // { title: "ML Model", url: "#", Icon: Brain },
  ];

  const navSecondary = [
    { title: "Infrastructure", url: "/health", Icon: Server },
    { title: "Settings", url: "#", Icon: Settings },
    { title: "Get Help", url: "#", Icon: HelpCircle },
  ];

  const user = {
    name: "Admin",
    email: "admin@pulsestream.io",
    avatar: "/avatars/shadcn.jpg",
  };

  let { ...restProps }: ComponentProps<typeof Sidebar.Root> = $props();

  const sidebar = Sidebar.useSidebar();
</script>

<Sidebar.Root collapsible="offcanvas" {...restProps}>
  <Sidebar.Header>
    <Sidebar.Menu>
      <Sidebar.MenuItem>
        <Sidebar.MenuButton class="data-[slot=sidebar-menu-button]:!p-1.5">
          {#snippet child({ props })}
            <a href="/dashboard" {...props}>
              <Activity class="!size-5 text-primary" />
              <span class="text-base font-semibold">PulseStream</span>
            </a>
          {/snippet}
        </Sidebar.MenuButton>
      </Sidebar.MenuItem>
    </Sidebar.Menu>
  </Sidebar.Header>

  <Sidebar.Content>
    <Sidebar.Group>
      <Sidebar.GroupContent class="flex flex-col gap-2">
        <Sidebar.Menu>
          {#each navMain as item (item.title)}
            <Sidebar.MenuItem>
              <Sidebar.MenuButton tooltipContent={item.title}>
                {#snippet child({ props })}
                  <a href={item.url} {...props}>
                    <item.Icon class="size-4" />
                    <span>{item.title}</span>
                  </a>
                {/snippet}
              </Sidebar.MenuButton>
            </Sidebar.MenuItem>
          {/each}
        </Sidebar.Menu>
      </Sidebar.GroupContent>
    </Sidebar.Group>

    <Sidebar.Group class="mt-auto">
      <Sidebar.GroupContent>
        <Sidebar.Menu>
          {#each navSecondary as item (item.title)}
            <Sidebar.MenuItem>
              <Sidebar.MenuButton tooltipContent={item.title}>
                {#snippet child({ props })}
                  <a href={item.url} {...props}>
                    <item.Icon class="size-4" />
                    <span>{item.title}</span>
                  </a>
                {/snippet}
              </Sidebar.MenuButton>
            </Sidebar.MenuItem>
          {/each}
        </Sidebar.Menu>
      </Sidebar.GroupContent>
    </Sidebar.Group>
  </Sidebar.Content>

  <Sidebar.Footer>
    <Sidebar.Menu>
      <Sidebar.MenuItem>
        <DropdownMenu.Root>
          <DropdownMenu.Trigger>
            {#snippet child({ props })}
              <Sidebar.MenuButton
                {...props}
                size="lg"
                class="data-[state=open]:bg-sidebar-accent data-[state=open]:text-sidebar-accent-foreground"
              >
                <Avatar.Root class="size-8 rounded-lg grayscale">
                  <Avatar.Image src={user.avatar} alt={user.name} />
                  <Avatar.Fallback class="rounded-lg">AD</Avatar.Fallback>
                </Avatar.Root>
                <div class="grid flex-1 text-start text-sm leading-tight">
                  <span class="truncate font-medium">{user.name}</span>
                  <span class="text-muted-foreground truncate text-xs"
                    >{user.email}</span
                  >
                </div>
              </Sidebar.MenuButton>
            {/snippet}
          </DropdownMenu.Trigger>
          <DropdownMenu.Content
            class="w-(--bits-dropdown-menu-anchor-width) min-w-56 rounded-lg"
            side={sidebar.isMobile ? "bottom" : "right"}
            align="end"
            sideOffset={4}
          >
            <DropdownMenu.Label class="p-0 font-normal">
              <div
                class="flex items-center gap-2 px-1 py-1.5 text-start text-sm"
              >
                <Avatar.Root class="size-8 rounded-lg">
                  <Avatar.Image src={user.avatar} alt={user.name} />
                  <Avatar.Fallback class="rounded-lg">AD</Avatar.Fallback>
                </Avatar.Root>
                <div class="grid flex-1 text-start text-sm leading-tight">
                  <span class="truncate font-medium">{user.name}</span>
                  <span class="text-muted-foreground truncate text-xs"
                    >{user.email}</span
                  >
                </div>
              </div>
            </DropdownMenu.Label>
            <DropdownMenu.Separator />
            <DropdownMenu.Item>
              <Settings class="size-4 mr-2" /> Settings
            </DropdownMenu.Item>
            <DropdownMenu.Separator />
            <DropdownMenu.Item>
              <HelpCircle class="size-4 mr-2" /> Get Help
            </DropdownMenu.Item>
          </DropdownMenu.Content>
        </DropdownMenu.Root>
      </Sidebar.MenuItem>
    </Sidebar.Menu>
  </Sidebar.Footer>
</Sidebar.Root>
