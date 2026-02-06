# 🌍 AltWorlds: Your Universe, Your Rules

![Paper Version](https://img.shields.io/badge/Paper-1.21+-blue?style=for-the-badge&logo=paper) ![Java Version](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java) ![Database](https://img.shields.io/badge/Database-None_(YAML)-green?style=for-the-badge)

**AltWorlds** redefines personal world management. Forget about complex, heavy database systems. This is a **professional, lightweight, and fully GUI-based** plugin that allows every player to create, manage, and moderate their own "mini-universes" within your server.

From private **Survival** worlds to collaborative **Creative** plots, including **Custom Templates**. Everything is managed through an intuitive graphical interface, optimized for modern servers.

---

## 🚀 Why choose AltWorlds?

### 🎨 100% GUI Experience
Your players don't need to memorize commands.
*   **Wizard-Style Creation**: Choose type, generator, and name step-by-step.
*   **Icon Selector**: Players can pick the icon for their world (blocks/items) directly from a paginated menu.
*   **In-Game Menu Editor**: Are you an admin? **Edit the main menu icons directly in-game** (Shift+Click in edit mode).

### 🛠️ Deep World Management
It's not just about "creating a world." It's about managing it.
*   **Hierarchical Roles**: `OWNER`, `ADMIN`, `MEMBER`, `VISITOR`.
*   **Self-Moderation**: Owners can invite, kick, and ban (Temp/Perm) users from *their* own worlds.
*   **On-the-fly Toggles**: Enable/disable PvP, Flight, Hunger, Weather, FireTick, Spawning, and more with a single click.

### ⚡ Smart Performance ("The Janitor")
Built on a robust **Lifecycle System**:
*   **Auto-Unload**: Empty worlds are automatically unloaded from RAM after a configurable cooldown.
*   **Session Locking**: Prevents corruption during load/unload cycles or rapid teleports.
*   **No Database**: Everything is stored in clean, portable YAML files (`/data/players/`). Backup and transfer folders with ease.

### 🗺️ Flexible World Types
*   **Vanilla**: Classic Survival experience.
*   **Creative**: With **Flat** or **Void** (Skyblock style) generator options.
*   **Templates**: Create folders with schematics or pre-built maps and let players clone them.
*   **Auto-Worlds**: Automatically assign specific worlds to players (ideal for plot servers or storylines).

---

## 📖 Detailed Features

### 🌟 Social System
*   **World Explorer**: List public worlds sorted by **Likes**, **Visits**, **Online Players**, or **Type**.
*   **Like System**: Let players vote for their favorite worlds!
*   **Invites**: Secure invitation system for private worlds.

### 🏠 Lobby & Home
*   **Central Lobby**: Configure a world as the Lobby. Includes a configurable inventory item to open the menu.
*   **Auto-Join Home**: Players can set their favorite world to join directly upon login, skipping the lobby.

### 🛡️ Administration (Server Worlds)
AltWorlds isn't just for players. As an admin, you can register **"Server Worlds"** (Lobbies, Spawns, RPG maps) so they appear in menus, have their own rules, and benefit from the optimized teleportation system.

---

## 🎮 Commands & Permissions

The plugin is designed to be GUI-first, but includes powerful commands for power users.

### Player Commands (`altworlds.user`)

| Command | Description |
| :--- | :--- |
| `/aw` or `/aw menu` | Opens the Main Control Panel. |
| `/aw tp <world>` | Travel to one of your worlds or a public one. |
| `/aw lobby` | Return to the server Lobby. |
| `/aw invite <player>` | Invite a friend to your private world. |
| `/aw kick <player>` | Kick a player from your world. |
| `/aw ban <player>` | Ban (Temp/Perm) a player from your world. |
| `/aw sethome <world>` | Set your auto-join login world. |
| `/aw setborder <radius>` | Set a visual border for your world. |
| `/aw like` | Give a "Like" to the current world. |

### Admin Commands (`altworlds.admin`)

| Command | Description |
| :--- | :--- |
| `/aw reload` | Reloads configuration and templates. |
| `/aw setlimit <user> <#>`| Changes the world limit for a specific player. |
| `/aw tp <player:world>` | Forced teleport to other players' worlds. |
| **Shift+Click in GUI** | Edit menu icons, regenerate other players' worlds, etc. |

---

## 📂 Installation & Configuration

1.  Download the `.jar` and place it in your `/plugins` folder.
2.  Restart your server (**Paper 1.21+** recommended).
3.  Done! Configuration files will be generated automatically.

### Folder Structure

*   `config.yml`: General settings, limits, unload timers, and messages.
*   `data/`: Stores player data and world settings (YAML).
*   `templates/`: **Important!** Place your world folders (schematics/maps) here to use them as templates.
*   `lobby/`: If you don't have a lobby, AltWorlds can auto-generate a void lobby for you.

### How to add Templates
1.  Copy a world folder (e.g., `SkyblockMap`) into `plugins/AltWorlds/templates/`.
2.  Run `/aw reload` or restart.
3.  It will automatically appear in the "Create World" menu!
4.  *(Optional)* Edit `templates.yml` to change its display name and icon.

---

## ⚙️ Advanced Configuration

AltWorlds allows you to tweak every detail. Example from `config.yml`:

<details>
<summary>📄 <b>Click here to expand the full config.yml</b></summary>

```yaml
lobby:
  # Lobby world name (folder/world id). Values: any world name string.
  worldName: "lobby"
  # Auto-create the lobby world if it does not exist. Values: true|false.
  autoCreate: true

  # Menu item configuration (clock by default).
  item:
    # Give the menu item to players when they join. Values: true|false.
    giveOnJoin: true
    # Material for the menu item. Values: any Bukkit Material name.
    material: "CLOCK"
    # Inventory slot for the menu item (hotbar). Values: 0-8.
    slot: 8
    # Where to give the item. Values: LOBBY_ONLY or EVERYWHERE.
    # LOBBY_ONLY: only while in lobby world (removed on exit).
    # EVERYWHERE: in any world (useful for hubs).
    scope: "EVERYWHERE"

gui:
  # Admin menu icons. Values: any Bukkit Material name.
  admin:
    # Icon for "View All Worlds".
    view_all: COMMAND_BLOCK
    # Icon for "Global Limit".
    global_limit: ANVIL
    # Icon for "Manage Players".
    manage_players: PLAYER_HEAD
    # Icon for "Customize This Menu".
    customize: PAINTING

  # Main menu icons. Values: any Bukkit Material name.
  main:
    # Icon for "My Worlds".
    my_worlds: WRITABLE_BOOK
    # Icon for "Explore Worlds".
    explore: COMPASS
    # Icon for "Create World".
    create: GRASS_BLOCK
    # Icon for "Lobby".
    lobby: BEACON
    # Icon for "Global World Icons".
    global_settings: COMPARATOR
    # Icon for "Server Worlds".
    server_worlds: NETHER_STAR
    # Icon for "Admin Settings".
    admin: COMMAND_BLOCK
    # Icon for "Auto-Join Home".
    auto_join: REPEATER
    # Icon for "Current World".
    current_world: COMPARATOR
    # Icon for "Customize Menu".
    customize: PAINTING

  # Explore menu icons. Values: any Bukkit Material name.
  explore:
    # Icon for "Sort: Players".
    sort_players: PLAYER_HEAD
    # Icon for "Sort: Likes".
    sort_likes: EMERALD
    # Icon for "Sort: Visits".
    sort_visits: OAK_SIGN
    # Icon for "Sort: Type".
    sort_type: PAPER
    # Icon for "Customize Menu".
    customize: PAINTING

  # Create menu icons (categories). Values: any Bukkit Material name.
  create:
    # Icon for "Vanilla".
    vanilla: OAK_SAPLING
    # Icon for "Templates".
    templates: PAPER
    # Icon for "Creative".
    creative: DIAMOND_BLOCK
    # Icon for "Admin / Private".
    admin: COMMAND_BLOCK
    # Icon for "Customize Menu".
    customize: PAINTING

  # Creative options menu icons. Values: any Bukkit Material name.
  creative:
    # Icon for "Vanilla Creative".
    vanilla: GRASS_BLOCK
    # Icon for "Flat Creative".
    flat: SANDSTONE
    # Icon for "Void Creative".
    void: GLASS
    # Icon for "Customize Menu".
    customize: PAINTING

  # World settings icons. Values: any Bukkit Material name.
  settings:
    # Icon for "Gamemode".
    gamemode: GOLDEN_APPLE
    # Icon for "PvP".
    pvp: IRON_SWORD
    # Icon for "Fly".
    fly: FEATHER
    # Icon for "Hunger".
    hunger: ROTTEN_FLESH
    # Icon for "Fall Damage".
    fallDamage: LEATHER_BOOTS
    # Icon for "Potions".
    potions: BREWING_STAND
    # Icon for "Allow Visitors".
    allowVisitors: OAK_DOOR
    # Icon for "Public Listing".
    public: GLOBE_BANNER_PATTERN
    # Icon for "Day/Night Cycle".
    doDaylightCycle: CLOCK
    # Icon for "Weather Cycle".
    doWeatherCycle: WATER_BUCKET
    # Icon for "Fire Spread".
    doFireTick: FLINT_AND_STEEL
    # Icon for "Mob Griefing".
    mobGriefing: CREEPER_HEAD
    # Icon for "Keep Inventory".
    keepInventory: CHEST
    # Icon for "Mob Spawning".
    doMobSpawning: ZOMBIE_HEAD
    # Icon for "Block Drops".
    doTileDrops: GRAVEL
    # Icon for "Crop Growth".
    randomTickSpeed: WHEAT
    # Icon for "World Border".
    border: IRON_BARS

login:
  # If true, always skip the lobby on join. Values: true|false.
  forceSkipLobby: false
  # If true, players can toggle auto-join home from the GUI. Values: true|false.
  allowUserSkipToggle: false

# -------------------------------------------------------------
# AUTO-WORLDS
# -------------------------------------------------------------
auto-worlds:
  # Enable auto-worlds. Values: true|false.
  enabled: true
  # If true, auto-worlds are copied for each join. Values: true|false.
  copyOnEveryJoin: true

  # Whether auto-worlds count toward the player's max world limit. Values: true|false.
  countTowardsLimit: false

  # Special permissions for auto-worlds.
  permissions:
    # Allow users to delete auto-worlds. Values: true|false.
    allowDelete: false
    # Allow users to regenerate auto-worlds. Values: true|false.
    allowRegen: false

# -------------------------------------------------------------
# ALL-WORLDS
# -------------------------------------------------------------
all-worlds:
  # Whether server-wide worlds count toward player limits. Values: true|false.
  countTowardsLimit: false

# -------------------------------------------------------------
# TELEPORTATION
# -------------------------------------------------------------
teleport:
  # Teleport delay in seconds. Values: integer >= 0.
  delay: 3
  # Cancel teleport if player moves. Values: true|false.
  cancelOnMove: false

# -------------------------------------------------------------
# RESTRICTIONS
# -------------------------------------------------------------
restrictions:
  # Allow renaming worlds. Values: true|false.
  allowRenaming: true
  creation:
    # Allow survival world creation. Values: true|false.
    allowSurvival: true
    # Allow template world creation. Values: true|false.
    allowTemplates: true
    # Allow creative world creation. Values: true|false.
    allowCreative: true
    creative:
      # Allow vanilla creative worlds. Values: true|false.
      vanilla: true
      # Allow flat creative worlds. Values: true|false.
      flat: true
      # Allow void creative worlds. Values: true|false.
      void: true

  names:
    # Minimum world name length. Values: integer >= 1.
    minLength: 3
    # Maximum world name length. Values: integer >= minLength.
    maxLength: 16
    # Regex for allowed names. Values: regex string.
    regex: "^[a-zA-Z0-9_-]+$"
    # Error message for invalid names (supports color codes). Values: string.
    errorMessage: "&cInvalid name! Use a-z, 0-9, _ or - (3-16 chars)."

# -------------------------------------------------------------
# LIMITS & INVITATIONS
# -------------------------------------------------------------
limits:
  # Default max worlds per player. Values: integer >= -1 (-1 = unlimited).
  maxWorldsPerPlayer: 3
  # If true, joined/invited worlds count toward max limit. Values: true|false.
  invitedWorldsCount: false

invitations:
  # Invitation expiration in seconds. Values: integer >= 0.
  timeout: 60

# -------------------------------------------------------------
# BANS
# -------------------------------------------------------------
bans:
  # Presets for temporary ban menu. Format examples: 30s, 10m, 2h, 1d.
  temp-presets:
    - "5m"
    - "30m"
    - "1h"
    - "6h"
    - "1d"
    - "7d"

defaults:
  # Default max worlds per player (fallback). Values: integer >= -1.
  maxWorldsPerPlayer: 3
  # Default member gamemode. Values: SURVIVAL|CREATIVE|ADVENTURE|SPECTATOR.
  memberGamemode: "SURVIVAL"
  # Default visitor gamemode. Values: SURVIVAL|CREATIVE|ADVENTURE|SPECTATOR.
  visitorGamemode: "SPECTATOR"

# Default settings used when generating new world configs.
world-defaults:
  survival:
    # Default gamemode for survival worlds. Values: SURVIVAL|CREATIVE|ADVENTURE|SPECTATOR.
    gamemode: SURVIVAL
    # Allow PvP. Values: true|false.
    pvp: true
    # Allow flight. Values: true|false.
    fly: false
    # Enable hunger loss. Values: true|false.
    hunger: true
    # Enable fall damage. Values: true|false.
    fallDamage: true
    # Allow potion usage. Values: true|false.
    potions: true
    # Allow visitors to join. Values: true|false.
    allowVisitors: true
    # Show in public listings. Values: true|false.
    public: true
    # Day/night cycle. Values: true|false.
    doDaylightCycle: true
    # Weather cycle. Values: true|false.
    doWeatherCycle: true
    # Fire spread. Values: true|false.
    doFireTick: true
    # Mob griefing (creepers, etc.). Values: true|false.
    mobGriefing: true
    # Keep inventory on death. Values: true|false.
    keepInventory: true
    # Natural mob spawning. Values: true|false.
    doMobSpawning: true
    # Block drops on break. Values: true|false.
    doTileDrops: true
    # Crop growth (tick speed). Values: true|false.
    randomTickSpeed: true

  creative:
    # Default gamemode for creative worlds. Values: SURVIVAL|CREATIVE|ADVENTURE|SPECTATOR.
    gamemode: CREATIVE
    # Allow PvP. Values: true|false.
    pvp: false
    # Allow flight. Values: true|false.
    fly: true
    # Enable hunger loss. Values: true|false.
    hunger: false
    # Enable fall damage. Values: true|false.
    fallDamage: false
    # Allow potion usage. Values: true|false.
    potions: true
    # Allow visitors to join. Values: true|false.
    allowVisitors: true
    # Show in public listings. Values: true|false.
    public: true
    # Day/night cycle. Values: true|false.
    doDaylightCycle: true
    # Weather cycle. Values: true|false.
    doWeatherCycle: false
    # Fire spread. Values: true|false.
    doFireTick: false
    # Mob griefing (creepers, etc.). Values: true|false.
    mobGriefing: false
    # Keep inventory on death. Values: true|false.
    keepInventory: true
    # Natural mob spawning. Values: true|false.
    doMobSpawning: false
    # Block drops on break. Values: true|false.
    doTileDrops: false
    # Crop growth (tick speed). Values: true|false.
    randomTickSpeed: false

  templates:
    # Default gamemode for template worlds. Values: SURVIVAL|CREATIVE|ADVENTURE|SPECTATOR.
    gamemode: SURVIVAL
    # Allow PvP. Values: true|false.
    pvp: true
    # Allow flight. Values: true|false.
    fly: false
    # Enable hunger loss. Values: true|false.
    hunger: true
    # Enable fall damage. Values: true|false.
    fallDamage: true
    # Allow potion usage. Values: true|false.
    potions: true
    # Allow visitors to join. Values: true|false.
    allowVisitors: true
    # Show in public listings. Values: true|false.
    public: true
    # Day/night cycle. Values: true|false.
    doDaylightCycle: true
    # Weather cycle. Values: true|false.
    doWeatherCycle: true
    # Fire spread. Values: true|false.
    doFireTick: true
    # Mob griefing (creepers, etc.). Values: true|false.
    mobGriefing: true
    # Keep inventory on death. Values: true|false.
    keepInventory: true
    # Natural mob spawning. Values: true|false.
    doMobSpawning: true
    # Block drops on break. Values: true|false.
    doTileDrops: true
    # Crop growth (tick speed). Values: true|false.
    randomTickSpeed: true

  all-worlds:
    # Default gamemode for server/all worlds. Values: SURVIVAL|CREATIVE|ADVENTURE|SPECTATOR.
    gamemode: SURVIVAL
    # Allow PvP. Values: true|false.
    pvp: true
    # Allow flight. Values: true|false.
    fly: false
    # Enable hunger loss. Values: true|false.
    hunger: true
    # Enable fall damage. Values: true|false.
    fallDamage: true
    # Allow potion usage. Values: true|false.
    potions: true
    # Allow visitors to join. Values: true|false.
    allowVisitors: true
    # Show in public listings. Values: true|false.
    public: true
    # Day/night cycle. Values: true|false.
    doDaylightCycle: true
    # Weather cycle. Values: true|false.
    doWeatherCycle: true
    # Fire spread. Values: true|false.
    doFireTick: true
    # Mob griefing (creepers, etc.). Values: true|false.
    mobGriefing: true
    # Keep inventory on death. Values: true|false.
    keepInventory: true
    # Natural mob spawning. Values: true|false.
    doMobSpawning: true
    # Block drops on break. Values: true|false.
    doTileDrops: true
    # Crop growth (tick speed). Values: true|false.
    randomTickSpeed: true

  auto-worlds:
    # Default gamemode for auto-worlds. Values: SURVIVAL|CREATIVE|ADVENTURE|SPECTATOR.
    gamemode: SURVIVAL
    # Allow PvP. Values: true|false.
    pvp: true
    # Allow flight. Values: true|false.
    fly: false
    # Enable hunger loss. Values: true|false.
    hunger: true
    # Enable fall damage. Values: true|false.
    fallDamage: true
    # Allow potion usage. Values: true|false.
    potions: true
    # Allow visitors to join. Values: true|false.
    allowVisitors: true
    # Show in public listings. Values: true|false.
    public: true
    # Day/night cycle. Values: true|false.
    doDaylightCycle: true
    # Weather cycle. Values: true|false.
    doWeatherCycle: true
    # Fire spread. Values: true|false.
    doFireTick: true
    # Mob griefing (creepers, etc.). Values: true|false.
    mobGriefing: true
    # Keep inventory on death. Values: true|false.
    keepInventory: true
    # Natural mob spawning. Values: true|false.
    doMobSpawning: true
    # Block drops on break. Values: true|false.
    doTileDrops: true
    # Crop growth (tick speed). Values: true|false.
    randomTickSpeed: true

  private-worlds:
    # Default gamemode for private templates. Values: SURVIVAL|CREATIVE|ADVENTURE|SPECTATOR.
    gamemode: CREATIVE
    # Allow PvP. Values: true|false.
    pvp: false
    # Allow flight. Values: true|false.
    fly: true
    # Enable hunger loss. Values: true|false.
    hunger: false
    # Enable fall damage. Values: true|false.
    fallDamage: false
    # Allow potion usage. Values: true|false.
    potions: true
    # Allow visitors to join. Values: true|false.
    allowVisitors: true
    # Show in public listings. Values: true|false.
    public: true
    # Day/night cycle. Values: true|false.
    doDaylightCycle: true
    # Weather cycle. Values: true|false.
    doWeatherCycle: false
    # Fire spread. Values: true|false.
    doFireTick: false
    # Mob griefing (creepers, etc.). Values: true|false.
    mobGriefing: false
    # Keep inventory on death. Values: true|false.
    keepInventory: true
    # Natural mob spawning. Values: true|false.
    doMobSpawning: false
    # Block drops on break. Values: true|false.
    doTileDrops: false
    # Crop growth (tick speed). Values: true|false.
    randomTickSpeed: false

  lobby:
    # Default gamemode for lobby. Values: SURVIVAL|CREATIVE|ADVENTURE|SPECTATOR.
    gamemode: ADVENTURE
    # Allow PvP. Values: true|false.
    pvp: false
    # Allow flight. Values: true|false.
    fly: false
    # Enable hunger loss. Values: true|false.
    hunger: false
    # Enable fall damage. Values: true|false.
    fallDamage: false
    # Allow potion usage. Values: true|false.
    potions: false
    # Allow visitors to join. Values: true|false.
    allowVisitors: true
    # Show in public listings. Values: true|false.
    public: false
    # Day/night cycle. Values: true|false.
    doDaylightCycle: false
    # Weather cycle. Values: true|false.
    doWeatherCycle: false
    # Fire spread. Values: true|false.
    doFireTick: false
    # Mob griefing (creepers, etc.). Values: true|false.
    mobGriefing: false
    # Keep inventory on death. Values: true|false.
    keepInventory: true
    # Natural mob spawning. Values: true|false.
    doMobSpawning: false
    # Block drops on break. Values: true|false.
    doTileDrops: false
    # Crop growth (tick speed). Values: true|false.
    randomTickSpeed: false

performance:
  # Unload worlds when empty. Values: true|false.
  unloadWorldsWhenEmpty: true
  # Delay before unloading empty worlds (seconds). Values: integer >= 0.
  unloadDelaySeconds: 45
  # Minimum uptime before a world can be unloaded (seconds). Values: integer >= 0.
  minWorldUptimeSeconds: 60
  # Preload spawn chunks when creating a world. Values: true|false.
  preloadSpawnChunksOnCreate: true
  </details>
```