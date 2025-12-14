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
import org.nmo.project_exfil.region.LootRegion;
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
    private final Map<String, LootRegion> lootRegions = new HashMap<>();

    private final Map<String, Map<String, ExtractionRegion>> extractionRegionsByWorld = new HashMap<>();
    private final Map<String, List<NPCRegion>> npcRegionsByWorld = new HashMap<>();
    private final Map<String, List<LootRegion>> lootRegionsByWorld = new HashMap<>();

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
                BoundingBox box = loadBoundingBox(section, key);
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
                int count = npcSection.getInt(key + ".count");
                BoundingBox box = loadBoundingBox(npcSection, key);
                npcRegions.put(key, new NPCRegion(worldName, box, count));
            }
        }

        ConfigurationSection lootSection = regionsConfig.getConfigurationSection("loot");
        if (lootSection != null) {
            for (String key : lootSection.getKeys(false)) {
                String worldName = lootSection.getString(key + ".world");
                int count = lootSection.getInt(key + ".count");
                BoundingBox box = loadBoundingBox(lootSection, key);
                lootRegions.put(key, new LootRegion(worldName, box, count));
            }
        }

        rebuildCaches();
    }

    private void rebuildCaches() {
        extractionRegionsByWorld.clear();
        for (Map.Entry<String, ExtractionRegion> entry : regions.entrySet()) {
            ExtractionRegion region = entry.getValue();
            extractionRegionsByWorld.computeIfAbsent(region.getWorldName(), k -> new HashMap<>()).put(entry.getKey(), region);
        }

        npcRegionsByWorld.clear();
        for (NPCRegion region : npcRegions.values()) {
            npcRegionsByWorld.computeIfAbsent(region.getWorldName(), k -> new java.util.ArrayList<>()).add(region);
        }

        lootRegionsByWorld.clear();
        for (LootRegion region : lootRegions.values()) {
            lootRegionsByWorld.computeIfAbsent(region.getWorldName(), k -> new java.util.ArrayList<>()).add(region);
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

        regionsConfig.set("loot", null);
        for (Map.Entry<String, LootRegion> entry : lootRegions.entrySet()) {
            String key = entry.getKey();
            LootRegion region = entry.getValue();
            BoundingBox box = region.getBox();
            
            regionsConfig.set("loot." + key + ".world", region.getWorldName());
            regionsConfig.set("loot." + key + ".minX", box.getMinX());
            regionsConfig.set("loot." + key + ".minY", box.getMinY());
            regionsConfig.set("loot." + key + ".minZ", box.getMinZ());
            regionsConfig.set("loot." + key + ".maxX", box.getMaxX());
            regionsConfig.set("loot." + key + ".maxY", box.getMaxY());
            regionsConfig.set("loot." + key + ".maxZ", box.getMaxZ());
            regionsConfig.set("loot." + key + ".count", region.getCount());
        }
        
        try {
            regionsConfig.save(regionsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        rebuildCaches();
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

        BoundingBox box = getSelectionBox(selection);
        
        String worldName = plugin.getGameManager().getTemplateName(admin.getWorld());
        regions.put(name, new ExtractionRegion(worldName, box));
        saveRegions();
        
        // Update Hologram
        updateHologram(name, worldName, box);
        
        plugin.getLanguageManager().send(admin, "exfil.region.saved", Placeholder.unparsed("name", name));
    }
    
    private void updateHologram(String name, String worldName, BoundingBox box) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            Location center = new Location(world, box.getCenterX(), box.getCenterY(), box.getCenterZ());
            DependencyHelper.createStaticExtractionHologram(name, center);
        }
    }

    public boolean isPlayerInExtractionPoint(Player player, String templateWorldName) {
        Location loc = player.getLocation();
        for (ExtractionRegion region : getExtractionRegions(templateWorldName).values()) {
            if (region.getBox().contains(loc.toVector())) {
                return true;
            }
        }
        return false;
    }
    
    public String getExtractionPointName(Player player, String templateWorldName) {
        Location loc = player.getLocation();
        for (Map.Entry<String, ExtractionRegion> entry : getExtractionRegions(templateWorldName).entrySet()) {
            if (entry.getValue().getBox().contains(loc.toVector())) {
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

        BoundingBox box = getSelectionBox(selection);
        
        String worldName = plugin.getGameManager().getTemplateName(admin.getWorld());
        npcRegions.put(name, new NPCRegion(worldName, box, count));
        saveRegions();
        
        plugin.getLanguageManager().send(admin, "exfil.region.npc_saved", 
            Placeholder.unparsed("name", name),
            Placeholder.unparsed("count", String.valueOf(count)));
    }

    public void saveLootRegion(Player admin, String name, int count) {
        Region selection = getPlayerSelection(admin);
        if (selection == null) return;

        BoundingBox box = getSelectionBox(selection);
        
        String worldName = plugin.getGameManager().getTemplateName(admin.getWorld());
        lootRegions.put(name, new LootRegion(worldName, box, count));
        saveRegions();
        
        plugin.getLanguageManager().send(admin, "exfil.region.loot_saved", 
            Placeholder.unparsed("name", name),
            Placeholder.unparsed("count", String.valueOf(count)));
    }

    public SpawnRegion getSpawnRegion(String templateWorldName) {
        return spawnRegions.get(templateWorldName);
    }
    
    public Map<String, ExtractionRegion> getExtractionRegions(String templateWorldName) {
        Map<String, ExtractionRegion> result = extractionRegionsByWorld.get(templateWorldName);
        return result != null ? result : java.util.Collections.emptyMap();
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

    public boolean deleteLootRegion(String name) {
        if (lootRegions.remove(name) != null) {
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

    public Map<String, LootRegion> getAllLootRegions() {
        return lootRegions;
    }

    public List<NPCRegion> getNPCRegionsForWorld(String templateWorldName) {
        List<NPCRegion> list = npcRegionsByWorld.get(templateWorldName);
        return list != null ? list : java.util.Collections.emptyList();
    }

    public List<LootRegion> getLootRegionsForWorld(String templateWorldName) {
        List<LootRegion> list = lootRegionsByWorld.get(templateWorldName);
        return list != null ? list : java.util.Collections.emptyList();
    }

    public CombatRegion getCombatRegion(String templateWorldName) {
        return combatRegions.get(templateWorldName);
    }

    private BoundingBox loadBoundingBox(ConfigurationSection section, String key) {
        double minX = section.getDouble(key + ".minX");
        double minY = section.getDouble(key + ".minY");
        double minZ = section.getDouble(key + ".minZ");
        double maxX = section.getDouble(key + ".maxX");
        double maxY = section.getDouble(key + ".maxY");
        double maxZ = section.getDouble(key + ".maxZ");
        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private BoundingBox getSelectionBox(Region selection) {
        return new BoundingBox(
            selection.getMinimumPoint().x(),
            selection.getMinimumPoint().y(),
            selection.getMinimumPoint().z(),
            selection.getMaximumPoint().x(),
            selection.getMaximumPoint().y(),
            selection.getMaximumPoint().z()
        );
    }
}
