package org.nmo.project_exfil.manager;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.nmo.project_exfil.ProjectEXFILPlugin;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import org.nmo.project_exfil.util.DependencyHelper;

import org.nmo.project_exfil.region.CombatRegion;
import org.nmo.project_exfil.region.ExtractionRegion;
import org.nmo.project_exfil.region.NPCRegion;
import org.nmo.project_exfil.region.SpawnRegion;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegionManager {

    private final ProjectEXFILPlugin plugin;
    private final Map<String, ExtractionRegion> regions = new HashMap<>();
    private final Map<String, SpawnRegion> spawnRegions = new HashMap<>();
    private final Map<String, CombatRegion> combatRegions = new HashMap<>();
    private final Map<String, NPCRegion> npcRegions = new HashMap<>();
    private final File regionsFile;
    private YamlConfiguration regionsConfig;

    public RegionManager(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
        this.regionsFile = new File(plugin.getDataFolder(), "regions.yml");
        loadRegions();
    }

    private void loadRegions() {
        if (!regionsFile.exists()) {
            try {
                regionsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        regionsConfig = YamlConfiguration.loadConfiguration(regionsFile);
        
        ConfigurationSection section = regionsConfig.getConfigurationSection("regions");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String worldName = section.getString(key + ".world");
                double minX = section.getDouble(key + ".minX");
                double minY = section.getDouble(key + ".minY");
                double minZ = section.getDouble(key + ".minZ");
                double maxX = section.getDouble(key + ".maxX");
                double maxY = section.getDouble(key + ".maxY");
                double maxZ = section.getDouble(key + ".maxZ");
                
                BoundingBox box = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
                regions.put(key, new ExtractionRegion(worldName, box));
            }
        }

        ConfigurationSection spawnSection = regionsConfig.getConfigurationSection("spawns");
        if (spawnSection != null) {
            for (String key : spawnSection.getKeys(false)) {
                String worldName = spawnSection.getString(key + ".world");
                double x = spawnSection.getDouble(key + ".x");
                double y = spawnSection.getDouble(key + ".y");
                double z = spawnSection.getDouble(key + ".z");
                double radius = spawnSection.getDouble(key + ".radius");
                
                spawnRegions.put(worldName, new SpawnRegion(worldName, x, y, z, radius));
            }
        }

        ConfigurationSection combatSection = regionsConfig.getConfigurationSection("combat");
        if (combatSection != null) {
            for (String key : combatSection.getKeys(false)) {
                String worldName = combatSection.getString(key + ".world");
                double minX = combatSection.getDouble(key + ".minX");
                double minZ = combatSection.getDouble(key + ".minZ");
                double maxX = combatSection.getDouble(key + ".maxX");
                double maxZ = combatSection.getDouble(key + ".maxZ");
                
                combatRegions.put(worldName, new CombatRegion(worldName, minX, minZ, maxX, maxZ));
            }
        }

        ConfigurationSection npcSection = regionsConfig.getConfigurationSection("npcs");
        if (npcSection != null) {
            for (String key : npcSection.getKeys(false)) {
                String worldName = npcSection.getString(key + ".world");
                double minX = npcSection.getDouble(key + ".minX");
                double minY = npcSection.getDouble(key + ".minY");
                double minZ = npcSection.getDouble(key + ".minZ");
                double maxX = npcSection.getDouble(key + ".maxX");
                double maxY = npcSection.getDouble(key + ".maxY");
                double maxZ = npcSection.getDouble(key + ".maxZ");
                int count = npcSection.getInt(key + ".count");
                
                BoundingBox box = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
                npcRegions.put(key, new NPCRegion(worldName, box, count));
            }
        }
    }

    public void saveRegions() {
        regionsConfig.set("regions", null); // Clear old
        for (Map.Entry<String, ExtractionRegion> entry : regions.entrySet()) {
            String key = entry.getKey();
            ExtractionRegion region = entry.getValue();
            BoundingBox box = region.getBox();
            
            regionsConfig.set("regions." + key + ".world", region.getWorldName());
            regionsConfig.set("regions." + key + ".minX", box.getMinX());
            regionsConfig.set("regions." + key + ".minY", box.getMinY());
            regionsConfig.set("regions." + key + ".minZ", box.getMinZ());
            regionsConfig.set("regions." + key + ".maxX", box.getMaxX());
            regionsConfig.set("regions." + key + ".maxY", box.getMaxY());
            regionsConfig.set("regions." + key + ".maxZ", box.getMaxZ());
        }

        regionsConfig.set("spawns", null);
        for (Map.Entry<String, SpawnRegion> entry : spawnRegions.entrySet()) {
            String key = entry.getKey(); // Use world name as key for spawns
            SpawnRegion region = entry.getValue();
            
            regionsConfig.set("spawns." + key + ".world", region.getWorldName());
            regionsConfig.set("spawns." + key + ".x", region.getX());
            regionsConfig.set("spawns." + key + ".y", region.getY());
            regionsConfig.set("spawns." + key + ".z", region.getZ());
            regionsConfig.set("spawns." + key + ".radius", region.getRadius());
        }

        regionsConfig.set("combat", null);
        for (Map.Entry<String, CombatRegion> entry : combatRegions.entrySet()) {
            String key = entry.getKey();
            CombatRegion region = entry.getValue();
            
            regionsConfig.set("combat." + key + ".world", region.getWorldName());
            regionsConfig.set("combat." + key + ".minX", region.getMinX());
            regionsConfig.set("combat." + key + ".minZ", region.getMinZ());
            regionsConfig.set("combat." + key + ".maxX", region.getMaxX());
            regionsConfig.set("combat." + key + ".maxZ", region.getMaxZ());
        }

        regionsConfig.set("npcs", null);
        for (Map.Entry<String, NPCRegion> entry : npcRegions.entrySet()) {
            String key = entry.getKey();
            NPCRegion region = entry.getValue();
            BoundingBox box = region.getBox();
            
            regionsConfig.set("npcs." + key + ".world", region.getWorldName());
            regionsConfig.set("npcs." + key + ".minX", box.getMinX());
            regionsConfig.set("npcs." + key + ".minY", box.getMinY());
            regionsConfig.set("npcs." + key + ".minZ", box.getMinZ());
            regionsConfig.set("npcs." + key + ".maxX", box.getMaxX());
            regionsConfig.set("npcs." + key + ".maxY", box.getMaxY());
            regionsConfig.set("npcs." + key + ".maxZ", box.getMaxZ());
            regionsConfig.set("npcs." + key + ".count", region.getCount());
        }
        
        try {
            regionsConfig.save(regionsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Region getPlayerSelection(Player player) {
        SessionManager manager = WorldEdit.getInstance().getSessionManager();
        try {
            return manager.get(BukkitAdapter.adapt(player)).getSelection(BukkitAdapter.adapt(player.getWorld()));
        } catch (IncompleteRegionException e) {
            plugin.getLanguageManager().send(player, "exfil.region.incomplete");
            return null;
        } catch (Exception e) {
              return null;
        }
    }

    public void saveExtractionPoint(Player admin, String name) {
        Region selection = getPlayerSelection(admin);
        if (selection == null) return;

        BoundingBox box = new BoundingBox(
            selection.getMinimumPoint().x(),
            selection.getMinimumPoint().y(),
            selection.getMinimumPoint().z(),
            selection.getMaximumPoint().x(),
            selection.getMaximumPoint().y(),
            selection.getMaximumPoint().z()
        );
        
        String worldName = plugin.getGameManager().getTemplateName(admin.getWorld());
        regions.put(name, new ExtractionRegion(worldName, box));
        saveRegions();
        
        // Update Hologram
        updateHologram(name, worldName, box);
        
        plugin.getLanguageManager().send(admin, "exfil.region.saved", Placeholder.unparsed("name", name));
    }
    
    private void updateHologram(String name, String worldName, BoundingBox box) {
        // Only create holograms for template worlds (where admins edit)
        // For game instances, we might need to spawn them dynamically per instance?
        // The user request implies "in the extraction point", which exists in game instances.
        // However, DecentHolograms are usually global or per-world.
        // If we create a hologram in the template world, SlimeWorldManager might not copy it to instances automatically if it's an entity.
        // But DecentHolograms stores holograms in its own config.
        // If we want holograms in instances, we need to create them when the instance is created.
        
        // Wait, the user says "extraction point central suspended".
        // If I create it here, it's for the template world.
        // When a game instance is created, we need to spawn holograms for that instance.
        
        // Let's just save the data here. The GameInstance should handle spawning holograms for its world.
        // But wait, if the admin is editing in a normal world, they want to see it too?
        // Let's spawn it in the world specified.
        
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            Location center = new Location(world, box.getCenterX(), box.getCenterY(), box.getCenterZ());
            DependencyHelper.createStaticExtractionHologram(name, center);
        }
    }

    public boolean isPlayerInExtractionPoint(Player player, String templateWorldName) {
        Location loc = player.getLocation();
        for (ExtractionRegion region : regions.values()) {
            // Check if region belongs to the template world
            if (region.getWorldName().equals(templateWorldName) && region.getBox().contains(loc.toVector())) {
                return true;
            }
        }
        return false;
    }
    
    public String getExtractionPointName(Player player, String templateWorldName) {
        Location loc = player.getLocation();
        for (Map.Entry<String, ExtractionRegion> entry : regions.entrySet()) {
            if (entry.getValue().getWorldName().equals(templateWorldName) && entry.getValue().getBox().contains(loc.toVector())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void saveSpawnRegion(Player admin, double radius) {
        String worldName = plugin.getGameManager().getTemplateName(admin.getWorld());
        Location loc = admin.getLocation();
        spawnRegions.put(worldName, new SpawnRegion(worldName, loc.getX(), loc.getY(), loc.getZ(), radius));
        saveRegions();
        plugin.getLanguageManager().send(admin, "exfil.region.spawn_saved", Placeholder.unparsed("radius", String.valueOf(radius)));
    }

    public void saveCombatRegion(Player admin) {
        Region selection = getPlayerSelection(admin);
        if (selection == null) return;

        String worldName = plugin.getGameManager().getTemplateName(admin.getWorld());
        double minX = selection.getMinimumPoint().x();
        double minZ = selection.getMinimumPoint().z();
        double maxX = selection.getMaximumPoint().x();
        double maxZ = selection.getMaximumPoint().z();

        combatRegions.put(worldName, new CombatRegion(worldName, minX, minZ, maxX, maxZ));
        saveRegions();
        
        plugin.getLanguageManager().send(admin, "exfil.region.combat_saved");
    }

    public void saveNPCRegion(Player admin, String name, int count) {
        Region selection = getPlayerSelection(admin);
        if (selection == null) return;

        BoundingBox box = new BoundingBox(
            selection.getMinimumPoint().x(),
            selection.getMinimumPoint().y(),
            selection.getMinimumPoint().z(),
            selection.getMaximumPoint().x(),
            selection.getMaximumPoint().y(),
            selection.getMaximumPoint().z()
        );
        
        String worldName = plugin.getGameManager().getTemplateName(admin.getWorld());
        npcRegions.put(name, new NPCRegion(worldName, box, count));
        saveRegions();
        
        plugin.getLanguageManager().send(admin, "exfil.region.npc_saved", 
            Placeholder.unparsed("name", name),
            Placeholder.unparsed("count", String.valueOf(count)));
    }

    public SpawnRegion getSpawnRegion(String templateWorldName) {
        return spawnRegions.get(templateWorldName);
    }
    
    public Map<String, ExtractionRegion> getExtractionRegions(String templateWorldName) {
        Map<String, ExtractionRegion> result = new HashMap<>();
        for (Map.Entry<String, ExtractionRegion> entry : regions.entrySet()) {
            if (entry.getValue().getWorldName().equals(templateWorldName)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public boolean deleteExtractionRegion(String name) {
        if (regions.remove(name) != null) {
            saveRegions();
            DependencyHelper.removeStaticExtractionHologram(name);
            return true;
        }
        return false;
    }

    public boolean deleteCombatRegion(String worldName) {
        if (combatRegions.remove(worldName) != null) {
            saveRegions();
            return true;
        }
        return false;
    }

    public boolean deleteNPCRegion(String name) {
        if (npcRegions.remove(name) != null) {
            saveRegions();
            return true;
        }
        return false;
    }

    public Map<String, ExtractionRegion> getAllExtractionRegions() {
        return regions;
    }

    public Map<String, CombatRegion> getAllCombatRegions() {
        return combatRegions;
    }

    public Map<String, NPCRegion> getAllNPCRegions() {
        return npcRegions;
    }

    public List<NPCRegion> getNPCRegionsForWorld(String templateWorldName) {
        List<NPCRegion> list = new java.util.ArrayList<>();
        for (NPCRegion region : npcRegions.values()) {
            if (region.getWorldName().equals(templateWorldName)) {
                list.add(region);
            }
        }
        return list;
    }

    public CombatRegion getCombatRegion(String templateWorldName) {
        return combatRegions.get(templateWorldName);
    }
}
