package org.nmo.project_exfil.ui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nmo.project_exfil.manager.AchievementManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 成就界面
 */
public class AchievementView {
    
    private final AchievementManager achievementManager;
    
    public AchievementView(AchievementManager achievementManager) {
        this.achievementManager = achievementManager;
    }
    
    public void open(Player player) {
        ChestGui gui = new ChestGui(6, "成就系统");
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        
        StaticPane pane = new StaticPane(0, 0, 9, 6);
        
        // 显示所有成就
        Set<String> completed = achievementManager.getPlayerAchievements(player);
        List<AchievementManager.Achievement> achievements = new ArrayList<>(achievementManager.getAllAchievements());
        
        int slot = 0;
        for (AchievementManager.Achievement achievement : achievements) {
            if (slot >= 45) break;
            
            boolean isCompleted = completed.contains(achievement.id);
            ItemStack achievementItem = createAchievementItem(achievement, player, isCompleted);
            pane.addItem(new GuiItem(achievementItem), slot % 9, slot / 9);
            slot++;
        }
        
        gui.addPane(pane);
        gui.show(player);
    }
    
    private ItemStack createAchievementItem(AchievementManager.Achievement achievement, Player player, boolean completed) {
        Material material = completed ? Material.GOLD_BLOCK : Material.GRAY_CONCRETE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        NamedTextColor nameColor = completed ? NamedTextColor.GOLD : NamedTextColor.GRAY;
        meta.displayName(Component.text(achievement.name, nameColor));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(achievement.description, NamedTextColor.GRAY));
        
        if (completed) {
            lore.add(Component.text("✓ 已完成", NamedTextColor.GREEN));
        } else {
            int progress = achievementManager.getPlayerProgress(player, achievement.id);
            lore.add(Component.text("进度: " + progress + "/" + achievement.targetAmount, NamedTextColor.YELLOW));
        }
        
        if (achievement.moneyReward > 0) {
            lore.add(Component.text("奖励: $" + String.format("%.2f", achievement.moneyReward), NamedTextColor.GOLD));
        }
        if (!achievement.itemRewards.isEmpty()) {
            lore.add(Component.text("物品奖励: " + achievement.itemRewards.size() + " 件", NamedTextColor.AQUA));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
}

