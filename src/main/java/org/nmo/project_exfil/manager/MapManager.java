package org.nmo.project_exfil.manager;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.nmo.project_exfil.ProjectEXFILPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import java.util.concurrent.CompletableFuture;

import java.util.ArrayList;
import java.util.List;

public class MapManager {

    private final ProjectEXFILPlugin plugin;
    private final List<GameMap> maps = new ArrayList<>();
    private final File mapsFile;
    private YamlConfiguration mapsConfig;
    private final File importFolder;
    private final File slimeWorldsFolder;

    public MapManager(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
        this.mapsFile = new File(plugin.getDataFolder(), "maps.yml");
        this.importFolder = new File(plugin.getDataFolder(), "import_maps");
        this.slimeWorldsFolder = new File(plugin.getDataFolder(), "slime_worlds");
        
        if (!importFolder.exists()) {
            importFolder.mkdirs();
        }
        if (!slimeWorldsFolder.exists()) {
            slimeWorldsFolder.mkdirs();
        }

        loadMaps();
    }

    private void loadMaps() {
        if (!mapsFile.exists()) {
            try {
                mapsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mapsConfig = YamlConfiguration.loadConfiguration(mapsFile);
        maps.clear();

        ConfigurationSection section = mapsConfig.getConfigurationSection("maps");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String displayName = section.getString(key + ".displayName");
            String fileName = section.getString(key + ".fileName");
            maps.add(new GameMap(key, displayName, fileName));
        }
    }

    public void saveMaps() {
        mapsConfig.set("maps", null);
        for (GameMap map : maps) {
            mapsConfig.set("maps." + map.getId() + ".displayName", map.getDisplayName());
            mapsConfig.set("maps." + map.getId() + ".fileName", map.getFileName());
        }
        try {
            mapsConfig.save(mapsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteDirectory(File file) {
        if (file.isDirectory()) {
            File[] entries = file.listFiles();
            if (entries != null) {
                for (File entry : entries) {
                    deleteDirectory(entry);
                }
            }
        }
        file.delete();
    }

    public void importMap(Player player, String fileName, String displayName) {
        File sourceFile = new File(importFolder, fileName);
        
        // Check for ZIP file
        if (!sourceFile.exists() && !fileName.endsWith(".zip")) {
            File zipFile = new File(importFolder, fileName + ".zip");
            if (zipFile.exists()) {
                sourceFile = zipFile;
            }
        }

        if (!sourceFile.exists()) {
            // Try adding .slime extension if missing
            File slimeFile = new File(importFolder, fileName + ".slime");
            if (slimeFile.exists()) {
                sourceFile = slimeFile;
            } else {
                plugin.getLanguageManager().send(player, "exfil.map.import_failed");
                return;
            }
        }

        if (sourceFile.getName().endsWith(".zip")) {
            plugin.getLanguageManager().send(player, "exfil.map.unzipping");
            final File finalZipFile = sourceFile;
            
            CompletableFuture.runAsync(() -> {
                File tempDir = new File(plugin.getDataFolder(), "temp_import_" + System.currentTimeMillis());
                try {
                    String folderName = finalZipFile.getName().substring(0, finalZipFile.getName().length() - 4);
                    File destDir = new File(tempDir, folderName);
                    
                    // Unzip
                    try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(finalZipFile)))) {
                        ZipEntry entry;
                        while ((entry = zis.getNextEntry()) != null) {
                            // Filter macOS junk files to prevent pollution
                            if (entry.getName().contains("__MACOSX") || entry.getName().contains(".DS_Store")) continue;
                            
                            File newFile = new File(destDir, entry.getName());
                            if (entry.isDirectory()) {
                                newFile.mkdirs();
                            } else {
                                new File(newFile.getParent()).mkdirs();
                                try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(newFile))) {
                                    byte[] buffer = new byte[1024];
                                    int len;
                                    while ((len = zis.read(buffer)) > 0) {
                                        bos.write(buffer, 0, len);
                                    }
                                }
                            }
                        }
                    }
                    
                    // Proceed with folder import from temp dir
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        importMapFolder(player, destDir, displayName, tempDir);
                    });
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    plugin.getLanguageManager().send(player, "exfil.map.import_failed");
                    deleteDirectory(tempDir);
                }
            });
            return;
        }

        if (sourceFile.isDirectory()) {
            importMapFolder(player, sourceFile, displayName, null);
        } else {
            // .slime file copy
            File destFile = new File(slimeWorldsFolder, sourceFile.getName());
            try {
                Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                
                String id = displayName.toLowerCase().replace(" ", "_");
                maps.add(new GameMap(id, displayName, destFile.getName()));
                saveMaps();
                
                plugin.getLanguageManager().send(player, "exfil.map.import_success", Placeholder.unparsed("map", displayName));
            } catch (IOException e) {
                e.printStackTrace();
                plugin.getLanguageManager().send(player, "exfil.map.import_failed");
            }
        }
    }

    private void importMapFolder(Player player, File sourceFile, String displayName, File tempCleanupDir) {
        // Check for level.dat
        File levelDat = new File(sourceFile, "level.dat");
        // Sometimes zip extracts to a subfolder
        if (!levelDat.exists()) {
            File[] subDirs = sourceFile.listFiles(File::isDirectory);
            if (subDirs != null && subDirs.length == 1) {
                sourceFile = subDirs[0];
                levelDat = new File(sourceFile, "level.dat");
            }
        }
        
        if (!levelDat.exists()) {
            plugin.getLanguageManager().send(player, "exfil.map.error.invalid_format");
            if (tempCleanupDir != null) deleteDirectory(tempCleanupDir);
            return;
        }

        plugin.getLanguageManager().send(player, "exfil.map.importing_vanilla");
        final File finalSourceFile = sourceFile;
        
        plugin.getGameManager().getSlimeManager().importVanillaWorld(finalSourceFile, sourceFile.getName())
            .thenRun(() -> {
                // Register
                String id = displayName.toLowerCase().replace(" ", "_");
                String destFileName = finalSourceFile.getName() + ".slime";
                
                maps.add(new GameMap(id, displayName, destFileName));
                saveMaps();
                
                plugin.getLanguageManager().send(player, "exfil.map.import_success", Placeholder.unparsed("map", displayName));
                
                if (tempCleanupDir != null) {
                    deleteDirectory(tempCleanupDir);
                }
            })
            .exceptionally(e -> {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException && cause.getCause() != null) {
                    cause = cause.getCause();
                }
                
                if (cause instanceof java.io.EOFException) {
                    plugin.getLanguageManager().send(player, "exfil.map.error.corrupted", Placeholder.unparsed("error", "EOFException (File truncated)"));
                } else {
                    e.printStackTrace();
                    plugin.getLanguageManager().send(player, "exfil.map.import_failed");
                }
                
                if (tempCleanupDir != null) {
                    deleteDirectory(tempCleanupDir);
                }
                return null;
            });
    }

    public boolean deleteMap(String id) {
        GameMap target = null;
        for (GameMap map : maps) {
            if (map.getId().equalsIgnoreCase(id)) {
                target = map;
                break;
            }
        }
        if (target != null) {
            // Delete file from slime_worlds
            File mapFile = new File(slimeWorldsFolder, target.getFileName());
            if (mapFile.exists()) {
                mapFile.delete();
            }
            
            maps.remove(target);
            saveMaps();
            return true;
        }
        return false;
    }

    public List<GameMap> getMaps() {
        return maps;
    }

    public static class GameMap {
        private final String id;
        private final String displayName;
        private final String fileName;

        public GameMap(String id, String displayName, String fileName) {
            this.id = id;
            this.displayName = displayName;
            this.fileName = fileName;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public String getFileName() { return fileName; }
        
        public String getTemplateName() {
            if (fileName.endsWith(".slime")) {
                return fileName.substring(0, fileName.length() - 6);
            }
            return fileName;
        }
    }
}
