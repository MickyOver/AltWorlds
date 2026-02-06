package com.altworlds;

import org.bukkit.World;
import org.bukkit.entity.Player;
import java.util.UUID;

public class WorldAccess {
    private final AltWorldsService service;

    public WorldAccess(AltWorldsService service) {
        this.service = service;
    }

    public boolean isAltWorld(World world) {
        return service.getOwnerUuidByInternalWorldName(world.getName()) != null;
    }

    private Role getRole(Player p, World w) {
        UUID playerUuid = p.getUniqueId();
        String internalName = w.getName();

        UUID ownerUuid = service.getOwnerUuidByInternalWorldName(internalName);
        if (ownerUuid == null) return Role.VISITOR;
        if (ownerUuid.equals(playerUuid)) return Role.OWNER;

        String realName = service.getWorldNameByInternal(internalName);
        if (realName != null) {
            if (service.isMember(ownerUuid, realName, playerUuid)) {
                return Role.MEMBER;
            }
        }
        return Role.VISITOR;
    }

    public boolean canBuild(Player p, World w) {
        Role r = getRole(p, w);
        return r == Role.OWNER || r == Role.MEMBER || p.hasPermission("altworlds.admin");
    }

    public boolean canInteract(Player p, World w) {
        Role r = getRole(p, w);
        return r == Role.OWNER || r == Role.MEMBER || p.hasPermission("altworlds.admin");
    }

    public boolean canOpenContainers(Player p, World w) {
        Role r = getRole(p, w);
        return r == Role.OWNER || r == Role.MEMBER || p.hasPermission("altworlds.admin");
    }
}