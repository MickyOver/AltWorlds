package com.altworlds;

import org.bukkit.GameRule;
import org.bukkit.Material;

public enum WorldSettings {
    PVP("pvp", "PvP Combat", Material.IRON_SWORD, "Allow players to fight", Category.CUSTOM, null),
    FLY("fly", "Fly Mode", Material.FEATHER, "Allow visitors to fly", Category.CUSTOM, null),
    HUNGER("hunger", "Hunger Loss", Material.ROTTEN_FLESH, "Players lose hunger", Category.CUSTOM, null),
    FALL_DAMAGE("fallDamage", "Fall Damage", Material.LEATHER_BOOTS, "Take damage when falling", Category.CUSTOM, null),
    POTIONS("potions", "Potion Usage", Material.BREWING_STAND, "Allow throwing potions", Category.CUSTOM, null),
    // WORLD_CHAT ELIMINADO
    VISITORS("allowVisitors", "Allow Visitors", Material.OAK_DOOR, "Let others join via /aw visit", Category.CUSTOM, null),
    PUBLIC("public", "Public Listing", Material.GLOBE_BANNER_PATTERN, "Show in Explorer Menu", Category.CUSTOM, null),

    DAY_CYCLE("doDaylightCycle", "Day/Night Cycle", Material.CLOCK, "Sun moves", Category.GAMERULE, GameRule.DO_DAYLIGHT_CYCLE),
    WEATHER("doWeatherCycle", "Weather Cycle", Material.WATER_BUCKET, "Rain/Storms happen", Category.GAMERULE, GameRule.DO_WEATHER_CYCLE),
    FIRE_SPREAD("doFireTick", "Fire Spread", Material.FLINT_AND_STEEL, "Fire spreads to blocks", Category.GAMERULE, GameRule.DO_FIRE_TICK),
    MOB_GRIEFING("mobGriefing", "Mob Griefing", Material.CREEPER_HEAD, "Creepers break blocks", Category.GAMERULE, GameRule.MOB_GRIEFING),
    KEEP_INVENTORY("keepInventory", "Keep Inventory", Material.CHEST, "Save items on death", Category.GAMERULE, GameRule.KEEP_INVENTORY),
    MOB_SPAWNING("doMobSpawning", "Mob Spawning", Material.ZOMBIE_HEAD, "Monsters spawn naturally", Category.GAMERULE, GameRule.DO_MOB_SPAWNING),
    BLOCK_FALL("doTileDrops", "Block Drops", Material.GRAVEL, "Blocks drop items when broken", Category.GAMERULE, GameRule.DO_TILE_DROPS),
    CROP_GROWTH("randomTickSpeed", "Crop Growth", Material.WHEAT, "Plants grow (Tick Speed)", Category.GAMERULE, null);

    public enum Category { CUSTOM, GAMERULE }

    private final String key;
    private final String displayName;
    private final Material icon;
    private final String description;
    private final Category category;
    private final GameRule<Boolean> gameRule;

    WorldSettings(String key, String displayName, Material icon, String description, Category category, GameRule<Boolean> gameRule) {
        this.key = key;
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
        this.category = category;
        this.gameRule = gameRule;
    }

    public String getKey() { return key; }
    public String getDisplayName() { return displayName; }
    public Material getIcon() { return icon; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public GameRule<Boolean> getGameRule() { return gameRule; }
}