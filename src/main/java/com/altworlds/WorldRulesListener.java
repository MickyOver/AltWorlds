package com.altworlds;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

public class WorldRulesListener implements Listener {
    private final AltWorldsService service;

    public WorldRulesListener(AltWorldsService service) {
        this.service = service;
    }

    // MÃ©todo auxiliar para leer la configuraciÃ³n real del plugin (La autoridad absoluta)
    private boolean check(Player p, String settingKey, boolean def) {
        String wName = p.getWorld().getName();
        return checkWorld(wName, settingKey, def);
    }

    // Sobrecarga para cuando no tenemos Player (ej: Mobs)
    private boolean checkWorld(String wName, String settingKey, boolean def) {
        // 1. Verificar si es mundo de jugador
        UUID owner = service.getOwnerUuidByInternalWorldName(wName);
        if (owner != null) {
            String realName = service.getWorldNameByInternal(wName);
            if (realName != null) {
                return service.getSetting(owner, realName, settingKey, def);
            }
        }

        // 2. Verificar si es Lobby o Mundo de Servidor
        if (service.isAllWorld(wName) || service.isLobbyWorld(wName)) {
            String realName = service.getWorldNameByInternal(wName);
            if (service.isLobbyWorld(wName)) realName = wName; // Nombre simple para lobby

            return service.getSetting(AltWorldsService.SERVER_UUID, realName, settingKey, def);
        }

        return def;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();

        // Seguridad de Spawn
        if (service.getOwnerUuidByInternalWorldName(p.getWorld().getName()) != null) {
            if (!e.isBedSpawn()) {
                Location spawn = p.getWorld().getSpawnLocation();
                spawn.setX(spawn.getBlockX() + 0.5);
                spawn.setZ(spawn.getBlockZ() + 0.5);

                int y = p.getWorld().getHighestBlockYAt(spawn.getBlockX(), spawn.getBlockZ());
                if (spawn.getY() < y) spawn.setY(y + 1);

                e.setRespawnLocation(spawn);
            }
        }

        // Actualizar inventario (reloj) con delay
        Bukkit.getScheduler().runTaskLater(service.getPlugin(), () -> {
            service.updatePlayerInventory(p);
        }, 5L);
    }

    // --- REGLAS DE JUEGO FORZADAS ---

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player && e.getDamager() instanceof Player) {
            if (!check((Player)e.getEntity(), WorldSettings.PVP.getKey(), false)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player) {
            if (!check((Player)e.getEntity(), WorldSettings.HUNGER.getKey(), true)) {
                e.setCancelled(true);
                ((Player)e.getEntity()).setFoodLevel(20);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && e.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (!check((Player)e.getEntity(), WorldSettings.FALL_DAMAGE.getKey(), true)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotionThrow(ProjectileLaunchEvent e) {
        if (e.getEntity().getShooter() instanceof Player) {
            Player p = (Player) e.getEntity().getShooter();
            if (e.getEntity().getType().name().contains("POTION")) {
                if (!check(p, WorldSettings.POTIONS.getKey(), true)) {
                    e.setCancelled(true);
                    p.sendMessage("§cPotions are disabled here.");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFlyToggle(PlayerToggleFlightEvent e) {
        Player p = e.getPlayer();
        if (p.getGameMode().name().contains("CREATIVE") || p.getGameMode().name().contains("SPECTATOR")) return;

        if (!check(p, WorldSettings.FLY.getKey(), false)) {
            if (!p.hasPermission("altworlds.admin")) {
                e.setCancelled(true);
                p.setAllowFlight(false);
            }
        }
    }

    @EventHandler
    public void onWorldChangeCheckFly(PlayerChangedWorldEvent e) {
        Bukkit.getScheduler().runTaskLater(service.getPlugin(), () -> {
            Player p = e.getPlayer();
            if (p.getGameMode().name().contains("CREATIVE") || p.getGameMode().name().contains("SPECTATOR")) return;

            if (check(p, WorldSettings.FLY.getKey(), false)) {
                p.setAllowFlight(true);
            } else {
                p.setAllowFlight(false);
                p.setFlying(false);
            }
        }, 2L);
    }

    // --- NUEVO: FORZAR KEEP INVENTORY ---
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent e) {
        Player p = e.getEntity();
        // Leemos nuestro setting, default true (conservador)
        boolean keep = check(p, WorldSettings.KEEP_INVENTORY.getKey(), true);

        if (keep) {
            e.setKeepInventory(true);
            e.setKeepLevel(true);
            e.setDroppedExp(0);
            e.getDrops().clear();
        }
        // Si keep es false, dejamos que Vanilla maneje el drop (que lo harÃ¡ por defecto)
    }

    // --- NUEVO: FORZAR BLOQUEO DE MOBS ---
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobSpawn(CreatureSpawnEvent e) {
        // Ignoramos spawns hechos por plugins (CUSTOM) o comandos, solo nos interesa natural/spawner
        CreatureSpawnEvent.SpawnReason r = e.getSpawnReason();
        if (r == CreatureSpawnEvent.SpawnReason.CUSTOM || r == CreatureSpawnEvent.SpawnReason.COMMAND) return;

        // Si el setting doMobSpawning es FALSE, cancelamos
        if (!checkWorld(e.getLocation().getWorld().getName(), WorldSettings.MOB_SPAWNING.getKey(), true)) {
            e.setCancelled(true);
        }
    }

    // --- BLOCK DROPS (Ya corregido) ---
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        if (!check(e.getPlayer(), WorldSettings.BLOCK_FALL.getKey(), true)) {
            e.setDropItems(false);
            e.setExpToDrop(0);
        }
    }
}