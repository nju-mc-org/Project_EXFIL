package org.nmo.project_exfil.manager;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.nmo.project_exfil.ProjectEXFILPlugin;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RegionManager {

    private final ProjectEXFILPlugin plugin;
    private final Map<String, ExtractionRegion> regions = new HashMap<>();
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
        if (section == null) return;
        
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

    public void saveRegions() {
        regionsConfig.set("regions", null); // Clear old
        for (Map.Entry<String, ExtractionRegion> entry : regions.entrySet()) {
            String key = entry.getKey();
            ExtractionRegion region = entry.getValue();
            BoundingBox box = region.box;
            
            regionsConfig.set("regions." + key + ".world", region.worldName);
            regionsConfig.set("regions." + key + ".minX", box.getMinX());
            regionsConfig.set("regions." + key + ".minY", box.getMinY());
            regionsConfig.set("regions." + key + ".minZ", box.getMinZ());
            regionsConfig.set("regions." + key + ".maxX", box.getMaxX());
            regionsConfig.set("regions." + key + ".maxY", box.getMaxY());
            regionsConfig.set("regions." + key + ".maxZ", box.getMaxZ());
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
        
        regions.put(name, new ExtractionRegion(admin.getWorld().getName(), box));
        saveRegions();
        
        plugin.getLanguageManager().send(admin, "exfil.region.saved", Placeholder.unparsed("name", name));
    }
    
    public boolean isPlayerInExtractionPoint(Player player, String templateWorldName) {
        Location loc = player.getLocation();
        for (ExtractionRegion region : regions.values()) {
            // Check if region belongs to the template world
            if (region.worldName.equals(templateWorldName) && region.box.contains(loc.toVector())) {
                return true;
            }
        }
        return false;
    }
    
    public String getExtractionPointName(Player player, String templateWorldName) {
        Location loc = player.getLocation();
        for (Map.Entry<String, ExtractionRegion> entry : regions.entrySet()) {
            if (entry.getValue().worldName.equals(templateWorldName) && entry.getValue().box.contains(loc.toVector())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static class ExtractionRegion {
        String worldName;
        BoundingBox box;
        
        ExtractionRegion(String worldName, BoundingBox box) {
            this.worldName = worldName;
            this.box = box;
        }
    }
}
