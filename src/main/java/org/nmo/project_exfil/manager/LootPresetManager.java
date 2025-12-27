package org.nmo.project_exfil.manager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.nmo.project_exfil.ProjectEXFILPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 战利品预设管理器
 * 支持多个战利品预设，管理员可以选择使用哪个预设
 */
public class LootPresetManager {
    
    private final ProjectEXFILPlugin plugin;
    private final File presetFile;
    private YamlConfiguration presetConfig;
    private final Map<String, LootPreset> presets = new HashMap<>();
    private String defaultPreset = "default";
    
    public LootPresetManager(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
        this.presetFile = new File(plugin.getDataFolder(), "loot_presets.yml");
        loadPresets();
    }
    
    /**
     * 加载所有预设
     */
    private void loadPresets() {
        if (!presetFile.exists()) {
            try {
                presetFile.createNewFile();
                createDefaultPreset();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create loot presets file: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        presetConfig = YamlConfiguration.loadConfiguration(presetFile);
        presets.clear();
        
        // 加载默认预设名称
        defaultPreset = presetConfig.getString("default_preset", "default");
        
        // 加载所有预设
        ConfigurationSection presetsSection = presetConfig.getConfigurationSection("presets");
        if (presetsSection != null) {
            for (String presetName : presetsSection.getKeys(false)) {
                LootPreset preset = loadPreset(presetName, presetsSection.getConfigurationSection(presetName));
                if (preset != null) {
                    presets.put(presetName, preset);
                }
            }
        }
        
        // 如果没有预设，创建默认预设
        if (presets.isEmpty()) {
            createDefaultPreset();
        }
    }
    
    /**
     * 创建默认预设
     */
    private void createDefaultPreset() {
        LootPreset defaultPreset = new LootPreset("default", "默认预设", new ArrayList<>());
        presets.put("default", defaultPreset);
        savePresets();
    }
    
    /**
     * 加载单个预设
     */
    private LootPreset loadPreset(String name, ConfigurationSection section) {
        if (section == null) return null;
        
        String displayName = section.getString("display_name", name);
        List<LootManager.LootItem> items = new ArrayList<>();
        
        ConfigurationSection itemsSection = section.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ItemStack item = itemsSection.getItemStack(key + ".item");
                double chance = itemsSection.getDouble(key + ".chance", 0.1);
                if (item != null) {
                    items.add(new LootManager.LootItem(item, chance));
                }
            }
        }
        
        return new LootPreset(name, displayName, items);
    }
    
    /**
     * 保存所有预设
     */
    public void savePresets() {
        presetConfig.set("default_preset", defaultPreset);
        presetConfig.set("presets", null);
        
        for (Map.Entry<String, LootPreset> entry : presets.entrySet()) {
            String presetName = entry.getKey();
            LootPreset preset = entry.getValue();
            
            presetConfig.set("presets." + presetName + ".display_name", preset.displayName);
            presetConfig.set("presets." + presetName + ".items", null);
            
            for (int i = 0; i < preset.items.size(); i++) {
                LootManager.LootItem item = preset.items.get(i);
                presetConfig.set("presets." + presetName + ".items." + i + ".item", item.getItem());
                presetConfig.set("presets." + presetName + ".items." + i + ".chance", item.getChance());
            }
        }
        
        try {
            presetConfig.save(presetFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save loot presets: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 创建新预设
     */
    public boolean createPreset(String name, String displayName) {
        if (presets.containsKey(name)) {
            return false; // 预设已存在
        }
        
        LootPreset preset = new LootPreset(name, displayName, new ArrayList<>());
        presets.put(name, preset);
        savePresets();
        return true;
    }
    
    /**
     * 删除预设
     */
    public boolean deletePreset(String name) {
        if (name.equals(defaultPreset)) {
            return false; // 不能删除默认预设
        }
        
        if (presets.remove(name) != null) {
            savePresets();
            return true;
        }
        return false;
    }
    
    /**
     * 获取预设
     */
    public LootPreset getPreset(String name) {
        return presets.get(name);
    }
    
    /**
     * 获取所有预设
     */
    public Map<String, LootPreset> getAllPresets() {
        return new HashMap<>(presets);
    }
    
    /**
     * 设置默认预设
     */
    public void setDefaultPreset(String name) {
        if (presets.containsKey(name)) {
            defaultPreset = name;
            savePresets();
        }
    }
    
    /**
     * 获取默认预设
     */
    public LootPreset getDefaultPreset() {
        return presets.get(defaultPreset);
    }
    
    /**
     * 获取默认预设名称
     */
    public String getDefaultPresetName() {
        return defaultPreset;
    }
    
    /**
     * 更新预设
     */
    public void updatePreset(String name, LootPreset preset) {
        presets.put(name, preset);
        savePresets();
    }
    
    /**
     * 战利品预设类
     */
    public static class LootPreset {
        public final String name;
        public final String displayName;
        public final List<LootManager.LootItem> items;
        
        public LootPreset(String name, String displayName, List<LootManager.LootItem> items) {
            this.name = name;
            this.displayName = displayName;
            this.items = new ArrayList<>(items);
        }
        
        /**
         * 复制预设
         */
        public LootPreset copy(String newName) {
            List<LootManager.LootItem> copiedItems = new ArrayList<>();
            for (LootManager.LootItem item : items) {
                copiedItems.add(new LootManager.LootItem(item.getItem().clone(), item.getChance()));
            }
            return new LootPreset(newName, displayName + " (副本)", copiedItems);
        }
    }
}

