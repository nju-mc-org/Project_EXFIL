package org.nmo.project_exfil.manager.gamemodule;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.manager.GameInstance;
import org.nmo.project_exfil.region.LootRegion;

import org.bukkit.inventory.Inventory;
import org.nmo.project_exfil.manager.LootManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class LootModule implements GameModule {

    private final Random random = new Random();
    private final Map<Location, Inventory> lootInventories = new HashMap<>();
    private static final String TEXTURE_VALUE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzRlNjI2MjJkM2VjMGI4ZTFiZjc0NjIxNTY1Njg0MTdjODg1OTM3YzE2OTI5MmE1MGNkOTc3ZWFmMTQ3NWQ2In19fQ==";
    private static final String UUID_STRING = "720faeb5-cd75-4293-837e-601c1b32a11d";

    public Inventory getInventory(Location loc) {
        if (lootInventories.containsKey(loc)) {
            return lootInventories.get(loc);
        }
        
        Inventory inv = org.bukkit.Bukkit.createInventory(null, 27, ProjectEXFILPlugin.getPlugin().getLanguageManager().getMessage("exfil.loot.box.title"));
        ProjectEXFILPlugin.getPlugin().getLootManager().generateLoot(inv);
        lootInventories.put(loc, inv);
        return inv;
    }

    @Override
    public void onStart(GameInstance instance) {
        List<LootRegion> regions = ProjectEXFILPlugin.getPlugin().getRegionManager().getLootRegionsForWorld(instance.getTemplateName());
        
        for (LootRegion region : regions) {
            spawnLootBoxes(instance, region);
        }
    }

    private void spawnLootBoxes(GameInstance instance, LootRegion region) {
        BoundingBox box = region.getBox();
        int count = region.getCount();
        World world = instance.getBukkitWorld();
        
        int minX = (int) Math.floor(box.getMinX());
        int maxX = (int) Math.floor(box.getMaxX());
        int minY = (int) Math.floor(box.getMinY());
        int maxY = (int) Math.floor(box.getMaxY());
        int minZ = (int) Math.floor(box.getMinZ());
        int maxZ = (int) Math.floor(box.getMaxZ());
        
        for (int i = 0; i < count; i++) {
            // Try to find a valid location
            for (int attempt = 0; attempt < 10; attempt++) {
                int x = random.nextInt(maxX - minX + 1) + minX;
                int z = random.nextInt(maxZ - minZ + 1) + minZ;
                
                boolean placed = false;
                // Scan from top to bottom to find a surface
                for (int y = maxY; y >= minY; y--) {
                    Block current = world.getBlockAt(x, y, z);
                    Block below = world.getBlockAt(x, y - 1, z);
                    
                    if (current.getType().isAir() && below.getType().isSolid()) {
                        current.setType(Material.PLAYER_HEAD);
                        
                        if (current.getState() instanceof Skull) {
                            Skull skull = (Skull) current.getState();
                            setSkullTexture(skull);
                            skull.update();
                        }
                        placed = true;
                        break;
                    }
                }
                if (placed) break;
            }
        }
    }

    private void setSkullTexture(Skull skull) {
        PlayerProfile profile = Bukkit.createProfile(UUID.fromString(UUID_STRING), "Box");
        profile.setProperty(new ProfileProperty("textures", TEXTURE_VALUE));
        skull.setPlayerProfile(profile);
    }

    @Override
    public void onPlayerJoin(GameInstance instance, Player player) {}

    @Override
    public void onPlayerQuit(GameInstance instance, Player player) {}

    @Override
    public void onTick(GameInstance instance) {}

    @Override
    public void onEnd(GameInstance instance) {}
}
