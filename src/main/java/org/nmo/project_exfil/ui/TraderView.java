package org.nmo.project_exfil.ui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nmo.project_exfil.manager.TraderManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

/**
 * 特勤处交易界面
 */
public class TraderView {
    
    private final TraderManager traderManager;
    
    public TraderView(TraderManager traderManager) {
        this.traderManager = traderManager;
    }
    
    public void open(Player player) {
        ChestGui gui = new ChestGui(6, "特勤处 - 交易与回收");
        gui.setOnGlobalClick(event -> {
            event.setCancelled(true);
            // 处理回收逻辑
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                ItemStack item = event.getCurrentItem();
                if (traderManager.recycleItem(player, item)) {
                    // 更新界面
                    open(player);
                }
            }
        });
        
        StaticPane pane = new StaticPane(0, 0, 9, 6);
        
        // 回收区域说明
        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.displayName(Component.text("回收说明", NamedTextColor.YELLOW));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("将物品拖拽到此界面即可回收", NamedTextColor.GRAY));
        lore.add(Component.text("回收价格根据物品类型而定", NamedTextColor.GRAY));
        infoMeta.lore(lore);
        infoItem.setItemMeta(infoMeta);
        
        pane.addItem(new GuiItem(infoItem), 4, 0);
        
        // 玩家统计信息
        TraderManager.TraderStats stats = traderManager.getPlayerStats(player);
        ItemStack statsItem = new ItemStack(Material.PAPER);
        ItemMeta statsMeta = statsItem.getItemMeta();
        statsMeta.displayName(Component.text("交易统计", NamedTextColor.AQUA));
        List<Component> statsLore = new ArrayList<>();
        statsLore.add(Component.text("已回收物品: " + stats.itemsRecycled, NamedTextColor.GRAY));
        statsLore.add(Component.text("回收总价值: $" + String.format("%.2f", stats.totalRecycled), NamedTextColor.GRAY));
        statsMeta.lore(statsLore);
        statsItem.setItemMeta(statsMeta);
        
        pane.addItem(new GuiItem(statsItem), 4, 5);
        
        gui.addPane(pane);
        gui.show(player);
    }
}

