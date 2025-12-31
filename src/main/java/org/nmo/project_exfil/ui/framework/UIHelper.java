package org.nmo.project_exfil.ui.framework;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.nmo.project_exfil.ProjectEXFILPlugin;

import java.util.function.Consumer;

/**
 * UI辅助工具类
 * 提供统一的UI组件创建和交互反馈
 */
public class UIHelper {
    
    /**
     * 创建统一的返回按钮
     * @param onClick 点击回调，如果为null则返回到主菜单
     * @return GuiItem 返回按钮
     */
    public static GuiItem createBackButton(Consumer<Player> onClick) {
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        ProjectEXFILPlugin plugin = ProjectEXFILPlugin.getPlugin();
        
        if (plugin != null) {
            backMeta.displayName(plugin.getLanguageManager().getMessage("exfil.team.back"));
        } else {
            backMeta.displayName(Component.text("返回", NamedTextColor.GRAY));
        }
        backItem.setItemMeta(backMeta);
        
        return new GuiItem(backItem, event -> {
            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();
            
            // 播放点击音效
            playClickSound(player);
            
            if (onClick != null) {
                onClick.accept(player);
            } else {
                // 默认返回到主菜单
                if (plugin != null && plugin.getMainMenuView() != null) {
                    event.getInventory().close();
                    plugin.getMainMenuView().open(player);
                } else {
                    player.closeInventory();
                }
            }
        });
    }
    
    /**
     * 创建返回按钮（返回到主菜单）
     * @return GuiItem 返回按钮
     */
    public static GuiItem createBackButton() {
        return createBackButton(null);
    }
    
    /**
     * 播放UI点击音效
     * @param player 玩家
     */
    public static void playClickSound(@NotNull Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }
    
    /**
     * 播放UI成功音效
     * @param player 玩家
     */
    public static void playSuccessSound(@NotNull Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.3f, 1.5f);
    }
    
    /**
     * 播放UI错误音效
     * @param player 玩家
     */
    public static void playErrorSound(@NotNull Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
    }
    
    /**
     * 创建填充物品（用于UI布局）
     * @return GuiItem 填充物品
     */
    public static GuiItem createFiller() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.text(" "));
        filler.setItemMeta(fillerMeta);
        return new GuiItem(filler, event -> event.setCancelled(true));
    }
}

