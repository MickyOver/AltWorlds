package com.altworlds;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public class AltWorldsCommand implements CommandExecutor, TabCompleter {
    private final AltWorldsPlugin plugin;
    private final AltWorldsService service;

    public AltWorldsCommand(AltWorldsPlugin plugin, AltWorldsService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        // Comando por defecto: Abrir menÃº
        if (args.length == 0 || args[0].equalsIgnoreCase("menu")) {
            plugin.getGui().openMain(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        // --- SISTEMA DE CREACIÃ“N DE MUNDOS (INPUT) ---
        if (sub.equals("name")) {
            if (args.length >= 2) {
                String name = args[1];
                plugin.getGui().handleNameInput(player, name);
            } else {
                player.sendMessage("§cUsage: /aw name <WorldName>");
            }
            return true;
        }
        else if (sub.equals("cancel")) {
            plugin.getGui().handleCancel(player);
            return true;
        }
        // -------------------------------------

        if (sub.equals("lobby")) {
            service.teleportToLobby(player);
        }
        else if (sub.equals("tp")) {
            if (args.length == 2) {
                String targetWorld = args[1];

                // 1. Probar si es mundo propio
                if (service.hasWorld(player.getUniqueId(), targetWorld)) {
                    service.teleportToOwnedWorld(player, targetWorld);
                } else {
                    // 2. Probar si es mundo del servidor
                    boolean isServerWorld = service.getAllWorldsList().contains(targetWorld);
                    if (isServerWorld) {
                        service.teleportToAllWorld(player, targetWorld);
                    } else {
                        // 3. Probar si es un mundo compartido (Donde soy miembro)
                        // NOTA: Esto requiere buscar en los archivos, el service se encarga.
                        UUID ownerUuid = service.findWorldOwnerIfMember(targetWorld, player.getUniqueId());

                        if (ownerUuid != null) {
                            service.teleportToWorld(player, ownerUuid, targetWorld);
                        } else {
                            player.sendMessage("§cWorld not found or you don't have access.");
                        }
                    }
                }
            }
            else player.sendMessage("§cUsage: /aw tp <world>");
        }
        else if (sub.equals("invite")) {
            if (args.length == 2) {
                service.sendInvite(player, args[1]);
            } else {
                String wName = service.getWorldNameByInternal(player.getWorld().getName());
                if(wName != null) plugin.getGui().openOnlinePlayersSelector(player, wName);
                else player.sendMessage("§cYou must be in a specific world or provide a name.");
            }
        }
        else if (sub.equals("accept")) {
            service.acceptInvite(player);
        }
        else if (sub.equals("leave")) {
            service.leaveWorld(player);
        }
        else if (sub.equals("kick")) {
            if (args.length == 2) service.kickPlayer(player, args[1]);
            else player.sendMessage("§cUsage: /aw kick <player>");
        }
        else if (sub.equals("ban")) {
                if (args.length >= 2) {
                    int mins = -1;
                    if (args.length == 3) {
                        try {
                            mins = Integer.parseInt(args[2]);
                        } catch (Exception e) {
                            player.sendMessage("§cInvalid minutes. Use a number or -1 for permanent.");
                            return true;
                        }
                    }
                    service.banPlayer(player, args[1], mins);
                } else {
                player.sendMessage("§cUsage: /aw ban <player> [minutes]");
            }
        }
        else if (sub.equals("like")) {
            String wName = service.getWorldNameByInternal(player.getWorld().getName());
            UUID owner = service.getOwnerUuidByInternalWorldName(player.getWorld().getName());
            if (wName != null && owner != null) {
                service.toggleLike(player, owner, wName);
            } else {
                player.sendMessage("§cYou must be in a player world to like it.");
            }
        }
        else if (sub.equals("unlike")) {
            // Reutilizamos la lÃ³gica de toggle
            String wName = service.getWorldNameByInternal(player.getWorld().getName());
            UUID owner = service.getOwnerUuidByInternalWorldName(player.getWorld().getName());
            if (wName != null && owner != null) {
                service.toggleLike(player, owner, wName);
            } else {
                player.sendMessage("§cYou must be in a player world.");
            }
        }
        else if (sub.equals("rename")) {
            if (args.length == 3) {
                service.renameWorld(player, args[1], args[2]);
            } else {
                player.sendMessage("§cUsage: /aw rename <oldName> <newName>");
            }
        }
        else if (sub.equals("setborder")) {
            if (args.length == 2) {
                try {
                    double radius = Double.parseDouble(args[1]);
                    service.setWorldBorder(player, radius, player.getLocation());
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid number.");
                }
            } else {
                player.sendMessage("§cUsage: /aw setborder <radius>");
            }
        }
        else if (sub.equals("removeborder")) {
            service.removeWorldBorder(player);
        }
        else if (sub.equals("sethome")) {
            if (args.length == 2) {
                String wName = args[1];
                boolean isOwned = service.hasWorld(player.getUniqueId(), wName);
                boolean isServer = service.getAllWorldsList().contains(wName);

                // Verificamos si es miembro (lÃ³gica simplificada para no cargar todos los archivos)
                UUID ownerUuid = service.findWorldOwnerIfMember(wName, player.getUniqueId());

                if (isOwned || isServer || ownerUuid != null) {
                    service.setPlayerSetting(player.getUniqueId(), "homeWorld", wName);
                    player.sendMessage("§aHome world set to: " + wName);
                } else {
                    player.sendMessage("§cYou don't have access to set home here.");
                }
            } else {
                player.sendMessage("§cUsage: /aw sethome <world>");
            }
        }
        else if (sub.equals("setlimit")) {
            if (!player.hasPermission("altworlds.admin")) {
                player.sendMessage("§cNo permission.");
                return true;
            }
            if (args.length == 3) {
                String targetName = args[1];
                String amountStr = args[2];

                // Buscamos UUID escaneando la carpeta de jugadores (Offline support)
                UUID targetUuid = service.findPlayerUuidByName(targetName);

                if (targetUuid == null) {
                    // Intento fallback con Bukkit (si estÃ¡ online o en cachÃ© de usercache.json)
                    Player t = Bukkit.getPlayer(targetName);
                    if (t != null) targetUuid = t.getUniqueId();
                    else {
                        try { targetUuid = Bukkit.getOfflinePlayer(targetName).getUniqueId(); } catch(Exception ignored){}
                    }
                }

                if (targetUuid != null) {
                    try {
                        int amount = Integer.parseInt(amountStr);
                        service.setMaxWorldsForPlayer(targetUuid, amount);
                        player.sendMessage("§aMax worlds for " + targetName + " set to: " + (amount == -1 ? "Unlimited" : amount));
                    } catch (NumberFormatException e) {
                        player.sendMessage("§cInvalid number. Use -1 for unlimited.");
                    }
                } else {
                    player.sendMessage("§cPlayer never joined or not found.");
                }
            } else {
                player.sendMessage("§cUsage: /aw setlimit <player> <amount>");
            }
        }
        // NUEVO
        else if (sub.equals("reload")) {
            if (player.hasPermission("altworlds.admin")) {
                plugin.reloadConfig();
                service.reloadTemplates();
                player.sendMessage("§a[AltWorlds] Config & Templates Reloaded!");
            } else {
                player.sendMessage("§cNo permission.");
            }
        }
        // --- NUEVOS COMANDOS PARA INPUT ---
        else if (sub.equals("confirm")) {
            plugin.getGui().handleCommandConfirm(player);
            return true;
        }
        else if (sub.equals("input")) {
            if (args.length >= 2) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < args.length; i++) sb.append(args[i]).append(" ");
                plugin.getGui().handleCommandInput(player, sb.toString().trim());
            } else {
                player.sendMessage("§cUsage: /aw input <text/number>");
            }
            return true;
        }
        // ----------------------------------
        else {
            plugin.getGui().openMain(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player p)) return Collections.emptyList();

        // Argumento 1: Subcomandos
        if (args.length == 1) {
            List<String> opts = new ArrayList<>(Arrays.asList(
                    "menu", "lobby", "tp", "invite", "accept", "leave", "kick",
                    "ban", "like", "unlike", "name", "cancel", "sethome",
                    "setborder", "removeborder", "rename", "confirm", "input"
            ));
            if (p.hasPermission("altworlds.admin")) {
                opts.add("reload");
                opts.add("setlimit");
            }
            // Filtro parcial (lo que el usuario ya escribiÃ³)
            return opts.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(java.util.stream.Collectors.toList());
        }

        // Argumento 2: Contexto segÃºn el subcomando
        if (args.length == 2) {
            String sub = args[0].toLowerCase();

            // Comandos que requieren un mundo propio
            if (sub.equals("rename") || sub.equals("setborder")) {
                return service.listPlayerWorlds(p.getUniqueId());
            }

            // Comandos que requieren cualquier mundo (Tp, Sethome)
            if (sub.equals("tp") || sub.equals("sethome")) {
                List<String> worlds = new ArrayList<>(service.listPlayerWorlds(p.getUniqueId()));
                worlds.addAll(service.getAllWorldsList()); // Mundos del servidor
                return worlds;
            }

            // Comandos que requieren un jugador (Invite, Kick, Ban, Setlimit)
            if (sub.equals("invite") || sub.equals("kick") || sub.equals("ban") || sub.equals("setlimit")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(java.util.stream.Collectors.toList());
            }

            // Sugerencia visual para input
            if (sub.equals("input")) {
                return Collections.singletonList("<value>");
            }
        }

        return Collections.emptyList();
    }
}
