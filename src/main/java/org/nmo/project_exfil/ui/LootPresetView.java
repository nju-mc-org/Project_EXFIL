package org.nmo.project_exfil.ui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.manager.LootPresetManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 战利品预设管理界面
 */
public class LootPresetView {
    
    private final LootPresetManager presetManager;
    private final ProjectEXFILPlugin plugin;
    
    public LootPresetView(LootPresetManager presetManager) {
        this.presetManager = presetManager;
        this.plugin = ProjectEXFILPlugin.getPlugin();
    }
    
    public void open(Player player) {
        ChestGui gui = new ChestGui(6, "战利品预设管理");
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        
        PaginatedPane pages = new PaginatedPane(0, 0, 9, 5);
        List<GuiItem> items = new ArrayList<>();
        
        Map<String, LootPresetManager.LootPreset> presets = presetManager.getAllPresets();
        String defaultPreset = presetManager.getDefaultPresetName();
        
        for (Map.Entry<String, LootPresetManager.LootPreset> entry : presets.entrySet()) {
            String presetName = entry.getKey();
            LootPresetManager.LootPreset preset = entry.getValue();
            boolean isDefault = presetName.equals(defaultPreset);
            
            ItemStack presetItem = new ItemStack(isDefault ? Material.GOLD_BLOCK : Material.CHEST);
            ItemMeta meta = presetItem.getItemMeta();
            
            NamedTextColor nameColor = isDefault ? NamedTextColor.GOLD : NamedTextColor.YELLOW;
            meta.displayName(Component.text(preset.displayName, nameColor));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("预设ID: " + presetName, NamedTextColor.GRAY));
            lore.add(Component.text("物品数量: " + preset.items.size(), NamedTextColor.GRAY));
            if (isDefault) {
                lore.add(Component.text("§6当前默认预设", NamedTextColor.GOLD));
            }
            lore.add(Component.text("", NamedTextColor.GRAY));
            lore.add(Component.text("左键: 设为默认", NamedTextColor.GREEN));
            lore.add(Component.text("右键: 编辑预设", NamedTextColor.AQUA));
            lore.add(Component.text("Shift+右键: 删除预设", NamedTextColor.RED));
            
            meta.lore(lore);
            presetItem.setItemMeta(meta);
            
            items.add(new GuiItem(presetItem, event -> {
                if (event.isLeftClick()) {
                    // 设为默认
                    presetManager.setDefaultPreset(presetName);
                    plugin.getLootManager().setCurrentPreset(presetName);
                    plugin.getLanguageManager().send(player, "exfil.loot.preset.set_default", 
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("name", preset.displayName));
                    open(player); // 刷新
                } else if (event.isRightClick()) {
                    if (event.isShiftClick()) {
                        // 删除预设
                        if (presetManager.deletePreset(presetName)) {
                            plugin.getLanguageManager().send(player, "exfil.loot.preset.deleted", 
                                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("name", preset.displayName));
                            open(player); // 刷新
                        } else {
                            plugin.getLanguageManager().send(player, "exfil.loot.preset.cannot_delete");
                        }
                    } else {
                        // 编辑预设（打开战利品编辑器）
                        gui.getInventory().close();
                        // 切换到该预设并打开编辑器
                        plugin.getLootManager().setCurrentPreset(presetName);
                        plugin.getLootManager().openEditor(player);
                    }
                }
            }));
        }
        
        pages.populateWithGuiItems(items);
        gui.addPane(pages);
        
        // 底部导航和操作
        StaticPane navPane = new StaticPane(0, 5, 9, 1);
        
        // 上一页
        ItemStack prevItem = new ItemStack(Material.ARROW);
        ItemMeta prevMeta = prevItem.getItemMeta();
        prevMeta.displayName(Component.text("上一页", NamedTextColor.GRAY));
        prevItem.setItemMeta(prevMeta);
        navPane.addItem(new GuiItem(prevItem, event -> {
            if (pages.getPage() > 0) {
                pages.setPage(pages.getPage() - 1);
                gui.update();
            }
        }), 0, 0);
        
        // 创建新预设
        ItemStack createItem = new ItemStack(Material.ANVIL);
        ItemMeta createMeta = createItem.getItemMeta();
        createMeta.displayName(Component.text("创建新预设", NamedTextColor.GREEN));
        createItem.setItemMeta(createMeta);
        navPane.addItem(new GuiItem(createItem, event -> {
            gui.getInventory().close();
            // 提示输入预设名称
            plugin.getLanguageManager().send(player, "exfil.loot.preset.enter_name");
            // 这里可以添加聊天监听器来接收输入
            // 简化版：直接创建默认名称的预设
            String newName = "preset_" + System.currentTimeMillis();
            if (presetManager.createPreset(newName, "新预设")) {
                plugin.getLanguageManager().send(player, "exfil.loot.preset.created", 
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("name", newName));
                open(player);
            }
        }), 4, 0);
        
        // 下一页
        ItemStack nextItem = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = nextItem.getItemMeta();
        nextMeta.displayName(Component.text("下一页", NamedTextColor.GRAY));
        nextItem.setItemMeta(nextMeta);
        navPane.addItem(new GuiItem(nextItem, event -> {
            if (pages.getPage() < pages.getPages() - 1) {
                pages.setPage(pages.getPage() + 1);
                gui.update();
            }
        }), 8, 0);
        
        gui.addPane(navPane);
        gui.show(player);
    }
}

