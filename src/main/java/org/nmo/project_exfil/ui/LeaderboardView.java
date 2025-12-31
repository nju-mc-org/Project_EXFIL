package org.nmo.project_exfil.ui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.nmo.project_exfil.manager.LeaderboardManager;
import org.nmo.project_exfil.ui.framework.UIHelper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

/**
 * 排行榜界面
 */
public class LeaderboardView {
    
    private final LeaderboardManager leaderboardManager;
    
    public LeaderboardView(LeaderboardManager leaderboardManager) {
        this.leaderboardManager = leaderboardManager;
    }
    
    public void open(Player player) {
        open(player, LeaderboardManager.LeaderboardType.KILLS);
    }
    
    public void open(Player player, LeaderboardManager.LeaderboardType type) {
        ChestGui gui = new ChestGui(6, "排行榜 - " + getTypeName(type));
        gui.setOnGlobalClick(event -> {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player)) return;
            Player clickPlayer = (Player) event.getWhoClicked();
            
            // 切换排行榜类型
            int slot = event.getSlot();
            if (slot == 45) {
                UIHelper.playClickSound(clickPlayer);
                open(clickPlayer, LeaderboardManager.LeaderboardType.KILLS);
            } else if (slot == 46) {
                UIHelper.playClickSound(clickPlayer);
                open(clickPlayer, LeaderboardManager.LeaderboardType.EXTRACTS);
            } else if (slot == 47) {
                UIHelper.playClickSound(clickPlayer);
                open(clickPlayer, LeaderboardManager.LeaderboardType.VALUE);
            } else if (slot == 48) {
                UIHelper.playClickSound(clickPlayer);
                open(clickPlayer, LeaderboardManager.LeaderboardType.PLAY_TIME);
            }
        });
        
        StaticPane pane = new StaticPane(0, 0, 9, 6);
        
        // 显示排行榜
        List<LeaderboardManager.LeaderboardEntry> entries = leaderboardManager.getLeaderboard(type);
        for (int i = 0; i < Math.min(10, entries.size()); i++) {
            LeaderboardManager.LeaderboardEntry entry = entries.get(i);
            ItemStack item = createLeaderboardItem(entry, i + 1, type);
            pane.addItem(new GuiItem(item), i % 9, i / 9);
        }
        
        // 底部按钮 - 切换排行榜类型
        StaticPane buttonPane = new StaticPane(0, 5, 9, 1);
        
        // 击杀数
        ItemStack killsItem = new ItemStack(Material.IRON_SWORD);
        ItemMeta killsMeta = killsItem.getItemMeta();
        killsMeta.displayName(Component.text("击杀数", 
            type == LeaderboardManager.LeaderboardType.KILLS ? NamedTextColor.GOLD : NamedTextColor.GRAY));
        killsItem.setItemMeta(killsMeta);
        buttonPane.addItem(new GuiItem(killsItem), 0, 0);
        
        // 撤离次数
        ItemStack extractsItem = new ItemStack(Material.EMERALD);
        ItemMeta extractsMeta = extractsItem.getItemMeta();
        extractsMeta.displayName(Component.text("撤离次数", 
            type == LeaderboardManager.LeaderboardType.EXTRACTS ? NamedTextColor.GOLD : NamedTextColor.GRAY));
        extractsItem.setItemMeta(extractsMeta);
        buttonPane.addItem(new GuiItem(extractsItem), 1, 0);
        
        // 总价值
        ItemStack valueItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta valueMeta = valueItem.getItemMeta();
        valueMeta.displayName(Component.text("总价值", 
            type == LeaderboardManager.LeaderboardType.VALUE ? NamedTextColor.GOLD : NamedTextColor.GRAY));
        valueItem.setItemMeta(valueMeta);
        buttonPane.addItem(new GuiItem(valueItem), 2, 0);
        
        // 游戏时间
        ItemStack timeItem = new ItemStack(Material.CLOCK);
        ItemMeta timeMeta = timeItem.getItemMeta();
        timeMeta.displayName(Component.text("游戏时间", 
            type == LeaderboardManager.LeaderboardType.PLAY_TIME ? NamedTextColor.GOLD : NamedTextColor.GRAY));
        timeItem.setItemMeta(timeMeta);
        buttonPane.addItem(new GuiItem(timeItem), 3, 0);
        
        // 返回按钮
        buttonPane.addItem(UIHelper.createBackButton(), 8, 0);
        
        gui.addPane(pane);
        gui.addPane(buttonPane);
        gui.show(player);
    }
    
    private ItemStack createLeaderboardItem(LeaderboardManager.LeaderboardEntry entry, int rank, 
                                           LeaderboardManager.LeaderboardType type) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.uuid);
        if (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline()) {
            meta.setOwningPlayer(offlinePlayer);
        }
        
        NamedTextColor rankColor = rank <= 3 ? NamedTextColor.GOLD : NamedTextColor.YELLOW;
        meta.displayName(Component.text("#" + rank + " " + offlinePlayer.getName(), rankColor));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(getTypeName(type) + ": " + formatValue(entry.value, type), NamedTextColor.GRAY));
        
        // 显示玩家排名
        Player onlinePlayer = Bukkit.getPlayer(entry.uuid);
        if (onlinePlayer != null) {
            int playerRank = leaderboardManager.getPlayerRank(onlinePlayer, type);
            if (playerRank > 0) {
                lore.add(Component.text("你的排名: #" + playerRank, NamedTextColor.AQUA));
            }
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private String getTypeName(LeaderboardManager.LeaderboardType type) {
        switch (type) {
            case KILLS:
                return "击杀数";
            case EXTRACTS:
                return "撤离次数";
            case VALUE:
                return "总价值";
            case PLAY_TIME:
                return "游戏时间";
            default:
                return "未知";
        }
    }
    
    private String formatValue(double value, LeaderboardManager.LeaderboardType type) {
        switch (type) {
            case KILLS:
            case EXTRACTS:
                return String.format("%.0f", value);
            case VALUE:
                return String.format("$%.2f", value);
            case PLAY_TIME:
                return String.format("%.1f 小时", value);
            default:
                return String.valueOf(value);
        }
    }
}

