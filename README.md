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

## 📸 Gallery

<!-- Upload your images to imgur or github and replace these links -->
![Main Menu Preview](https://via.placeholder.com/700x300?text=Preview+Main+Menu+GUI)
![World Settings](https://via.placeholder.com/700x300?text=Preview+World+Settings)

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

```yaml
limits:
  maxWorldsPerPlayer: 3  # Default limit (admins can change this per player)

performance:
  unloadWorldsWhenEmpty: true
  unloadDelaySeconds: 45 # Time before unloading an empty world
  minWorldUptimeSeconds: 60 # Minimum uptime for a world after loading

restrictions:
  creation:
    allowCreative: true
    allowTemplates: true