package org.nmo.project_exfil.manager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.ui.LootEditorView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LootManager {

    private final ProjectEXFILPlugin plugin;
    private final File lootFile;
    private YamlConfiguration lootConfig;
    private final List<LootItem> lootTable = new ArrayList<>();
    private final Random random = new Random();
    private LootEditorView lootEditorView;
    private String currentPreset = "default";

    public LootManager(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
        this.lootFile = new File(plugin.getDataFolder(), "loot.yml");
        loadLootTable();
    }
    
    public void setLootEditorView(LootEditorView view) {
        this.lootEditorView = view;
    }

    private void loadLootTable() {
        if (!lootFile.exists()) {
            try {
                lootFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        lootConfig = YamlConfiguration.loadConfiguration(lootFile);
        
        lootTable.clear();
        ConfigurationSection section = lootConfig.getConfigurationSection("items");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ItemStack item = section.getItemStack(key + ".item");
                double chance = section.getDouble(key + ".chance");
                if (item != null) {
                    lootTable.add(new LootItem(item, chance));
                }
            }
        }
    }

    public void saveLootTable() {
        lootConfig.set("items", null);
        for (int i = 0; i < lootTable.size(); i++) {
            LootItem lootItem = lootTable.get(i);
            lootConfig.set("items." + i + ".item", lootItem.getItem());
            lootConfig.set("items." + i + ".chance", lootItem.getChance());
        }
        
        try {
            lootConfig.save(lootFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openEditor(Player player) {
        if (lootEditorView == null) {
            lootEditorView = new LootEditorView(plugin, this);
        }
        lootEditorView.open(player);
    }

    public List<LootItem> getLootTable() {
        return lootTable;
    }

    public void updateLootTable(List<LootItem> newTable) {
        this.lootTable.clear();
        this.lootTable.addAll(newTable);
        saveLootTable();
    }

    public void generateLoot(Inventory inventory) {
        generateLoot(inventory, currentPreset);
    }
    
    /**
     * 使用指定预设生成战利品
     */
    public void generateLoot(Inventory inventory, String presetName) {
        inventory.clear();
        
        // 获取预设
        org.nmo.project_exfil.manager.LootPresetManager presetManager = 
            plugin.getLootPresetManager();
        if (presetManager == null) {
            // 如果没有预设管理器，使用默认战利品表
            generateLootFromTable(inventory);
            return;
        }
        
        org.nmo.project_exfil.manager.LootPresetManager.LootPreset preset = 
            presetManager.getPreset(presetName);
        if (preset == null) {
            preset = presetManager.getDefaultPreset();
        }
        
        if (preset != null) {
            // 使用预设生成
            for (LootItem lootItem : preset.items) {
                if (random.nextDouble() <= lootItem.getChance()) {
                    inventory.addItem(lootItem.getItem().clone());
                }
            }
        } else {
            // 后备：使用默认战利品表
            generateLootFromTable(inventory);
        }
    }
    
    /**
     * 从默认战利品表生成
     */
    private void generateLootFromTable(Inventory inventory) {
        for (LootItem lootItem : lootTable) {
            if (random.nextDouble() <= lootItem.getChance()) {
                inventory.addItem(lootItem.getItem().clone());
            }
        }
    }
    
    /**
     * 设置当前使用的预设
     */
    public void setCurrentPreset(String presetName) {
        this.currentPreset = presetName;
    }
    
    /**
     * 获取当前预设
     */
    public String getCurrentPreset() {
        return currentPreset;
    }

    public static class LootItem {
        private final ItemStack item;
        private double chance;

        public LootItem(ItemStack item, double chance) {
            this.item = item;
            this.chance = chance;
        }

        public ItemStack getItem() {
            return item;
        }

        public double getChance() {
            return chance;
        }
        
        public void setChance(double chance) {
            this.chance = chance;
        }
    }
}
