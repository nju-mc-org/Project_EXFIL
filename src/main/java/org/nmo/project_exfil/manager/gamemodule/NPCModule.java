package org.nmo.project_exfil.manager.gamemodule;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.manager.GameInstance;
import org.nmo.project_exfil.region.NPCRegion;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;
import org.mcmonkey.sentinel.SentinelTrait;
import me.zombie_striker.qg.api.QualityArmory;

import java.util.ArrayList;
import java.util.List;

public class NPCModule implements GameModule {

    private final List<NPC> npcs = new ArrayList<>();

    @Override
    public void onStart(GameInstance game) {
        List<NPCRegion> regions = ProjectEXFILPlugin.getPlugin().getRegionManager().getNPCRegionsForWorld(game.getTemplateName());
        for (NPCRegion region : regions) {
            for (int i = 0; i < region.getCount(); i++) {
                Location loc = findRandomLocationInBox(game, region.getBox());
                if (loc != null) {
                    spawnSoldier(loc);
                }
            }
        }
    }

    @Override
    public void onEnd(GameInstance game) {
        for (NPC npc : npcs) {
            npc.destroy();
        }
        npcs.clear();
    }

    private Location findRandomLocationInBox(GameInstance game, org.bukkit.util.BoundingBox box) {
        for (int i = 0; i < 10; i++) {
            double x = box.getMinX() + Math.random() * (box.getMaxX() - box.getMinX());
            double z = box.getMinZ() + Math.random() * (box.getMaxZ() - box.getMinZ());
            int y = game.getBukkitWorld().getHighestBlockYAt((int)x, (int)z);
            
            if (y >= box.getMinY() && y <= box.getMaxY()) {
                return new Location(game.getBukkitWorld(), x, y + 1, z);
            }
        }
        return null;
    }

    private void spawnSoldier(Location loc) {
        NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "Scavenger");
        npc.spawn(loc);
        npcs.add(npc);

        SentinelTrait sentinel = npc.getOrAddTrait(SentinelTrait.class);
        sentinel.addTarget("PLAYERS");
        // sentinel.addAvoid("PLAYERS"); // Causing crash due to Citizens/Sentinel version mismatch
        // sentinel.avoidRange = 10.0;
        
        sentinel.range = 35.0;
        sentinel.chaseRange = 35.0;
        sentinel.realistic = true;
        sentinel.setHealth(20);
        
        sentinel.respawnTime = -1;
        sentinel.rangedChase = true; // Enable movement
        sentinel.closeChase = false; // Disable melee chase to prevent rushing
        sentinel.chaseRange = 10.0; // Limit movement radius
        sentinel.accuracy = 10.0;
        
        try {
            org.bukkit.inventory.ItemStack weapon = QualityArmory.getCustomItemAsItemStack("ak47");
            if (weapon == null) {
                weapon = QualityArmory.getCustomItemAsItemStack("m4a1s");
            }
            
            if (weapon != null) {
                Equipment equip = npc.getOrAddTrait(Equipment.class);
                equip.set(Equipment.EquipmentSlot.HAND, weapon);
            } else {
                Equipment equip = npc.getOrAddTrait(Equipment.class);
                equip.set(Equipment.EquipmentSlot.HAND, new org.bukkit.inventory.ItemStack(Material.IRON_SWORD));
            }
        } catch (NoClassDefFoundError | Exception e) {
            Equipment equip = npc.getOrAddTrait(Equipment.class);
            equip.set(Equipment.EquipmentSlot.HAND, new org.bukkit.inventory.ItemStack(Material.IRON_SWORD));
        }
    }
}
