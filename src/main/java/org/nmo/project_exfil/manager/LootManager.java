package org.nmo.project_exfil.manager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.ui.LootEditorView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class LootManager {

    private final ProjectEXFILPlugin plugin;
    private final File lootFile;
    private YamlConfiguration lootConfig;
    private final List<LootItem> lootTable = new ArrayList<>();
    private final Random random = new Random();
    private LootEditorView lootEditorView;

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
        inventory.clear();
        // Simple algorithm: iterate through all items and roll for each
        // Or pick X items? The user didn't specify.
        // "put items... set probability". Usually implies each item has a chance to appear.
        
        for (LootItem lootItem : lootTable) {
            if (random.nextDouble() <= lootItem.getChance()) {
                // Find a random empty slot or add?
                // If we just add, it might stack or fill up.
                // Let's try to add it.
                inventory.addItem(lootItem.getItem().clone());
            }
        }
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
