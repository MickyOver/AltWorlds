package com.altworlds;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.*;

public class AltWorldsGUI implements Listener {
    private final AltWorldsPlugin plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    // Datos temporales
    private final Set<UUID> pendingNameInput = new HashSet<>();
    private final Map<UUID, WizardData> creationData = new HashMap<>();
    private final Map<UUID, String> pendingRename = new HashMap<>();
    private final Map<UUID, String> pendingBorderInput = new HashMap<>();
    private final Map<UUID, String> explorerSort = new HashMap<>();
    private final Set<UUID> pendingGlobalLimitInput = new HashSet<>();
    private final Map<UUID, UUID> pendingPlayerLimitSet = new HashMap<>();
    private final Map<UUID, Runnable> pendingConfirm = new HashMap<>();
    private final Map<UUID, BanRequest> pendingBanInput = new HashMap<>();
    private final Map<UUID, BanRequest> pendingBanTempSelect = new HashMap<>();
    private final Map<UUID, BanRequest> pendingBanConfirm = new HashMap<>();
    private final Map<UUID, CloneRequest> pendingCloneInput = new HashMap<>();

    private static final List<Material> ICON_MATERIALS = new ArrayList<>();

    static {
        ICON_MATERIALS.add(Material.GRASS_BLOCK); ICON_MATERIALS.add(Material.DIRT); ICON_MATERIALS.add(Material.STONE);
        ICON_MATERIALS.add(Material.COBBLESTONE); ICON_MATERIALS.add(Material.MOSSY_COBBLESTONE); ICON_MATERIALS.add(Material.SAND);
        ICON_MATERIALS.add(Material.RED_SAND); ICON_MATERIALS.add(Material.GRAVEL); ICON_MATERIALS.add(Material.OAK_LOG);
        ICON_MATERIALS.add(Material.SPRUCE_LOG); ICON_MATERIALS.add(Material.BIRCH_LOG); ICON_MATERIALS.add(Material.JUNGLE_LOG);
        ICON_MATERIALS.add(Material.ACACIA_LOG); ICON_MATERIALS.add(Material.DARK_OAK_LOG); ICON_MATERIALS.add(Material.CRIMSON_STEM);
        ICON_MATERIALS.add(Material.WARPED_STEM); ICON_MATERIALS.add(Material.OAK_LEAVES); ICON_MATERIALS.add(Material.OAK_PLANKS);
        ICON_MATERIALS.add(Material.BRICKS); ICON_MATERIALS.add(Material.STONE_BRICKS); ICON_MATERIALS.add(Material.MUD_BRICKS);
        ICON_MATERIALS.add(Material.SNOW_BLOCK); ICON_MATERIALS.add(Material.ICE); ICON_MATERIALS.add(Material.PACKED_ICE);
        ICON_MATERIALS.add(Material.CACTUS); ICON_MATERIALS.add(Material.PUMPKIN); ICON_MATERIALS.add(Material.MELON);
        ICON_MATERIALS.add(Material.COAL_BLOCK); ICON_MATERIALS.add(Material.IRON_BLOCK); ICON_MATERIALS.add(Material.GOLD_BLOCK);
        ICON_MATERIALS.add(Material.DIAMOND_BLOCK); ICON_MATERIALS.add(Material.EMERALD_BLOCK); ICON_MATERIALS.add(Material.LAPIS_BLOCK);
        ICON_MATERIALS.add(Material.NETHERITE_BLOCK); ICON_MATERIALS.add(Material.AMETHYST_BLOCK); ICON_MATERIALS.add(Material.COPPER_BLOCK);
        ICON_MATERIALS.add(Material.RAW_IRON_BLOCK); ICON_MATERIALS.add(Material.RAW_GOLD_BLOCK); ICON_MATERIALS.add(Material.QUARTZ_BLOCK);
        ICON_MATERIALS.add(Material.DIAMOND_SWORD); ICON_MATERIALS.add(Material.IRON_SWORD); ICON_MATERIALS.add(Material.GOLDEN_SWORD);
        ICON_MATERIALS.add(Material.NETHERITE_SWORD); ICON_MATERIALS.add(Material.DIAMOND_PICKAXE); ICON_MATERIALS.add(Material.IRON_AXE);
        ICON_MATERIALS.add(Material.BOW); ICON_MATERIALS.add(Material.CROSSBOW); ICON_MATERIALS.add(Material.ARROW);
        ICON_MATERIALS.add(Material.SHIELD); ICON_MATERIALS.add(Material.TOTEM_OF_UNDYING); ICON_MATERIALS.add(Material.TRIDENT);
        ICON_MATERIALS.add(Material.TNT); ICON_MATERIALS.add(Material.FLINT_AND_STEEL);
        ICON_MATERIALS.add(Material.CLOCK); ICON_MATERIALS.add(Material.COMPASS); ICON_MATERIALS.add(Material.RECOVERY_COMPASS);
        ICON_MATERIALS.add(Material.SPYGLASS); ICON_MATERIALS.add(Material.MAP); ICON_MATERIALS.add(Material.WRITABLE_BOOK);
        ICON_MATERIALS.add(Material.PAPER); ICON_MATERIALS.add(Material.BOOKSHELF); ICON_MATERIALS.add(Material.ENCHANTING_TABLE);
        ICON_MATERIALS.add(Material.ANVIL); ICON_MATERIALS.add(Material.CHEST); ICON_MATERIALS.add(Material.ENDER_CHEST);
        ICON_MATERIALS.add(Material.BARREL); ICON_MATERIALS.add(Material.CRAFTING_TABLE); ICON_MATERIALS.add(Material.FURNACE);
        ICON_MATERIALS.add(Material.BLAST_FURNACE); ICON_MATERIALS.add(Material.SMOKER); ICON_MATERIALS.add(Material.BEACON);
        ICON_MATERIALS.add(Material.CONDUIT); ICON_MATERIALS.add(Material.COMMAND_BLOCK); ICON_MATERIALS.add(Material.REDSTONE_BLOCK);
        ICON_MATERIALS.add(Material.REDSTONE_LAMP); ICON_MATERIALS.add(Material.TARGET); ICON_MATERIALS.add(Material.JUKEBOX);
        ICON_MATERIALS.add(Material.NOTE_BLOCK); ICON_MATERIALS.add(Material.SCAFFOLDING);
        ICON_MATERIALS.add(Material.NETHERRACK); ICON_MATERIALS.add(Material.SOUL_SAND); ICON_MATERIALS.add(Material.MAGMA_BLOCK);
        ICON_MATERIALS.add(Material.GLOWSTONE); ICON_MATERIALS.add(Material.SHROOMLIGHT); ICON_MATERIALS.add(Material.NETHER_BRICKS);
        ICON_MATERIALS.add(Material.RED_NETHER_BRICKS); ICON_MATERIALS.add(Material.OBSIDIAN); ICON_MATERIALS.add(Material.CRYING_OBSIDIAN);
        ICON_MATERIALS.add(Material.BLAZE_ROD); ICON_MATERIALS.add(Material.GHAST_TEAR); ICON_MATERIALS.add(Material.NETHER_STAR);
        ICON_MATERIALS.add(Material.END_STONE); ICON_MATERIALS.add(Material.PURPUR_BLOCK); ICON_MATERIALS.add(Material.END_ROD);
        ICON_MATERIALS.add(Material.ENDER_PEARL); ICON_MATERIALS.add(Material.ENDER_EYE); ICON_MATERIALS.add(Material.DRAGON_EGG);
        ICON_MATERIALS.add(Material.SHULKER_BOX); ICON_MATERIALS.add(Material.ELYTRA); ICON_MATERIALS.add(Material.DRAGON_HEAD);
        ICON_MATERIALS.add(Material.APPLE); ICON_MATERIALS.add(Material.GOLDEN_APPLE); ICON_MATERIALS.add(Material.ENCHANTED_GOLDEN_APPLE);
        ICON_MATERIALS.add(Material.BREAD); ICON_MATERIALS.add(Material.COOKED_BEEF); ICON_MATERIALS.add(Material.CAKE);
        ICON_MATERIALS.add(Material.COOKIE); ICON_MATERIALS.add(Material.SWEET_BERRIES); ICON_MATERIALS.add(Material.GLOW_BERRIES);
        ICON_MATERIALS.add(Material.WATER_BUCKET); ICON_MATERIALS.add(Material.LAVA_BUCKET); ICON_MATERIALS.add(Material.MILK_BUCKET);
        ICON_MATERIALS.add(Material.AXOLOTL_BUCKET); ICON_MATERIALS.add(Material.PAINTING); ICON_MATERIALS.add(Material.ITEM_FRAME);
        ICON_MATERIALS.add(Material.FLOWER_POT); ICON_MATERIALS.add(Material.LANTERN); ICON_MATERIALS.add(Material.SOUL_LANTERN);
        ICON_MATERIALS.add(Material.CAMPFIRE); ICON_MATERIALS.add(Material.SOUL_CAMPFIRE); ICON_MATERIALS.add(Material.CANDLE);
        ICON_MATERIALS.add(Material.WHITE_WOOL); ICON_MATERIALS.add(Material.RED_WOOL); ICON_MATERIALS.add(Material.BLUE_WOOL);
        ICON_MATERIALS.add(Material.LIME_WOOL); ICON_MATERIALS.add(Material.YELLOW_WOOL); ICON_MATERIALS.add(Material.BLACK_WOOL);
        ICON_MATERIALS.add(Material.RED_BED); ICON_MATERIALS.add(Material.CYAN_BED); ICON_MATERIALS.add(Material.GLASS);
        ICON_MATERIALS.add(Material.TINTED_GLASS);
    }

    private static class WizardData { String type; String generator; GameMode mode; }
    private static class BanRequest { UUID ownerUuid; String worldName; UUID targetUuid; String targetName; long durationSeconds; }
    private static class CloneRequest { UUID ownerUuid; String worldName; CloneRequest(UUID o, String w){ ownerUuid=o; worldName=w; } }

    public AltWorldsGUI(AltWorldsPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openMain(Player p) {
        Inventory inv = createMenu(36, "AltWorlds: Main Menu");
        AltWorldsService svc = plugin.getService();

        // Iconos centrales
        inv.setItem(10, createItem(svc.getGuiIcon("main.my_worlds", Material.WRITABLE_BOOK), "§e§lMy Worlds", "§7List your worlds"));
        inv.setItem(12, createItem(svc.getGuiIcon("main.explore", Material.COMPASS), "§6§lExplore Worlds", "§7Visit public worlds\n§7See popular builds"));
        inv.setItem(14, createItem(svc.getGuiIcon("main.create", Material.GRASS_BLOCK), "§a§lCreate World", "§7Start new adventure"));
        inv.setItem(16, createItem(svc.getGuiIcon("main.lobby", Material.BEACON), "§b§lLobby", "§7Return to spawn"));
        inv.setItem(20, createItem(svc.getGuiIcon("main.server_worlds", Material.NETHER_STAR), "§d§lServer Worlds", "§7Official Server Maps\n§7Accessible to everyone."));

        // BotÃ³n Auto-Join
        if (plugin.getConfig().getBoolean("login.allowUserSkipToggle", true)) {
            boolean skipping = svc.getPlayerSetting(p.getUniqueId(), "skipLobby", false);
            String status = skipping ? "§aON" : "§cOFF";
            inv.setItem(24, createItem(svc.getGuiIcon("main.auto_join", Material.REPEATER), "§6§lAuto-Join Home", "§7Skip lobby on join?\n§7Status: " + status));
        }

        // BotÃ³n Current World Settings (Centro)
        String internal = p.getWorld().getName();
        UUID ownerCheck = svc.getOwnerUuidByInternalWorldName(internal);
        boolean isAllWorld = svc.isAllWorld(internal);
        boolean isLobby = svc.isLobbyWorld(internal);
        boolean canManage = (ownerCheck != null && ownerCheck.equals(p.getUniqueId())) || (p.hasPermission("altworlds.admin") && (isAllWorld || isLobby || ownerCheck != null));

        if (canManage) {
            inv.setItem(22, createItem(svc.getGuiIcon("main.current_world", Material.COMPARATOR), "§b§lCurrent World Settings", "§7Manage this world"));
        }

        // --- SECCIÃ“N ADMIN ---
        if (p.hasPermission("altworlds.admin")) {
            // 1. Lobby Item Visualizer (Slot 27 - Abajo Izquierda)
            inv.setItem(27, createItem(svc.getLobbyItemMaterial(), "§eLobby Item", "§7This item is given to players\n§7to open the menu.\n§c(Visual Only)\n§eClick to change material"));

            // 2. NUEVO: Global World Settings (Slot 28 - A la derecha del reloj)
            // Abre el menÃº preview para cambiar iconos por defecto de settings
            inv.setItem(28, createItem(svc.getGuiIcon("main.global_settings", Material.COMPARATOR), "§6Global World Icons", "§7Preview & Edit default icons\n§7for world settings (PvP, Fly, etc)."));

            // 3. Admin Settings (Slot 35 - Abajo Derecha)
            inv.setItem(35, createItem(svc.getGuiIcon("main.admin", Material.COMMAND_BLOCK), "§c§lAdmin Settings", "§7Manage limits & view worlds"));

            // 4. Customize Menu (Slot 8 - Arriba Derecha)
            inv.setItem(8, createItem(svc.getGuiIcon("main.customize", Material.PAINTING), "§9Customize Menu", "§7Click to edit menu icons\n§c(Admins Only)"));
        }

        p.openInventory(inv);
    }

    public void openGuiEditor(Player p) {
        Inventory inv = createMenu(36, "Edit: Main Menu");
        AltWorldsService svc = plugin.getService();
        inv.setItem(10, createItem(svc.getGuiIcon("main.my_worlds", Material.WRITABLE_BOOK), "§eMy Worlds", "§eClick to change icon"));
        inv.setItem(12, createItem(svc.getGuiIcon("main.explore", Material.COMPASS), "§6Explore Worlds", "§eClick to change icon"));
        inv.setItem(14, createItem(svc.getGuiIcon("main.create", Material.GRASS_BLOCK), "§aCreate World", "§eClick to change icon"));
        inv.setItem(16, createItem(svc.getGuiIcon("main.lobby", Material.BEACON), "§bLobby", "§eClick to change icon"));
        inv.setItem(20, createItem(svc.getGuiIcon("main.server_worlds", Material.NETHER_STAR), "§dServer Worlds", "§eClick to change icon"));
        inv.setItem(35, createItem(svc.getGuiIcon("main.admin", Material.COMMAND_BLOCK), "§cAdmin Settings", "§eClick to change icon"));

        // Slot 8: Customize
        inv.setItem(8, createItem(Material.BARRIER, "§cExit Editor", "§7Finish editing icons"));

        inv.setItem(24, createItem(svc.getGuiIcon("main.auto_join", Material.REPEATER), "§6Auto-Join Home", "§eClick to change icon"));
        inv.setItem(22, createItem(svc.getGuiIcon("main.current_world", Material.COMPARATOR), "§bCurrent World", "§eClick to change icon"));

        // Slot 27: Lobby Item (Este cambia el material fÃ­sico, no solo el icono del gui)
        inv.setItem(27, createItem(svc.getLobbyItemMaterial(), "§eLobby Item", "§eClick to change the actual\n§eitem given to players"));

        // Slot 28: Global Settings Icon
        inv.setItem(28, createItem(svc.getGuiIcon("main.global_settings", Material.COMPARATOR), "§6Global World Icons", "§eClick to change icon"));

        p.openInventory(inv);
    }

    public void openServerWorlds(Player p) {
        Inventory inv = createMenu(54, "AltWorlds: Server Worlds");
        List<String> allWorlds = plugin.getService().getAllWorldsList();
        for (String wName : allWorlds) {
            Material icon = plugin.getService().getWorldIcon(AltWorldsService.SERVER_UUID, wName);
            inv.addItem(createItem(icon, "§b" + wName, "§7Click to visit"));
        }
        inv.setItem(53, createItem(Material.ARROW, "§cBack", null));
        p.openInventory(inv);
    }

    private void openAdminSettings(Player p) {
        Inventory inv = createMenu(27, "Admin: Settings");
        AltWorldsService svc = plugin.getService();

        // Usamos svc.getGuiIcon para leer de la config
        inv.setItem(11, createItem(svc.getGuiIcon("admin.view_all", Material.COMMAND_BLOCK), "§eView All Worlds", "§7List every world on the server"));

        int globalLimit = plugin.getConfig().getInt("defaults.maxWorldsPerPlayer", 3);
        inv.setItem(13, createItem(svc.getGuiIcon("admin.global_limit", Material.ANVIL), "§bGlobal Limit", "§7Current Default: §e" + (globalLimit == -1 ? "Unlimited" : globalLimit) + "\n\n§eClick to change via command"));

        inv.setItem(15, createItem(svc.getGuiIcon("admin.manage_players", Material.PLAYER_HEAD), "§6Manage Players", "§7View players details\n§7Change specific limits\n\n§eClick to select player"));

        // BotÃ³n de Customizar este menÃº
        inv.setItem(8, createItem(svc.getGuiIcon("admin.customize", Material.PAINTING), "§9Customize This Menu", "§7Click to edit icons"));

        inv.setItem(26, createItem(Material.ARROW, "§cBack", null));
        p.openInventory(inv);
    }

    public void openAdminGuiEditor(Player p) {
        Inventory inv = createMenu(27, "Edit: Admin Menu");
        AltWorldsService svc = plugin.getService();

        inv.setItem(11, createItem(svc.getGuiIcon("admin.view_all", Material.COMMAND_BLOCK), "§eView All Worlds", "§eClick to change icon (admin.view_all)"));
        inv.setItem(13, createItem(svc.getGuiIcon("admin.global_limit", Material.ANVIL), "§bGlobal Limit", "§eClick to change icon (admin.global_limit)"));
        inv.setItem(15, createItem(svc.getGuiIcon("admin.manage_players", Material.PLAYER_HEAD), "§6Manage Players", "§eClick to change icon (admin.manage_players)"));
        inv.setItem(8, createItem(Material.BARRIER, "§cExit Editor", "§7Finish editing icons"));

        p.openInventory(inv);
    }

    private void openPlayerList(Player p) {
        Inventory inv = createMenu(54, "Admin: Player List");
        Map<UUID, String> players = plugin.getService().getKnownPlayers();
        int slot = 0;
        for (Map.Entry<UUID, String> entry : players.entrySet()) {
            if (slot >= 53) break;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = getSkullMeta(head);
            if (meta == null) continue;
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.getKey()));
            meta.displayName(c("§e" + entry.getValue()));
            List<String> lore = new ArrayList<>();
            lore.add("§7Click to view details");
            meta.lore(toComponents(lore));
            head.setItemMeta(meta);
            inv.setItem(slot++, head);
        }
        inv.setItem(53, createItem(Material.ARROW, "§cBack", null));
        p.openInventory(inv);
    }

    private void openPlayerInspector(Player admin, UUID targetUuid) {
        YamlConfiguration config = plugin.getService().getPlayerConfig(targetUuid);
        String name = config.getString("name", "Unknown");
        Inventory inv = createMenu(27, "Inspect: " + name);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = getSkullMeta(head);
        if (meta == null) {
            admin.openInventory(inv);
            return;
        }
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(targetUuid));
        meta.displayName(c("§e" + name));
        List<String> lore = new ArrayList<>();
        long lastLogin = config.getLong("lastLogin", 0);
        String lastLoginStr = (lastLogin > 0) ? dateFormat.format(new Date(lastLogin)) : "Never";
        int limit = plugin.getService().getMaxWorldsForPlayer(targetUuid);
        int count = plugin.getService().listPlayerWorlds(targetUuid).size();
        lore.add("");
        lore.add("§7Last Server Login: §b" + lastLoginStr);
        lore.add("§7Worlds Owned: §b" + count + " / " + (limit == -1 ? "Unlimited" : limit));
        lore.add("");
        lore.add("§eClick to change World Limit");
        meta.lore(toComponents(lore));
        head.setItemMeta(meta);
        inv.setItem(11, head);
        inv.setItem(15, createItem(Material.WRITABLE_BOOK, "§aView Player Worlds", "§7Click to see a list of\n§7all worlds owned by this player.\n§7(Creation date, Last visit, etc)"));
        inv.setItem(26, createItem(Material.ARROW, "§cBack", null));
        admin.openInventory(inv);
    }

    private void openPlayerWorldsList(Player admin, UUID targetUuid) {
        YamlConfiguration config = plugin.getService().getPlayerConfig(targetUuid);
        String name = config.getString("name", "Unknown");
        Inventory inv = createMenu(54, "Worlds: " + name);
        List<String> worlds = plugin.getService().listPlayerWorlds(targetUuid);
        for (String wName : worlds) {
            String path = "worlds." + wName;
            long created = config.getLong(path + ".created", 0);
            long lastVisit = config.getLong(path + ".lastVisit", 0);
            String type = plugin.getService().getCreationType(targetUuid, wName);
            int visits = plugin.getService().getVisits(targetUuid, wName);
            Material icon = plugin.getService().getWorldIcon(targetUuid, wName);
            List<String> lore = new ArrayList<>();
            lore.add("§7Type: §f" + type);
            lore.add("§7Created: §e" + (created > 0 ? dateFormat.format(new Date(created)) : "Unknown"));
            lore.add("§7Last Owner Visit: §e" + (lastVisit > 0 ? dateFormat.format(new Date(lastVisit)) : "Unknown"));
            lore.add("§7Total Visits: §b" + visits);
            lore.add("");
            lore.add("§eClick to Teleport");
            lore.add("§eShift-Click to Manage Members");
            inv.addItem(createItem(icon, "§a" + wName, String.join("\n", lore)));
        }
        inv.setItem(53, createItem(Material.ARROW, "§cBack", null));
        admin.openInventory(inv);

    }

    public void openAdminAllWorlds(Player p) {
        String sort = explorerSort.getOrDefault(p.getUniqueId(), "players");
        Inventory inv = createMenu(54, "Admin: All Worlds (" + capitalize(sort) + ")");
        AltWorldsService svc = plugin.getService();

        // Botones de ordenamiento (Mismos que Explorer)
        inv.setItem(0, createItem(svc.getGuiIcon("explore.sort_players", Material.PLAYER_HEAD), "§eSort: Players", "§7Sort by online"));
        inv.setItem(1, createItem(svc.getGuiIcon("explore.sort_likes", Material.EMERALD), "§aSort: Likes", "§7Sort by likes"));
        inv.setItem(2, createItem(svc.getGuiIcon("explore.sort_visits", Material.OAK_SIGN), "§bSort: Visits", "§7Sort by visits"));
        inv.setItem(3, createItem(svc.getGuiIcon("explore.sort_type", Material.PAPER), "§dSort: Type", "§7Sort by type"));

        // Obtener TODOS los mundos (privados y pÃºblicos)
        List<AltWorldsService.WorldInfo> worlds = svc.getAllWorldsInfo(sort);

        int slot = 9;
        for (AltWorldsService.WorldInfo info : worlds) {
            if (slot >= 53) break;

            // Usar el icono real del mundo
            Material iconMat = svc.getWorldIcon(info.ownerUuid, info.wName);

            ItemStack icon = new ItemStack(iconMat);
            ItemMeta meta = icon.getItemMeta();
            if (meta == null) continue;
            meta.displayName(c("§6" + info.ownerName + "§7: §b" + info.wName));
            List<String> lore = new ArrayList<>();
            lore.add("§7Type: " + svc.getCreationType(info.ownerUuid, info.wName));
            lore.add("§7Online: " + info.currentPlayers);
            lore.add("§7Likes: " + info.likes);
            lore.add("§7Click to Teleport");
            meta.lore(toComponents(lore));
            icon.setItemMeta(meta);

            inv.setItem(slot++, icon);
        }
        inv.setItem(53, createItem(Material.ARROW, "§cBack", null));
        p.openInventory(inv);
    }

    public void openExplorer(Player p) {
        String sort = explorerSort.getOrDefault(p.getUniqueId(), "players");
        Inventory inv = createMenu(54, "Explore: " + capitalize(sort));
        AltWorldsService svc = plugin.getService();

        // Iconos desde config
        inv.setItem(0, createItem(svc.getGuiIcon("explore.sort_players", Material.PLAYER_HEAD), "§eSort: Players", "§7Click to sort by online"));
        inv.setItem(1, createItem(svc.getGuiIcon("explore.sort_likes", Material.EMERALD), "§aSort: Likes", "§7Click to sort by likes"));
        inv.setItem(2, createItem(svc.getGuiIcon("explore.sort_visits", Material.OAK_SIGN), "§bSort: Visits", "§7Click to sort by total visits"));
        inv.setItem(3, createItem(svc.getGuiIcon("explore.sort_type", Material.PAPER), "§dSort: Type", "§7Click to sort by type"));

        // Solo admins ven el botÃ³n de editar
        if (p.hasPermission("altworlds.admin")) {
            inv.setItem(8, createItem(svc.getGuiIcon("explore.customize", Material.PAINTING), "§9Customize Menu", "§7Edit icons"));
        }

        List<AltWorldsService.WorldInfo> worlds = svc.getPublicWorlds(sort);
        int slot = 9;
        for (AltWorldsService.WorldInfo info : worlds) {
            if (slot >= 53) break;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = getSkullMeta(head);
            if (meta == null) continue;
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(info.ownerUuid));
            meta.displayName(c("§b" + info.wName));
            String type = svc.getCreationType(info.ownerUuid, info.wName);
            List<String> lore = new ArrayList<>();
            lore.add("§7Owner: §f" + info.ownerName);
            lore.add("§7Type: §b" + type);
            lore.add("§7Online: §a" + info.currentPlayers);
            lore.add("§7Likes: §d" + info.likes);
            lore.add("§7Visits: §e" + info.visits);
            lore.add("");
            lore.add("§eClick to Visit!");
            lore.add("§eShift-Click to Like/Unlike");
            meta.lore(toComponents(lore));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            head.setItemMeta(meta);
            inv.setItem(slot++, head);
        }
        inv.setItem(53, createItem(Material.ARROW, "§cBack", null));
        p.openInventory(inv);
    }

    private void openCategorySelector(Player p) {
        Inventory inv = createMenu(27, "Create: Select Category");
        AltWorldsService svc = plugin.getService();
        boolean allowSurvival = plugin.getConfig().getBoolean("restrictions.creation.allowSurvival", true);
        boolean allowTemplates = plugin.getConfig().getBoolean("restrictions.creation.allowTemplates", true);
        boolean allowCreative = plugin.getConfig().getBoolean("restrictions.creation.allowCreative", true);
        boolean isAdmin = p.hasPermission("altworlds.admin");

        String adminLore = "\n\n§c[Admin]\n§eShift-Click to Toggle Global";

        if (allowSurvival || isAdmin) inv.setItem(11, createItem(svc.getGuiIcon("create.vanilla", Material.OAK_SAPLING), "§a§lVanilla", "§7Survival World\n" + (allowSurvival?"§a(Enabled)":"§c(Disabled)") + (isAdmin?adminLore:"")));
        if (allowTemplates || isAdmin) inv.setItem(13, createItem(svc.getGuiIcon("create.templates", Material.PAPER), "§d§lTemplates", "§7Custom Maps\n" + (allowTemplates?"§a(Enabled)":"§c(Disabled)") + (isAdmin?adminLore:"")));
        if (allowCreative || isAdmin) inv.setItem(15, createItem(svc.getGuiIcon("create.creative", Material.DIAMOND_BLOCK), "§b§lCreative", "§7Creative Mode\n" + (allowCreative?"§a(Enabled)":"§c(Disabled)") + (isAdmin?adminLore:"")));

        if (isAdmin) {
            inv.setItem(22, createItem(svc.getGuiIcon("create.admin", Material.COMMAND_BLOCK), "§c§lAdmin / Private", "§7Internal Templates\n§c(Admins Only)"));
            // BotÃ³n Customizar
            inv.setItem(8, createItem(svc.getGuiIcon("create.customize", Material.PAINTING), "§9Customize Menu", "§7Edit icons"));
        }

        inv.setItem(26, createItem(Material.ARROW, "§cBack", null));
        p.openInventory(inv);
    }

    private void openCreativeOptions(Player p) {
        Inventory inv = createMenu(27, "Create: Creative Options");
        AltWorldsService svc = plugin.getService();
        boolean allowVanilla = plugin.getConfig().getBoolean("restrictions.creation.creative.vanilla", true);
        boolean allowFlat = plugin.getConfig().getBoolean("restrictions.creation.creative.flat", true);
        boolean allowVoid = plugin.getConfig().getBoolean("restrictions.creation.creative.void", true);
        boolean isAdmin = p.hasPermission("altworlds.admin");

        String adminLore = "\n\n§c[Admin]\n§eShift-Click to Toggle";
        if (allowVanilla || isAdmin) inv.setItem(11, createItem(svc.getGuiIcon("creative.vanilla", Material.GRASS_BLOCK), "§bVanilla Creative", "§7Normal terrain\n" + (allowVanilla?"§a(Enabled)":"§c(Disabled)") + (isAdmin?adminLore:"")));
        if (allowFlat || isAdmin) inv.setItem(13, createItem(svc.getGuiIcon("creative.flat", Material.SANDSTONE), "§bFlat Creative", "§7Superflat\n" + (allowFlat?"§a(Enabled)":"§c(Disabled)") + (isAdmin?adminLore:"")));
        if (allowVoid || isAdmin) inv.setItem(15, createItem(svc.getGuiIcon("creative.void", Material.GLASS), "§bVoid Creative", "§7Empty world\n" + (allowVoid?"§a(Enabled)":"§c(Disabled)") + (isAdmin?adminLore:"")));

        if (isAdmin) {
            inv.setItem(8, createItem(svc.getGuiIcon("creative.customize", Material.PAINTING), "§9Customize Menu", "§7Edit icons"));
        }

        inv.setItem(26, createItem(Material.ARROW, "§cBack", null));
        p.openInventory(inv);
    }

    public void openWorldManager(Player p, String worldName) {
        Inventory inv = createMenu(54, "Manage: " + worldName);
        AltWorldsService svc = plugin.getService();
        String internal = p.getWorld().getName();

        boolean isAllWorld = svc.isAllWorld(internal);
        boolean isLobby = svc.isLobbyWorld(internal);
        UUID ownerUuid = (isAllWorld || isLobby) ? AltWorldsService.SERVER_UUID : svc.getOwnerUuidByInternalWorldName(internal);
        if (ownerUuid == null) ownerUuid = p.getUniqueId();

        boolean isAdmin = p.hasPermission("altworlds.admin");
        boolean isOwner = ownerUuid.equals(p.getUniqueId());

        // --- FILA 1: ACCIONES PRINCIPALES (0-5) ---

        // 0. Set Home
        String currentHome = svc.getPlayerSettingStr(p.getUniqueId(), "homeWorld");
        boolean isHome = currentHome != null && currentHome.equals(worldName);
        Material setHomeIcon = svc.getGuiIcon("manage.set_home", Material.RED_BED);
        inv.setItem(0, createItem(setHomeIcon, "§6Set as Home", isHome ? "§aCurrently set as Home" : "§7Click to set as your\n§7login world."));

        // 1. Change Icon
        Material currentIcon = svc.getWorldIcon(ownerUuid, worldName);
        Material changeIconMat = svc.getGuiIcon("manage.change_icon", Material.ITEM_FRAME);
        inv.setItem(1, createItem(changeIconMat, "§bChange Icon", "§7Current: " + currentIcon.name() + "\n§eClick to select new icon"));

        if (!isAllWorld && !isLobby) {
            // 2. Rename World
            boolean canRename = plugin.getConfig().getBoolean("restrictions.allowRenaming", true) || isAdmin;

            if (canRename) {
                Material renameMat = svc.getGuiIcon("manage.rename", Material.NAME_TAG);
                inv.setItem(2, createItem(renameMat, "§eRename World", "§7Click to rename via chat"));
            } else {
                inv.setItem(2, createItem(Material.BARRIER, "§cRename Locked", "§7Renaming disabled."));
            }

            // 3. Delete World
            Material deleteMat = svc.getGuiIcon("manage.delete", Material.TNT);
            inv.setItem(3, createItem(deleteMat, "§4§lDELETE WORLD", "§cWarning! Irreversible.\n§7Shift-Click to Delete\n§e(Requires chat confirmation)"));

            // 4. Regenerate World
            boolean isTemplate = svc.isTemplateWorld(ownerUuid, worldName);
            if (!isTemplate || isAdmin) {
                Material regenMat = svc.getGuiIcon("manage.regenerate", Material.LIME_DYE);
                inv.setItem(4, createItem(regenMat, "§a§lRegenerate World", "§7Resets world.\n§cKeeps settings.\n§7Shift-Click to Confirm via Chat."));
            } else {
                inv.setItem(4, createItem(Material.GRAY_DYE, "§7Regenerate (Locked)", "§cTemplate locked."));
            }

            // 5. Manage Members
            Material membersMat = svc.getGuiIcon("manage.members", Material.PLAYER_HEAD);
            inv.setItem(5, createItem(membersMat, "§bManage Members", "§7Invite, Kick or change Game Mode\n§7players in this world."));
        } else {
            // Placeholder para mundos de servidor (Acciones bloqueadas)
            inv.setItem(3, createItem(Material.GRAY_DYE, "§7Delete World", "§cServer/Lobby worlds cannot be deleted via GUI."));
            inv.setItem(4, createItem(Material.GRAY_DYE, "§7Regenerate (Locked)", "§cCannot regen server worlds/lobby."));
        }

        if (isAdmin && !isLobby) {
            Material cloneMat = svc.getGuiIcon("manage.clone", Material.CARTOGRAPHY_TABLE);
            inv.setItem(6, createItem(cloneMat, "§d§lClone World",
                    "§7Create a full copy\n§7with same settings & members.\n§eShift-Click to Clone"));
        }

        // --- FILA 2 en adelante: SETTINGS ---
        // Empiezan en slot 18 (Fila 3) para dejar espacio visual.
        int slot = 18;

        boolean isCreativeWorld = svc.isCreativeWorld(ownerUuid, worldName);
        boolean canEditFullSettings = isAdmin || (isOwner && isCreativeWorld);

        for (WorldSettings setting : WorldSettings.values()) {
            if (slot > 53) break;

            boolean isSpecial = (setting == WorldSettings.VISITORS || setting == WorldSettings.PUBLIC);
            boolean canEdit = canEditFullSettings || isSpecial;

            // Icono desde config o default
            Material iconMat = svc.getGuiIcon("settings." + setting.getKey(), setting.getIcon());

            List<String> lore = new ArrayList<>();
            lore.add("§7" + setting.getDescription());
            lore.add("");

            if (setting == WorldSettings.DIFFICULTY) {
                String diff = svc.getSettingString(ownerUuid, worldName, setting.getKey(), "NORMAL");
                lore.add("§7Value: §b" + diff.toUpperCase());
                if (!canEdit) {
                    lore.add("§c(Locked for this world type)");
                } else {
                    lore.add("§eClick to Cycle");
                }
            } else {
                boolean currentState = svc.getSetting(ownerUuid, worldName, setting.getKey(), false);
                String statusColor = currentState ? "§aON" : "§cOFF";
                lore.add("§7Status: " + statusColor);

                if (!canEdit) {
                    lore.add("§c(Locked for this world type)");
                    // Mantenemos el icono visual pero indicamos bloqueo en lore
                } else {
                    lore.add("§eClick to Toggle");
                }
            }

            inv.setItem(slot, createItem(iconMat, "§6" + setting.getDisplayName(), String.join("\n", lore)));
            slot++;
        }

        // --- BOTÃ“N SET BORDER (Justo a continuaciÃ³n de los settings) ---
        Material borderIcon = svc.getGuiIcon("settings.border", Material.IRON_BARS);

        double currentBorder = p.getWorld().getWorldBorder().getSize() / 2;
        if (currentBorder > 1000000) currentBorder = 0;

        inv.setItem(slot, createItem(borderIcon, "§9Set World Border", "§7Radius: §e" + (currentBorder == 0 ? "Infinite" : currentBorder) + "\n\n§eClick to set radius via chat\n§7Type '0' to disable."));

        // --- ADMIN: CUSTOMIZE ICONS (Arriba Derecha - Slot 8) ---
        if (isAdmin) {
            inv.setItem(8, createItem(Material.PAINTING, "§9Customize Icons", "§7Edit default icons for settings"));
        }

        inv.setItem(53, createItem(Material.ARROW, "§cBack", null));
        p.openInventory(inv);
    }

    public void openSettingsIconEditor(Player p) {
        Inventory inv = createMenu(54, "Edit: World Settings");
        AltWorldsService svc = plugin.getService();

        inv.setItem(0, createItem(svc.getGuiIcon("manage.set_home", Material.RED_BED), "§6Set as Home", "§eClick to change icon"));
        inv.setItem(1, createItem(svc.getGuiIcon("manage.change_icon", Material.ITEM_FRAME), "§bChange Icon", "§eClick to change icon"));
        inv.setItem(2, createItem(svc.getGuiIcon("manage.rename", Material.NAME_TAG), "§eRename World", "§eClick to change icon"));
        inv.setItem(3, createItem(svc.getGuiIcon("manage.delete", Material.TNT), "§cDelete World", "§eClick to change icon"));
        inv.setItem(4, createItem(svc.getGuiIcon("manage.regenerate", Material.LIME_DYE), "§aRegenerate World", "§eClick to change icon"));
        inv.setItem(5, createItem(svc.getGuiIcon("manage.members", Material.PLAYER_HEAD), "§bManage Members", "§eClick to change icon"));
        inv.setItem(6, createItem(svc.getGuiIcon("manage.clone", Material.CARTOGRAPHY_TABLE), "§dClone World", "§eClick to change icon"));
        inv.setItem(8, createItem(Material.BARRIER, "§cExit Editor", "§7Finish editing icons"));

        int slot = 18;
        for (WorldSettings setting : WorldSettings.values()) {
            Material mat = svc.getGuiIcon("settings." + setting.getKey(), setting.getIcon());
            inv.setItem(slot++, createItem(mat, "§6" + setting.getDisplayName(), "§eClick to change icon"));
        }

        // Icono del borde
        Material borderMat = svc.getGuiIcon("settings.border", Material.IRON_BARS);
        inv.setItem(slot, createItem(borderMat, "§9Set World Border", "§eClick to change icon"));

        p.openInventory(inv);
    }

    public void openMembersManager(Player p, String worldName, UUID ownerUuid) {
        Inventory inv = createMenu(54, "Members: " + worldName);
        AltWorldsService svc = plugin.getService();

        inv.setItem(4, createItem(Material.EMERALD, "§a§lInvite Player", "§7Click to select an\n§7online player to invite."));
        inv.setItem(6, createItem(Material.BARRIER, "§c§lBanned Players", "§7Click to view & unban\n§7banned players."));

        Set<UUID> members = svc.getWorldMembers(ownerUuid, worldName);

        // Lista ordenada: Owner primero
        List<UUID> sortedMembers = new ArrayList<>();
        sortedMembers.add(ownerUuid);
        for (UUID uuid : members) {
            if (!uuid.equals(ownerUuid)) sortedMembers.add(uuid);
        }

        int slot = 9;
        for (UUID memUuid : sortedMembers) {
            if (slot >= 53) break;

            // Obtener datos
            String name = Bukkit.getOfflinePlayer(memUuid).getName();
            Role role = (memUuid.equals(ownerUuid)) ? Role.OWNER : svc.getPlayerRole(ownerUuid, worldName, memUuid);

            // Obtener Gamemode actual (especÃ­fico o default del mundo)
            org.bukkit.GameMode gm = svc.getSpecificPlayerGamemode(ownerUuid, worldName, memUuid);
            String gmName = (gm != null) ? gm.name() : "DEFAULT";

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = getSkullMeta(head);
            if (meta == null) continue;
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(memUuid));
            meta.displayName(c("§e" + (name == null ? "Unknown" : name)));

            List<String> lore = new ArrayList<>();
            lore.add("§7Role: §f" + role.name());
            lore.add("§7Gamemode: §b" + gmName);
            lore.add("");
            lore.add("§eLeft-Click: §bCycle Gamemode");

            if (!memUuid.equals(ownerUuid)) {
                // El owner no puede ser kickeado ni cambiado de rol aquÃ­ (solo GM)
                lore.add("§eRight-Click: §cKick Player");
                lore.add("§eShift-Right: §6Temp Ban");
                lore.add("§eShift-Left: §4Perm Ban");
            } else {
                lore.add("§7(Owner cannot be kicked)");
            }

            meta.lore(toComponents(lore));
            head.setItemMeta(meta);
            inv.setItem(slot++, head);
        }
        inv.setItem(53, createItem(Material.ARROW, "§cBack", null));
        p.openInventory(inv);
    }

    private void openBannedList(Player p, String worldName, UUID ownerUuid) {
        Inventory inv = createMenu(54, "Banned: " + worldName);
        YamlConfiguration config = plugin.getService().getPlayerConfig(ownerUuid);
        String base = "worlds." + worldName + ".banned";
        ConfigurationSection banned = config.getConfigurationSection(base);
        boolean changed = false;

        int slot = 0;
        if (banned != null) {
            for (String key : banned.getKeys(false)) {
                if (slot >= 53) break;
                UUID target = parseUuid(key);
                if (target == null) continue;

                long unbanTime = config.getLong(base + "." + key, -1);
                if (unbanTime != -1 && System.currentTimeMillis() > unbanTime) {
                    config.set(base + "." + key, null);
                    changed = true;
                    continue;
                }

                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = getSkullMeta(head);
                if (meta == null) continue;
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(target));
                String name = Bukkit.getOfflinePlayer(target).getName();
                meta.displayName(c("§c" + (name == null ? "Unknown" : name)));
                List<String> lore = new ArrayList<>();
                if (unbanTime == -1) lore.add("§4Permanent Ban");
                else lore.add("§eTime Left: §f" + formatDurationSeconds((unbanTime - System.currentTimeMillis()) / 1000));
                lore.add("§eClick to Unban");
                lore.add("§8ID:" + target);
                meta.lore(toComponents(lore));
                head.setItemMeta(meta);
                inv.setItem(slot++, head);
            }
        }

        if (changed) plugin.getService().savePlayerConfig(ownerUuid);
        inv.setItem(53, createItem(Material.ARROW, "§cBack", null));
        p.openInventory(inv);
    }

    private void openTempBanMenu(Player p, BanRequest req) {
        Inventory inv = createMenu(27, "Temp Ban: " + req.targetName);
        pendingBanTempSelect.put(p.getUniqueId(), req);

        List<String> presets = plugin.getConfig().getStringList("bans.temp-presets");
        if (presets.isEmpty()) {
            presets = Arrays.asList("5m", "30m", "1h", "6h", "1d", "7d");
        }
        int[] slots = new int[] {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 23, 24, 25};
        int idx = 0;
        for (String preset : presets) {
            if (idx >= slots.length) break;
            String label = preset.trim().toLowerCase();
            if (label.isEmpty()) continue;
            long seconds = parseDurationSeconds(label);
            if (seconds == Long.MIN_VALUE || seconds == -1) {
                plugin.getLogger().warning("Invalid temp ban preset: " + preset);
                continue;
            }
            inv.setItem(slots[idx++], createItem(Material.CLOCK, "§e" + label, "§7Temp ban for " + formatDurationSeconds(seconds)));
        }

        inv.setItem(22, createItem(Material.PAPER, "§bCustom", "§7Type a time in chat\n§730s, 10m, 2h, 1d"));
        p.openInventory(inv);
    }

    private void openBanConfirmMenu(Player p, BanRequest req) {
        Inventory inv = createMenu(27, "Confirm Ban: " + req.targetName);
        pendingBanConfirm.put(p.getUniqueId(), req);

        String durationLabel = (req.durationSeconds == -1) ? "Permanent" : formatDurationSeconds(req.durationSeconds);
        inv.setItem(13, createItem(Material.BARRIER, "§cBan " + req.targetName, "§7Duration: §f" + durationLabel));
        inv.setItem(11, createItem(Material.LIME_WOOL, "§aConfirm", "§7Click to confirm"));
        inv.setItem(15, createItem(Material.RED_WOOL, "§cCancel", "§7Back"));

        p.openInventory(inv);
    }

    public void openOnlinePlayersSelector(Player p, String worldName) {
        Inventory inv = createMenu(54, "Invite to: " + worldName);
        AltWorldsService svc = plugin.getService();
        UUID ownerUuid = svc.getOwnerUuidByInternalWorldName(p.getWorld().getName());
        int slot = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (slot >= 53) break;
            if (online.getUniqueId().equals(p.getUniqueId())) continue;
            if (ownerUuid != null && (svc.isMember(ownerUuid, worldName, online.getUniqueId()) || online.getUniqueId().equals(ownerUuid))) continue;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = getSkullMeta(head);
            if (meta == null) continue;
            meta.setOwningPlayer(online);
            meta.displayName(c("§e" + online.getName()));
            meta.lore(toComponents(Collections.singletonList("§aClick to Invite")));
            head.setItemMeta(meta);
            inv.setItem(slot++, head);
        }
        inv.setItem(53, createItem(Material.ARROW, "§cBack", null));
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        String title = plain(e.getView().title());
        // Filtro de inventarios del plugin
        if (!title.startsWith("AltWorlds:") &&
                !title.startsWith("Explore:") &&
                !title.startsWith("Create:") &&
                !title.startsWith("Manage:") &&
                !title.startsWith("Icon:") &&
                !title.startsWith("Admin:") &&
                !title.startsWith("Worlds:") &&
                !title.startsWith("Inspect:") &&
                !title.startsWith("Members:") &&
                !title.startsWith("Banned:") &&
                !title.startsWith("Temp Ban:") &&
                !title.startsWith("Confirm Ban:") &&
                !title.startsWith("Invite to:") &&
                !title.startsWith("Edit:")) {
            return;
        }

        e.setCancelled(true);
        ItemStack currentItem = e.getCurrentItem();
        if (currentItem == null) return;
        ItemMeta currentMeta = currentItem.getItemMeta();
        if (currentMeta == null) return;

        Player p = (Player) e.getWhoClicked();
        String name = plainDisplayName(currentMeta);
        // --- SISTEMA DE BACK BUTTON ---
        if (name.contains("Back")) {
            if (title.startsWith("Icon:")) {
                String raw = title.replace("Icon: ", "");
                String context = raw.contains(" #") ? raw.split(" #")[0] : raw;
                if (context.startsWith("gui.admin")) openAdminGuiEditor(p);
                else if (context.startsWith("gui.")) openGuiEditor(p);
                else if (context.startsWith("tpl.")) openTemplateIconEditor(p);
                else openWorldManager(p, context); // Context es el nombre del mundo
            }
            else if (title.startsWith("Create: Creative") ||
                    title.startsWith("Create: Select Template") ||
                    title.startsWith("Create: Select Private")) {
                openCategorySelector(p);
            }
            else if (title.startsWith("Create:")) openMain(p);
            else if (title.equals("Admin: Player List")) openAdminSettings(p);
            else if (title.equals("Admin: Settings")) openMain(p);
            else if (title.startsWith("Inspect:")) openPlayerList(p);
            // CORREGIDO: Logica robusta para volver de "Worlds: Jugador"
            else if (title.startsWith("Worlds:")) {
                UUID target = null;

                // 1. Intentar leer ID del Lore (Limpiando colores primero)
                if (currentMeta.hasLore()) {
                    List<String> lore = plainLore(currentMeta);
                    for (String line : lore) {
                        if (line.startsWith("ID:")) {
                            String uuidStr = line.replace("ID:", "").trim();
                            UUID parsed = parseUuid(uuidStr);
                            if (parsed != null) target = parsed;
                            break;
                        }
                    }
                }
                // 2. Plan B: Si falla el Lore, intentar por nombre del titulo
                if (target == null) {
                    String playerName = title.replace("Worlds: ", "").trim();
                    target = plugin.getService().findPlayerUuidByName(playerName);
                }
                // 3. Ejecutar accion
                if (target != null) {
                    openPlayerInspector(p, target);
                } else {
                    // Si todo falla, volvemos a la lista general para no quedarnos atrapados
                    openPlayerList(p);
                }
            }

            // CORREGIDO: Back para Global Icons (Antes Close)
            else if (title.equals("Admin: Global Icons")) {
                openMain(p);
            }

            else if (title.startsWith("Members:")) {
                String wName = title.replace("Members: ", "");
                openWorldManager(p, wName);
            }
            else if (title.startsWith("Banned:")) {
                String wName = title.replace("Banned: ", "");
                UUID owner = plugin.getService().getOwnerUuidByInternalWorldName(p.getWorld().getName());
                if (owner == null) {
                    owner = plugin.getService().findWorldOwnerIfMember(wName, p.getUniqueId());
                    if (owner == null) owner = p.getUniqueId();
                }
                openMembersManager(p, wName, owner);
            }
            else if (title.startsWith("Temp Ban:")) {
                BanRequest req = pendingBanTempSelect.get(p.getUniqueId());
                if (req != null) openMembersManager(p, req.worldName, req.ownerUuid);
                else openMain(p);
            }
            else if (title.startsWith("Confirm Ban:")) {
                BanRequest req = pendingBanConfirm.get(p.getUniqueId());
                if (req != null) openMembersManager(p, req.worldName, req.ownerUuid);
                else openMain(p);
            }
            else if (title.startsWith("Invite to:")) {
                String wName = title.replace("Invite to: ", "");
                UUID owner = plugin.getService().getOwnerUuidByInternalWorldName(p.getWorld().getName());
                if(owner != null) openMembersManager(p, wName, owner);
            }
            else if (title.contains("Admin: All Worlds")) openAdminSettings(p);
            else if (title.contains("AltWorlds: Server Worlds")) openMain(p);
            else if (title.startsWith("Edit:")) {
                // Salidas de editores
                if (title.contains("Explore")) openExplorer(p);
                else if (title.contains("Creative")) openCreativeOptions(p);
                else if (title.contains("Create")) openCategorySelector(p);
                else if (title.contains("Admin")) openAdminSettings(p);
                else if (title.contains("Settings")) {
                    String wName = plugin.getService().getWorldNameByInternal(p.getWorld().getName());
                    if(wName != null) openWorldManager(p, wName); else openMain(p);
                }
                else if (title.contains("Templates")) openTemplateSelector(p);
                else openMain(p);
            }
            else openMain(p);
            return;
        }

        // --- MENÃš PRINCIPAL ---
        //noinspection IfCanBeSwitch
        if (title.equals("AltWorlds: Main Menu")) {
            switch (name) {
                case "Create World" -> openCategorySelector(p);
                case "My Worlds" -> openMyWorlds(p);
                case "Explore Worlds" -> { explorerSort.put(p.getUniqueId(), "players"); openExplorer(p); }
                case "Server Worlds" -> openServerWorlds(p);
                case "Lobby" -> plugin.getService().teleportToLobby(p);
                case "Admin Settings" -> openAdminSettings(p);
                case "Customize Menu" -> openGuiEditor(p);
                case "Current World Settings" -> {
                    String internal = p.getWorld().getName();
                    String wName = plugin.getService().getWorldNameByInternal(internal);
                    if (wName != null) openWorldManager(p, wName);
                }
                case "Auto-Join Home" -> {
                    boolean current = plugin.getService().getPlayerSetting(p.getUniqueId(), "skipLobby", false);
                    plugin.getService().setPlayerSetting(p.getUniqueId(), "skipLobby", !current);
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                    openMain(p);
                }
                case "Lobby Item" -> openIconSelector(p, "gui.lobby_item", 0);
                case "Global World Icons" -> openGlobalSettingsPreview(p);
                default -> { }
            }
            return;
        }
        // --- NUEVO: GESTION DE GLOBAL ICONS ---
        if (title.equals("Admin: Global Icons")) {
            if (name.contains("Customize Menu")) {
                openGlobalIconsEditor(p);
                return;
            }
            return;
        }

        if (title.equals("Edit: Global Icons")) {
            if (name.contains("Exit")) { openGlobalSettingsPreview(p); return; }
            String key = switch (name) {
                case "Set as Home" -> "manage.set_home";
                case "Change Icon" -> "manage.change_icon";
                case "Rename World" -> "manage.rename";
                case "Delete World" -> "manage.delete";
                case "Regenerate World" -> "manage.regenerate";
                case "Manage Members" -> "manage.members";
                case "Clone World" -> "manage.clone";
                case "Set World Border" -> "settings.border";
                default -> null;
            };
            if (key == null) {
                for (WorldSettings s : WorldSettings.values()) {
                    if (name.contains(s.getDisplayName())) {
                        key = "settings." + s.getKey();
                        break;
                    }
                }
            }

            if (key != null) openIconSelector(p, "gui." + key, 0);
            return;
        }

        // --- MY WORLDS ---
        if (title.equals("AltWorlds: My Worlds")) {
            if (e.getCurrentItem().getType() == Material.AIR) return;
            String wName = name.trim();
            List<String> lore = plainLore(currentMeta);
            UUID targetOwner = null;
            if (!lore.isEmpty()) {
                for (String line : lore) {
                    if (line.startsWith("ID:")) {
                        try {
                            targetOwner = UUID.fromString(line.replace("ID:", "").trim());
                        } catch (Exception ex) {
                            plugin.getLogger().warning("Invalid owner UUID in lore: " + line);
                        }
                        break;
                    }
                }
            }
            if (targetOwner != null) plugin.getService().teleportToWorld(p, targetOwner, wName);
            else {
                if (plugin.getService().hasWorld(p.getUniqueId(), wName)) plugin.getService().teleportToOwnedWorld(p, wName);
                else p.sendMessage("§cWorld not found.");
            }
            return;
        }

        // --- GESTIÃ“N DE EDITORES (Customize Menu) ---
        if (title.equals("Edit: Main Menu")) {
            if (name.contains("Exit Editor")) { openMain(p); return; }
            String key = resolveMainMenuEditKey(name);
            if (key != null) openIconSelector(p, "gui." + key, 0);
            return;
        }

        if (title.equals("Edit: Explore Menu")) {
            if (name.contains("Exit")) { openExplorer(p); return; }
            String key = switch (name) {
                case "Sort: Players" -> "explore.sort_players";
                case "Sort: Likes" -> "explore.sort_likes";
                case "Sort: Visits" -> "explore.sort_visits";
                case "Sort: Type" -> "explore.sort_type";
                case "Customize Menu" -> "explore.customize";
                default -> null;
            };
            if (key != null) openIconSelector(p, "gui." + key, 0);
            return;
        }

        if (title.equals("Edit: Create Menu")) {
            if (name.contains("Exit")) { openCategorySelector(p); return; }
            String key = switch (name) {
                case "Vanilla" -> "create.vanilla";
                case "Templates" -> "create.templates";
                case "Creative" -> "create.creative";
                case "Admin" -> "create.admin";
                case "Customize Menu" -> "create.customize";
                default -> null;
            };
            if (key != null) openIconSelector(p, "gui." + key, 0);
            return;
        }

        if (title.equals("Edit: Creative Menu")) {
            if (name.contains("Exit")) { openCreativeOptions(p); return; }
            String key = switch (name) {
                case "Vanilla" -> "creative.vanilla";
                case "Flat" -> "creative.flat";
                case "Void" -> "creative.void";
                case "Customize Menu" -> "creative.customize";
                default -> null;
            };
            if (key != null) openIconSelector(p, "gui." + key, 0);
            return;
        }

        if (title.equals("Edit: Admin Menu")) {
            if (name.contains("Exit")) { openAdminSettings(p); return; }
            String key = switch (name) {
                case "View All Worlds" -> "admin.view_all";
                case "Global Limit" -> "admin.global_limit";
                case "Manage Players" -> "admin.manage_players";
                case "Customize This Menu" -> "admin.customize";
                default -> null;
            };
            if (key != null) openIconSelector(p, "gui." + key, 0);
            return;
        }

        if (title.equals("Edit: World Settings")) {
            if (name.contains("Exit")) {
                String wName = plugin.getService().getWorldNameByInternal(p.getWorld().getName());
                if(wName != null) openWorldManager(p, wName);
                else openMain(p);
                return;
            }
            String key = switch (name) {
                case "Set as Home" -> "manage.set_home";
                case "Change Icon" -> "manage.change_icon";
                case "Rename World" -> "manage.rename";
                case "Delete World" -> "manage.delete";
                case "Regenerate World" -> "manage.regenerate";
                case "Manage Members" -> "manage.members";
                case "Clone World" -> "manage.clone";
                case "World Border" -> "settings.border";
                default -> null;
            };
            if (key == null) {
                for (WorldSettings s : WorldSettings.values()) {
                    if (name.contains(s.getDisplayName())) {
                        key = "settings." + s.getKey();
                        break;
                    }
                }
            }
            if (key != null) openIconSelector(p, "gui." + key, 0);
            return;
        }

        if (title.equals("Edit: Templates")) {
            if (name.contains("Back")) { openTemplateSelector(p); return; }
            if (e.getCurrentItem().getType() != Material.AIR) {
                String folderKey = null;
                TemplatesConfig tc = plugin.getService().getTemplates();
                for(String f : tc.getTemplateFolders()) {
                    if (stripLegacy(tc.getDisplayName(f)).equals(name)) { folderKey = f; break; }
                }
                if (folderKey != null) {
                    openIconSelector(p, "tpl." + folderKey, 0);
                }
            }
            return;
        }

        // --- SUBMENÃšS Y ACCIONES ---

        if (title.contains("Server Worlds")) {
            if (!name.contains("Back")) {
                plugin.getService().teleportToAllWorld(p, name);
            }
        }

        if (title.equals("Admin: Settings")) {
            if (name.contains("View All Worlds")) openAdminAllWorlds(p);
            else if (name.contains("Global Limit")) {
                p.closeInventory();
                pendingGlobalLimitInput.add(p.getUniqueId());
                p.sendMessage("§a[AltWorlds] §eEnter global limit using: §6/aw input <number>");
            }
            else if (name.contains("Manage Players")) openPlayerList(p);
            else if (name.contains("Customize This Menu")) openAdminGuiEditor(p);
            return;
        }

        if (title.equals("Admin: Player List")) {
            if (e.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                SkullMeta meta = getSkullMeta(e.getCurrentItem());
                if (meta == null) return;
                if (meta.getOwningPlayer() != null) {
                    openPlayerInspector(p, meta.getOwningPlayer().getUniqueId());
                }
            }
            return;
        }

        if (title.startsWith("Inspect:")) {
            if (e.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                SkullMeta meta = getSkullMeta(e.getCurrentItem());
                if (meta == null) return;
                if (meta.getOwningPlayer() != null) {
                    UUID targetUuid = meta.getOwningPlayer().getUniqueId();
                    String targetName = plainDisplayName(meta); // Nombre visual

                    p.closeInventory();
                    pendingPlayerLimitSet.put(p.getUniqueId(), targetUuid);

                    // MENSAJE CLARO Y ESPECÃFICO
                    p.sendMessage("§a[AltWorlds] §eSet max worlds for " + targetName);
                    p.sendMessage("§eCurrent Limit: Write the number.");
                    p.sendMessage("§6Command: /aw input <number>");
                    p.sendMessage("§7(Example: /aw input 5  |  Use -1 for Unlimited)");
                }
            }
            else if (name.contains("View Player Worlds")) {
                ItemStack headItem = e.getInventory().getItem(11);
                if (headItem != null && headItem.hasItemMeta()) {
                    ItemMeta rawMeta = headItem.getItemMeta();
                    if (rawMeta instanceof SkullMeta headMeta && headMeta.getOwningPlayer() != null) {
                        openPlayerWorldsList(p, headMeta.getOwningPlayer().getUniqueId());
                    }
                }
            }
        }

        if (title.startsWith("Worlds:")) {
            if (e.getCurrentItem().getType() != Material.AIR && !name.contains("Back")) {
                String playerName = title.replace("Worlds: ", "");
                UUID target = plugin.getService().findPlayerUuidByName(playerName);
                if (target != null) {
                    if (e.isShiftClick()) openMembersManager(p, name, target);
                    else plugin.getService().teleportToWorld(p, target, name);
                }
            }
        }

        if (title.startsWith("Members:")) {
            String wName = title.replace("Members: ", "");
            UUID ownerUuid = plugin.getService().getOwnerUuidByInternalWorldName(p.getWorld().getName());
            if (ownerUuid == null) {
                ownerUuid = plugin.getService().findWorldOwnerIfMember(wName, p.getUniqueId());
                if(ownerUuid == null) ownerUuid = p.getUniqueId();
            }

            if (name.contains("Invite Player")) {
                openOnlinePlayersSelector(p, wName);
                return;
            }

            if (name.contains("Banned Players")) {
                openBannedList(p, wName, ownerUuid);
                return;
            }

            if (e.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                SkullMeta meta = getSkullMeta(e.getCurrentItem());
                if (meta == null) return;
                if (meta.getOwningPlayer() != null) {
                    UUID target = meta.getOwningPlayer().getUniqueId();
                    if (e.isRightClick()) {
                        if (e.isShiftClick()) {
                            if (!target.equals(ownerUuid)) {
                                BanRequest req = new BanRequest();
                                req.ownerUuid = ownerUuid;
                                req.worldName = wName;
                                req.targetUuid = target;
                                req.targetName = meta.getOwningPlayer().getName();
                                openTempBanMenu(p, req);
                            } else p.sendMessage("§cCannot ban the owner.");
                        } else {
                            if (!target.equals(ownerUuid)) {
                                plugin.getService().kickPlayer(p, meta.getOwningPlayer().getName());
                                openMembersManager(p, wName, ownerUuid);
                            } else p.sendMessage("§cCannot kick the owner.");
                        }
                    }
                    else if (e.isLeftClick()) {
                        if (e.isShiftClick()) {
                            if (!target.equals(ownerUuid)) {
                                BanRequest req = new BanRequest();
                                req.ownerUuid = ownerUuid;
                                req.worldName = wName;
                                req.targetUuid = target;
                                req.targetName = meta.getOwningPlayer().getName();
                                req.durationSeconds = -1;
                                openBanConfirmMenu(p, req);
                            } else p.sendMessage("§cCannot ban the owner.");
                        } else {

                        org.bukkit.GameMode current = plugin.getService().getSpecificPlayerGamemode(ownerUuid, wName, target);
                        org.bukkit.GameMode next = (current == null) ? org.bukkit.GameMode.SURVIVAL : switch (current) {
                            case SURVIVAL -> org.bukkit.GameMode.CREATIVE;
                            case CREATIVE -> org.bukkit.GameMode.ADVENTURE;
                            case ADVENTURE -> org.bukkit.GameMode.SPECTATOR;
                            default -> null;
                        };

                        plugin.getService().setSpecificPlayerGamode(ownerUuid, wName, target, next);
                        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                        p.sendMessage("§aGamemode updated for " + meta.getOwningPlayer().getName());

                        Player tPlayer = Bukkit.getPlayer(target);
                        if (tPlayer != null && tPlayer.getWorld().getName().equals(p.getWorld().getName())) {
                            tPlayer.setGameMode(next != null ? next : org.bukkit.GameMode.SURVIVAL);
                        }
                        openMembersManager(p, wName, ownerUuid);
                    }
                }
            }
            return;
        }
        }


        if (title.startsWith("Banned:")) {
            String wName = title.replace("Banned: ", "");
            UUID ownerUuid = plugin.getService().getOwnerUuidByInternalWorldName(p.getWorld().getName());
            if (ownerUuid == null) {
                ownerUuid = plugin.getService().findWorldOwnerIfMember(wName, p.getUniqueId());
                if (ownerUuid == null) ownerUuid = p.getUniqueId();
            }

            if (e.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                SkullMeta meta = getSkullMeta(e.getCurrentItem());
                if (meta == null) return;
                UUID target = null;
                if (meta.getOwningPlayer() != null) target = meta.getOwningPlayer().getUniqueId();
                if (target == null && currentMeta.hasLore()) {
                    List<String> lore = plainLore(currentMeta);
                    if (!lore.isEmpty()) {
                        for (String line : lore) {
                            if (line.startsWith("ID:")) {
                                UUID parsed = parseUuid(line.replace("ID:", "").trim());
                                if (parsed != null) target = parsed;
                                break;
                            }
                        }
                    }
                }
                if (target != null) {
                    plugin.getService().unbanPlayer(ownerUuid, wName, target);
                    openBannedList(p, wName, ownerUuid);
                }
            }
            return;
        }

        if (title.startsWith("Temp Ban:")) {
            BanRequest req = pendingBanTempSelect.get(p.getUniqueId());
            if (req == null) return;

            if (name.contains("Back")) {
                pendingBanTempSelect.remove(p.getUniqueId());
                openMembersManager(p, req.worldName, req.ownerUuid);
                return;
            }

            if (name.contains("Custom")) {
                pendingBanInput.put(p.getUniqueId(), req);
                p.closeInventory();
                p.sendMessage("§eEnter ban time using: §6/aw input <time>");
                p.sendMessage("§7Examples: 30s, 10m, 2h, 1d, perm");
                return;
            }

            long seconds = parseDurationSeconds(name.trim());
            if (seconds == Long.MIN_VALUE) {
                p.sendMessage("§cInvalid time.");
                return;
            }
            req.durationSeconds = seconds;
            openBanConfirmMenu(p, req);
            return;
        }

        if (title.startsWith("Confirm Ban:")) {
            BanRequest req = pendingBanConfirm.remove(p.getUniqueId());
            if (req == null) return;
            pendingBanTempSelect.remove(p.getUniqueId());

            if (name.contains("Confirm")) {
                plugin.getService().banPlayer(p, req.ownerUuid, req.worldName, req.targetUuid, req.durationSeconds);
                openMembersManager(p, req.worldName, req.ownerUuid);
            } else if (name.contains("Cancel") || name.contains("Back")) {
                openMembersManager(p, req.worldName, req.ownerUuid);
            }
            return;
        }

        if (title.startsWith("Invite to:")) {
            if (e.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                SkullMeta meta = getSkullMeta(e.getCurrentItem());
                if (meta == null) return;
                if (meta.getOwningPlayer() != null) {
                    plugin.getService().sendInvite(p, meta.getOwningPlayer().getName());
                    p.closeInventory();
                }
            }
        }

        if (title.startsWith("Explore:")) {
            if (name.contains("Sort: Players")) { explorerSort.put(p.getUniqueId(), "players"); openExplorer(p); }
            else if (name.contains("Sort: Likes")) { explorerSort.put(p.getUniqueId(), "likes"); openExplorer(p); }
            else if (name.contains("Sort: Visits")) { explorerSort.put(p.getUniqueId(), "visits"); openExplorer(p); }
            else if (name.contains("Sort: Type")) { explorerSort.put(p.getUniqueId(), "type"); openExplorer(p); }
            else if (name.contains("Customize Menu")) { openExploreEditor(p); return; }
            else if (e.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                if (e.isShiftClick()) {
                    List<String> lore = plainLore(currentMeta);
                    if (!lore.isEmpty()) {
                        String ownerName = lore.getFirst().replace("Owner: ", "");
                        String wName = plainDisplayName(currentMeta);
                        UUID ownerUuid = plugin.getService().findPlayerUuidByName(ownerName);
                        if (ownerUuid != null) {
                            plugin.getService().toggleLike(p, ownerUuid, wName);
                            openExplorer(p);
                        }
                    }
                } else {
                    List<String> lore = plainLore(currentMeta);
                    if (!lore.isEmpty()) {
                        String ownerName = lore.getFirst().replace("Owner: ", "");
                        String wName = plainDisplayName(currentMeta);
                        UUID ownerUuid = plugin.getService().findPlayerUuidByName(ownerName);
                        if (ownerUuid != null) plugin.getService().teleportToWorld(p, ownerUuid, wName);
                    }
                }
            }
        }

        if (title.contains("Select Category")) {
            if (name.contains("Customize Menu")) { openCreateEditor(p); return; }
            if (e.isShiftClick() && p.hasPermission("altworlds.admin")) {
                if (name.contains("Vanilla")) toggleConfig(p, "restrictions.creation.allowSurvival", () -> openCategorySelector(p));
                else if (name.contains("Templates")) toggleConfig(p, "restrictions.creation.allowTemplates", () -> openCategorySelector(p));
                else if (name.contains("Creative")) toggleConfig(p, "restrictions.creation.allowCreative", () -> openCategorySelector(p));
                return;
            }
            if (name.contains("Vanilla")) prepareWizard(p, "vanilla", "NORMAL", GameMode.SURVIVAL);
            else if (name.contains("Templates")) openTemplateSelector(p);
            else if (name.contains("Creative")) openCreativeOptions(p);
            else if (name.contains("Admin / Private")) openPrivateTemplateSelector(p);
        }

        if (title.contains("Creative Options")) {
            if (name.contains("Customize Menu")) { openCreativeEditor(p); return; }
            if (e.isShiftClick() && p.hasPermission("altworlds.admin")) {
                if (name.contains("Vanilla")) toggleConfig(p, "restrictions.creation.creative.vanilla", () -> openCreativeOptions(p));
                else if (name.contains("Flat")) toggleConfig(p, "restrictions.creation.creative.flat", () -> openCreativeOptions(p));
                else if (name.contains("Void")) toggleConfig(p, "restrictions.creation.creative.void", () -> openCreativeOptions(p));
                return;
            }
            if (name.contains("Vanilla")) prepareWizard(p, "creative", "NORMAL", GameMode.CREATIVE);
            else if (name.contains("Flat")) prepareWizard(p, "creative", "FLAT", GameMode.CREATIVE);
            else if (name.contains("Void")) prepareWizard(p, "creative", "VOID", GameMode.CREATIVE);
        }

        if (title.contains("Select Template")) {
            if (name.contains("Edit Template Icons")) { openTemplateIconEditor(p); return; }
            String folderName = name;
            TemplatesConfig tc = plugin.getService().getTemplates();
            for(String f : tc.getTemplateFolders()) {
                if (stripLegacy(tc.getDisplayName(f)).equals(name)) { folderName = f; break; }
            }
            prepareWizard(p, "template:" + folderName, "NORMAL", GameMode.SURVIVAL);
        }

        if (title.contains("Select Private")) {
            prepareWizard(p, "privatetemplate:" + name, "NORMAL", GameMode.SURVIVAL);
        }

        if (title.startsWith("Manage:")) {
            String wName = title.replace("Manage: ", "");

            if (name.contains("Customize Icons")) { openSettingsIconEditor(p); return; }

            if (name.contains("Rename World")) {
                if (name.contains("Locked")) return;
                p.closeInventory();
                pendingRename.put(p.getUniqueId(), wName);
                p.sendMessage("§a[AltWorlds] Type new name in chat using /aw input <name>");
                return;
            }
            if (name.contains("Set World Border")) {
                p.closeInventory();
                pendingBorderInput.put(p.getUniqueId(), wName);
                p.sendMessage("§a[AltWorlds] §eTo set the border, type:");
                p.sendMessage("§6/aw input <radius>");
                p.sendMessage("§7(Example: /aw input 100)");
                return;
            }
            if (name.contains("Set as Home")) {
                plugin.getService().setPlayerSetting(p.getUniqueId(), "homeWorld", wName);
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                p.sendMessage("§a[AltWorlds] World '" + wName + "' set as your new Home.");
                openWorldManager(p, wName);
                return;
            }
            if (name.contains("Change Icon")) { openIconSelector(p, wName, 0); return; }
            if (name.contains("Manage Members")) {
                UUID ownerUuid = plugin.getService().getOwnerUuidByInternalWorldName(p.getWorld().getName());
                if(ownerUuid != null) openMembersManager(p, wName, ownerUuid);
                return;
            }
            if (name.contains("Clone World")) {
                if (!p.hasPermission("altworlds.admin")) return;
                if (!e.isShiftClick()) {
                    p.sendMessage("§eShift-Click to clone this world.");
                    return;
                }
                UUID ownerUuid = plugin.getService().getOwnerUuidByInternalWorldName(p.getWorld().getName());
                if (plugin.getService().isAllWorld(p.getWorld().getName()) || plugin.getService().isLobbyWorld(p.getWorld().getName())) {
                    ownerUuid = AltWorldsService.SERVER_UUID;
                }
                if (ownerUuid == null) ownerUuid = p.getUniqueId();
                p.closeInventory();
                pendingCloneInput.put(p.getUniqueId(), new CloneRequest(ownerUuid, wName));
                p.sendMessage("§a[AltWorlds] Type the new world name using /aw input <name>");
                return;
            }

            if (name.contains("DELETE") && e.isShiftClick()) {
                if (name.contains("Locked")) return;
                p.closeInventory();
                requestConfirmation(p, "DELETE " + wName, () -> plugin.getService().deleteWorld(p, wName));
                return;
            }

            if (name.contains("Regenerate") && e.isShiftClick()) {
                if (name.contains("Locked")) return;
                p.closeInventory();
                requestConfirmation(p, "REGENERATE " + wName, () -> plugin.getService().regenerateWorld(p, wName));
                return;
            }

            for (WorldSettings s : WorldSettings.values()) {
                if (s.getDisplayName().equals(name)) {
                    UUID owner = plugin.getService().getOwnerUuidByInternalWorldName(p.getWorld().getName());
                    if (owner == null) owner = p.getUniqueId(); // Fallback

                    if (s == WorldSettings.DIFFICULTY) {
                        String current = plugin.getService().getSettingString(owner, wName, s.getKey(), "NORMAL");
                        String[] order = new String[] { "PEACEFUL", "NORMAL", "HARD" };
                        String next = "NORMAL";
                        for (int i = 0; i < order.length; i++) {
                            if (order[i].equalsIgnoreCase(current)) {
                                next = order[(i + 1) % order.length];
                                break;
                            }
                        }
                        plugin.getService().setWorldSettingString(owner, wName, s.getKey(), next);
                    } else {
                        boolean current = plugin.getService().getSetting(owner, wName, s.getKey(), false);
                        plugin.getService().setWorldSetting(owner, wName, s.getKey(), !current);
                    }
                    openWorldManager(p, wName);
                    break;
                }
            }
        }

        // --- SELECTOR DE ICONOS ---
        if (title.startsWith("Icon:")) {
            String raw = title.replace("Icon: ", "");
            String[] parts = raw.split(" #");
            String context = parts[0];
            int page = (parts.length > 1) ? Integer.parseInt(parts[1]) : 0;

            if (name.equals("Previous Page")) { openIconSelector(p, context, page - 1); return; }
            if (name.equals("Next Page")) { openIconSelector(p, context, page + 1); return; }

            if (e.getCurrentItem().getType() != Material.AIR && !name.contains("Page")) {
                if (context.startsWith("gui.")) {
                    String key = context.replace("gui.", "");
                    Material mat = e.getCurrentItem().getType();
                    plugin.getService().setGuiIcon(key, mat);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2);

                    if (key.startsWith("admin.")) openAdminGuiEditor(p);
                    else if (key.startsWith("explore.")) openExploreEditor(p);
                    else if (key.startsWith("create.")) openCreateEditor(p);
                    else if (key.startsWith("creative.")) openCreativeEditor(p);
                    else if (key.startsWith("settings.")) openGlobalIconsEditor(p); // CORREGIDO AQUÍ
                    else if (key.startsWith("manage.")) openGlobalIconsEditor(p);
                    else openGuiEditor(p);
                }
                else if (context.startsWith("tpl.")) {
                    String key = context.replace("tpl.", "");
                    Material mat = e.getCurrentItem().getType();
                    plugin.getService().setTemplateIcon(key, mat);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2);
                    openTemplateIconEditor(p);
                }
                else {
                    // Icono de mundo
                    String internal = p.getWorld().getName();
                    boolean isAllWorld = plugin.getService().isAllWorld(internal);
                    boolean isLobby = plugin.getService().isLobbyWorld(internal);
                    UUID ownerUuid = (isAllWorld || isLobby) ? AltWorldsService.SERVER_UUID : plugin.getService().getOwnerUuidByInternalWorldName(internal);
                    if(ownerUuid == null) ownerUuid = p.getUniqueId();

                    plugin.getService().setWorldIcon(ownerUuid, context, e.getCurrentItem().getType());
                    openWorldManager(p, context);
                }
            }
        }

        if (title.contains("Admin: All Worlds")) {
            // LÃ³gica de teletransporte para Admin All Worlds
            if (!name.contains("Sort") && !name.contains("Back")) {
                // El nombre viene como "Owner: WorldName" en colores
                String[] parts = name.split(": ");
                if(parts.length == 2) {
                    UUID target = plugin.getService().findPlayerUuidByName(parts[0]);
                    String wName = parts[1];
                    if(target != null) plugin.getService().teleportToWorld(p, target, wName);
                }
            }
        }
    }

    private void requestConfirmation(Player p, String actionName, Runnable action) {
        p.sendMessage("§c[AltWorlds] §e==================================");
        p.sendMessage("§c[AltWorlds] §e ACTION: " + actionName);
        p.sendMessage("§c[AltWorlds] §e Type '/aw confirm' to execute.");
        p.sendMessage("§c[AltWorlds] §e Type anything else to cancel.");
        p.sendMessage("§c[AltWorlds] §e==================================");
        pendingConfirm.put(p.getUniqueId(), action);
    }

    private void toggleConfig(Player p, String path, Runnable reopenMenu) {
        boolean current = plugin.getConfig().getBoolean(path, true);
        plugin.getConfig().set(path, !current);
        plugin.saveConfig();
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2);
        reopenMenu.run();
    }

    private void requestName(Player p, WizardData data) {
        p.closeInventory();
        creationData.put(p.getUniqueId(), data);
        pendingNameInput.add(p.getUniqueId());
        sendCreateWorldInputMessage(p);
    }

    private void prepareWizard(Player p, String type, String gen, GameMode mode) {
        WizardData data = new WizardData();
        data.type = type; data.generator = gen; data.mode = mode;
        requestName(p, data);
    }
    private void sendCreateWorldInputMessage(Player p) {
        p.sendMessage("§a[AltWorlds] §e==================================");
        p.sendMessage("§a[AltWorlds] §e ACTION: CREATE WORLD");
        p.sendMessage("§a[AltWorlds] §e Use: §6/aw name <WorldName>");
        p.sendMessage("§a[AltWorlds] §e Cancel: §c/aw cancel");
        p.sendMessage("§a[AltWorlds] §e==================================");
    }

    public void handleNameInput(Player p, String name) {
        if (!pendingNameInput.contains(p.getUniqueId())) {
            p.sendMessage("§cYou are not currently creating a world.");
            return;
        }

        pendingNameInput.remove(p.getUniqueId());
        WizardData data = creationData.remove(p.getUniqueId());

        if (data != null) {
            Map<String, String> f = new HashMap<>();
            f.put("gm", data.mode.name());
            f.put("generator", data.generator);
            plugin.getService().createWorld(p, name, data.type, f);
        } else {
            p.sendMessage("§cSession expired.");
        }
    }

    public void handleCancel(Player p) {
        if (pendingNameInput.contains(p.getUniqueId())) {
            pendingNameInput.remove(p.getUniqueId());
            creationData.remove(p.getUniqueId());
            p.sendMessage("§cWorld creation cancelled.");
        } else {
            p.sendMessage("§cNothing to cancel.");
        }
    }



    public void openMyWorlds(Player p) {
        Inventory inv = createMenu(54, "AltWorlds: My Worlds");
        UUID uuid = p.getUniqueId();
        AltWorldsService svc = plugin.getService();

        // 1. Mundos Propios
        for(String wName : svc.listPlayerWorlds(uuid)) {
            boolean exists = svc.worldFolderExists(uuid, wName);
            Material icon = exists ? svc.getWorldIcon(uuid, wName) : Material.BARRIER;
            inv.addItem(createItem(icon, "§b" + wName, exists ? "§7Click to teleport" : "§cMissing"));
        }

        // 2. Mundos Compartidos (Formato de lista: "OwnerUUID:WorldName")
        List<String> joined = svc.listJoinedWorlds(uuid);
        if(joined != null && !joined.isEmpty()) {
            for(String entry : joined) {
                String[] parts = entry.split(":");
                if(parts.length < 2) continue;

                try {
                    UUID ownerUuid = UUID.fromString(parts[0]);
                    String wName = parts[1];
                    String ownerName = Bukkit.getOfflinePlayer(ownerUuid).getName();
                    if(ownerName == null) ownerName = "Unknown";

                    Material icon = svc.getWorldIcon(ownerUuid, wName);
                    // IMPORTANTE: En el Lore guardamos la info para el click
                    inv.addItem(createItem(icon, "§b" + wName, "§7(Shared World)\n§7Owner: " + ownerName + "\n§8ID:" + ownerUuid));
                } catch(Exception e) {
                    // Ignorar entradas corruptas
                }
            }
        }

        inv.setItem(53, createItem(Material.ARROW, "§cBack", null));
        p.openInventory(inv);
    }

    private void openTemplateSelector(Player p) {
        Inventory inv = createMenu(54, "Create: Select Template");
        TemplatesConfig tc = plugin.getService().getTemplates();

        // BotÃ³n para editar iconos de templates (Solo admin)
        if (p.hasPermission("altworlds.admin")) {
            inv.setItem(8, createItem(Material.PAINTING, "§9Edit Template Icons", "§7Click to switch mode.\n§7Then click a template to change its icon."));
        }

        for (String folder : tc.getTemplateFolders()) {
            inv.addItem(createItem(tc.getIcon(folder), tc.getDisplayName(folder), "§7Click to use template"));
        }
        inv.setItem(53, createItem(Material.ARROW, "§cBack", null));
        p.openInventory(inv);
    }

    private void openPrivateTemplateSelector(Player p) {
        Inventory inv = createMenu(54, "Create: Select Private");
        List<String> privates = plugin.getService().getPrivateTemplates();
        for (String folder : privates) {
            inv.addItem(createItem(Material.COMMAND_BLOCK, "§c" + folder, "§7Click to use private template"));
        }
        inv.setItem(53, createItem(Material.ARROW, "§cBack", null));
        p.openInventory(inv);
    }

    private void openIconSelector(Player p, String context, int page) {
        Inventory inv = createMenu(54, "Icon: " + context + " #" + page);

        int itemsPerPage = 45;
        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, ICON_MATERIALS.size());

        for (int i = start; i < end; i++) {
            inv.addItem(createItem(ICON_MATERIALS.get(i), "§eSelect", null));
        }

        if (page > 0) {
            inv.setItem(45, createItem(Material.ARROW, "§ePrevious Page", null));
        }

        inv.setItem(49, createItem(Material.BARRIER, "§cBack", null));

        if (end < ICON_MATERIALS.size()) {
            inv.setItem(53, createItem(Material.ARROW, "§eNext Page", null));
        }

        p.openInventory(inv);
    }

    public void handleCommandConfirm(Player p) {
        if (pendingConfirm.containsKey(p.getUniqueId())) {
            Runnable action = pendingConfirm.remove(p.getUniqueId());
            p.sendMessage("§a[AltWorlds] Action confirmed.");
            Bukkit.getScheduler().runTask(plugin, action);
        } else {
            p.sendMessage("§cNo pending confirmation.");
        }
    }

    public void handleCommandInput(Player p, String input) {
        UUID uuid = p.getUniqueId();

        if (pendingNameInput.contains(uuid)) {
            handleNameInput(p, input); // Este mÃ©todo ya existÃ­a para /aw name
        }
        else if (pendingRename.containsKey(uuid)) {
            String oldName = pendingRename.remove(uuid);
            plugin.getService().renameWorld(p, oldName, input);
        }
        else if (pendingBorderInput.containsKey(uuid)) {
            String wName = pendingBorderInput.remove(uuid);
            try {
                double radius = Double.parseDouble(input);
                if (radius <= 0) plugin.getService().removeWorldBorder(p);
                else {
                    // Pasamos la ubicaciÃ³n del jugador como centro
                    plugin.getService().setWorldBorder(p, radius, p.getLocation());
                }
                openWorldManager(p, wName);
            } catch (NumberFormatException ex) { p.sendMessage("§cInvalid number."); }
        }
        else if (pendingCloneInput.containsKey(uuid)) {
            CloneRequest req = pendingCloneInput.remove(uuid);
            plugin.getService().cloneWorld(p, req.ownerUuid, req.worldName, input);
        }
        else if (pendingGlobalLimitInput.contains(uuid)) {
            pendingGlobalLimitInput.remove(uuid);
            try {
                int limit = Integer.parseInt(input);
                plugin.getConfig().set("defaults.maxWorldsPerPlayer", limit);
                plugin.saveConfig();
                p.sendMessage("§aGlobal limit updated to: " + limit);
                openAdminSettings(p);
            } catch (NumberFormatException ex) { p.sendMessage("§cInvalid number."); }
        }
        else if (pendingBanInput.containsKey(uuid)) {
            BanRequest req = pendingBanInput.remove(uuid);
            long seconds = parseDurationSeconds(input);
            if (seconds == Long.MIN_VALUE) {
                pendingBanInput.put(uuid, req);
                p.sendMessage("§cInvalid time. Use 30s, 10m, 2h, 1d or perm.");
            } else {
                req.durationSeconds = seconds;
                openBanConfirmMenu(p, req);
            }
        }
        else if (pendingPlayerLimitSet.containsKey(uuid)) {
            UUID target = pendingPlayerLimitSet.remove(uuid);
            try {
                int limit = Integer.parseInt(input);
                plugin.getService().setMaxWorldsForPlayer(target, limit);
                p.sendMessage("§aLimit updated to: " + limit);
                openPlayerInspector(p, target);
            } catch (NumberFormatException ex) { p.sendMessage("§cInvalid number."); }
        } else {
            p.sendMessage("§cNo input expected.");
        }
    }

    public void openExploreEditor(Player p) {
        Inventory inv = createMenu(54, "Edit: Explore Menu");
        AltWorldsService svc = plugin.getService();

        inv.setItem(0, createItem(svc.getGuiIcon("explore.sort_players", Material.PLAYER_HEAD), "§eSort: Players", "§eClick to change"));
        inv.setItem(1, createItem(svc.getGuiIcon("explore.sort_likes", Material.EMERALD), "§aSort: Likes", "§eClick to change"));
        inv.setItem(2, createItem(svc.getGuiIcon("explore.sort_visits", Material.OAK_SIGN), "§bSort: Visits", "§eClick to change"));
        inv.setItem(3, createItem(svc.getGuiIcon("explore.sort_type", Material.PAPER), "§dSort: Type", "§eClick to change"));

        inv.setItem(8, createItem(Material.BARRIER, "§cExit Editor", "§7Finish editing icons"));

        p.openInventory(inv);
    }


    // Editor especÃ­fico para los iconos globales de settings
    public void openGlobalIconsEditor(Player p) {
        Inventory inv = createMenu(54, "Edit: Global Icons");
        AltWorldsService svc = plugin.getService();

        inv.setItem(0, createItem(svc.getGuiIcon("manage.set_home", Material.RED_BED), "§6Set as Home", "§eClick to change icon"));
        inv.setItem(1, createItem(svc.getGuiIcon("manage.change_icon", Material.ITEM_FRAME), "§bChange Icon", "§eClick to change icon"));
        inv.setItem(2, createItem(svc.getGuiIcon("manage.rename", Material.NAME_TAG), "§eRename World", "§eClick to change icon"));
        inv.setItem(3, createItem(svc.getGuiIcon("manage.delete", Material.TNT), "§cDelete World", "§eClick to change icon"));
        inv.setItem(4, createItem(svc.getGuiIcon("manage.regenerate", Material.LIME_DYE), "§aRegenerate World", "§eClick to change icon"));
        inv.setItem(5, createItem(svc.getGuiIcon("manage.members", Material.PLAYER_HEAD), "§bManage Members", "§eClick to change icon"));
        inv.setItem(6, createItem(svc.getGuiIcon("manage.clone", Material.CARTOGRAPHY_TABLE), "§dClone World", "§eClick to change icon"));
        inv.setItem(8, createItem(Material.BARRIER, "§cExit Editor", "§7Finish editing icons"));

        int slot = 18;
        for (WorldSettings setting : WorldSettings.values()) {
            Material mat = svc.getGuiIcon("settings." + setting.getKey(), setting.getIcon());
            inv.setItem(slot++, createItem(mat, "§6" + setting.getDisplayName(), "§eClick to change icon"));
        }

        Material borderMat = svc.getGuiIcon("settings.border", Material.IRON_BARS);
        inv.setItem(slot, createItem(borderMat, "§9Set World Border", "§eClick to change icon"));
        p.openInventory(inv);
    }

    public void openCreateEditor(Player p) {
        Inventory inv = createMenu(27, "Edit: Create Menu");
        AltWorldsService svc = plugin.getService();
        inv.setItem(11, createItem(svc.getGuiIcon("create.vanilla", Material.OAK_SAPLING), "§aVanilla", "§eClick to change"));
        inv.setItem(13, createItem(svc.getGuiIcon("create.templates", Material.PAPER), "§dTemplates", "§eClick to change"));
        inv.setItem(15, createItem(svc.getGuiIcon("create.creative", Material.DIAMOND_BLOCK), "§bCreative", "§eClick to change"));
        inv.setItem(22, createItem(svc.getGuiIcon("create.admin", Material.COMMAND_BLOCK), "§cAdmin", "§eClick to change"));
        inv.setItem(8, createItem(Material.BARRIER, "§cExit Editor", "§7Finish editing icons"));
        p.openInventory(inv);
    }

    public void openCreativeEditor(Player p) {
        Inventory inv = createMenu(27, "Edit: Creative Menu");
        AltWorldsService svc = plugin.getService();
        inv.setItem(11, createItem(svc.getGuiIcon("creative.vanilla", Material.GRASS_BLOCK), "§bVanilla", "§eClick to change"));
        inv.setItem(13, createItem(svc.getGuiIcon("creative.flat", Material.SANDSTONE), "§bFlat", "§eClick to change"));
        inv.setItem(15, createItem(svc.getGuiIcon("creative.void", Material.GLASS), "§bVoid", "§eClick to change"));
        inv.setItem(8, createItem(Material.BARRIER, "§cExit Editor", "§7Finish editing icons"));
        p.openInventory(inv);
    }

    public void openTemplateIconEditor(Player p) {
        Inventory inv = createMenu(54, "Edit: Templates");
        TemplatesConfig tc = plugin.getService().getTemplates();
        for (String folder : tc.getTemplateFolders()) {
            inv.addItem(createItem(tc.getIcon(folder), tc.getDisplayName(folder), "§eClick to change icon"));
        }
        inv.setItem(53, createItem(Material.ARROW, "§cBack", null));
        p.openInventory(inv);
    }

    // MenÃº visual para que el admin cambie los iconos globales desde el lobby
    public void openGlobalSettingsPreview(Player p) {
        Inventory inv = createMenu(54, "Admin: Global Icons");
        AltWorldsService svc = plugin.getService();

        inv.setItem(0, createItem(svc.getGuiIcon("manage.set_home", Material.RED_BED), "§6Set as Home", "§7(Icon Preview)"));
        inv.setItem(1, createItem(svc.getGuiIcon("manage.change_icon", Material.ITEM_FRAME), "§bChange Icon", "§7(Icon Preview)"));
        inv.setItem(2, createItem(svc.getGuiIcon("manage.rename", Material.NAME_TAG), "§eRename World", "§7(Icon Preview)"));
        inv.setItem(3, createItem(svc.getGuiIcon("manage.delete", Material.TNT), "§cDelete World", "§7(Icon Preview)"));
        inv.setItem(4, createItem(svc.getGuiIcon("manage.regenerate", Material.LIME_DYE), "§aRegenerate World", "§7(Icon Preview)"));
        inv.setItem(5, createItem(svc.getGuiIcon("manage.members", Material.PLAYER_HEAD), "§bManage Members", "§7(Icon Preview)"));
        inv.setItem(6, createItem(svc.getGuiIcon("manage.clone", Material.CARTOGRAPHY_TABLE), "§dClone World", "§7(Icon Preview)"));

        int slot = 18;
        // Recorremos todos los settings para mostrar su icono actual
        for (WorldSettings setting : WorldSettings.values()) {
            Material mat = svc.getGuiIcon("settings." + setting.getKey(), setting.getIcon());
            inv.setItem(slot++, createItem(mat, "§6" + setting.getDisplayName(), "§7(Icon Preview)\n§7Value cannot be changed here."));
        }

        // Icono del borde
        Material borderMat = svc.getGuiIcon("settings.border", Material.IRON_BARS);
        inv.setItem(slot, createItem(borderMat, "§9Set World Border", "§7(Icon Preview)"));

        // BotÃ³n Customize (Arriba Derecha - Slot 8)
        inv.setItem(8, createItem(Material.PAINTING, "§9Customize Menu", "§7Click to edit these icons"));
        inv.setItem(53, createItem(Material.ARROW, "§cBack", null));
        p.openInventory(inv);
    }

    private long parseDurationSeconds(String input) {
        if (input == null) return Long.MIN_VALUE;
        String s = input.trim().toLowerCase();
        if (s.isEmpty()) return Long.MIN_VALUE;
        if (s.equals("perm") || s.equals("permanent") || s.equals("forever") || s.equals("-1") || s.equals("0")) return -1;

        try {
            if (s.matches("\\d+")) {
                long minutes = Long.parseLong(s);
                return minutes * 60L;
            }
            if (s.matches("\\d+[smhd]")) {
                long value = Long.parseLong(s.substring(0, s.length() - 1));
                char unit = s.charAt(s.length() - 1);
                return switch (unit) {
                    case 's' -> value;
                    case 'm' -> value * 60L;
                    case 'h' -> value * 3600L;
                    case 'd' -> value * 86400L;
                    default -> Long.MIN_VALUE;
                };
            }
        } catch (NumberFormatException ex) {
            return Long.MIN_VALUE;
        }
        return Long.MIN_VALUE;
    }

    private String formatDurationSeconds(long seconds) {
        if (seconds < 0) return "Permanent";
        long s = seconds;
        long days = s / 86400; s %= 86400;
        long hours = s / 3600; s %= 3600;
        long mins = s / 60; s %= 60;

        StringBuilder out = new StringBuilder();
        if (days > 0) out.append(days).append("d ");
        if (hours > 0) out.append(hours).append("h ");
        if (mins > 0) out.append(mins).append("m ");
        if (s > 0 || out.isEmpty()) out.append(s).append("s");
        return out.toString().trim();
    }

    private String resolveMainMenuEditKey(String name) {
        return switch (name) {
            case "My Worlds" -> "main.my_worlds";
            case "Explore Worlds" -> "main.explore";
            case "Create World" -> "main.create";
            case "Lobby Item" -> "lobby_item";
            case "Lobby" -> "main.lobby";
            case "Server Worlds" -> "main.server_worlds";
            case "Admin Settings" -> "main.admin";
            case "Customize Menu" -> "main.customize";
            case "Auto-Join Home" -> "main.auto_join";
            case "Current World" -> "main.current_world";
            case "Global World Icons" -> "main.global_settings";
            default -> null;
        };
    }

    private static Component c(String legacy) {
        return legacy == null ? Component.empty() : LEGACY.deserialize(legacy);
    }

    private static String plain(Component component) {
        return component == null ? "" : PLAIN.serialize(component);
    }

    private static String stripLegacy(String legacy) {
        return legacy == null ? "" : plain(LEGACY.deserialize(legacy));
    }

    private static String plainDisplayName(ItemMeta meta) {
        if (meta == null) return "";
        return plain(meta.displayName());
    }

    private static List<String> plainLore(ItemMeta meta) {
        if (meta == null) return Collections.emptyList();
        List<Component> lore = meta.lore();
        if (lore == null) return Collections.emptyList();
        return lore.stream().map(PLAIN::serialize).toList();
    }

    private static List<Component> toComponents(List<String> lines) {
        if (lines == null || lines.isEmpty()) return Collections.emptyList();
        return lines.stream().map(s -> (Component) LEGACY.deserialize(s)).toList();
    }

    private static List<Component> toComponents(String lore) {
        if (lore == null || lore.isEmpty()) return Collections.emptyList();
        return Arrays.stream(lore.split("\n")).map(s -> (Component) LEGACY.deserialize(s)).toList();
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private SkullMeta getSkullMeta(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return (meta instanceof SkullMeta skull) ? skull : null;
    }

    @SuppressWarnings("deprecation")
    private static Inventory createMenu(int size, String title) {
        return Bukkit.createInventory(null, size, c(title));
    }

    private String capitalize(String str) { return str.substring(0, 1).toUpperCase() + str.substring(1); }
    private ItemStack createItem(Material mat, String name, String lore) {
        ItemStack i = new ItemStack(mat != null ? mat : Material.STONE);
        ItemMeta m = i.getItemMeta();
        if (m == null) return i;
        if (name != null) m.displayName(c(name));
        if (lore != null && !lore.isEmpty()) m.lore(toComponents(lore));
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        i.setItemMeta(m);
        return i;
    }
}





















