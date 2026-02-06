package com.altworlds;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class WorldProtectionListener implements Listener {

    private final AltWorldsService service;
    private final WorldAccess access;

    public WorldProtectionListener(AltWorldsService service) {
        this.service = service;
        this.access = new WorldAccess(service);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (!access.isAltWorld(p.getWorld())) return;
        if (!access.canBuild(p, p.getWorld())) {
            e.setCancelled(true);
            p.sendMessage("§cYou cannot break blocks here.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (!access.isAltWorld(p.getWorld())) return;
        if (!access.canBuild(p, p.getWorld())) {
            e.setCancelled(true);
            p.sendMessage("§cYou cannot place blocks here.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!access.isAltWorld(p.getWorld())) return;

        // Allow Clock usage
        Material clockMat = service.getLobbyItemMaterial();
        if (e.getItem() != null && e.getItem().getType() == clockMat) {
            return;
        }

        if (!access.canInteract(p, p.getWorld())) {
            e.setCancelled(true);
            // Don't spam message on pressure plates
            if (e.getAction().name().contains("RIGHT")) {
                p.sendMessage("§cYou cannot interact here.");
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (!access.isAltWorld(p.getWorld())) return;
        if (!access.canOpenContainers(p, p.getWorld())) {
            e.setCancelled(true);
            p.sendMessage("§cYou cannot open containers here.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        if (!access.isAltWorld(p.getWorld())) return;
        if (!access.canBuild(p, p.getWorld())) {
            e.setCancelled(true);
        }
    }
}