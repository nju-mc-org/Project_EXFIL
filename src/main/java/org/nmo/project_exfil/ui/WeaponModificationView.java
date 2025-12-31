package org.nmo.project_exfil.ui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import me.zombie_striker.qg.api.QualityArmory;
import me.zombie_striker.qg.guns.Gun;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.manager.WeaponModificationManager;
import org.nmo.project_exfil.ui.framework.UIHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 武器改装界面
 */
public class WeaponModificationView {
    
    private final ProjectEXFILPlugin plugin;
    private final WeaponModificationManager modManager;
    
    public WeaponModificationView(ProjectEXFILPlugin plugin, WeaponModificationManager modManager) {
        this.plugin = plugin;
        this.modManager = modManager;
    }
    
    /**
     * 打开武器改装界面
     */
    public void open(Player player) {
        // 检查是否持有武器
        if (!modManager.isHoldingGun(player)) {
            plugin.getLanguageManager().send(player, "exfil.weapon.not_holding_gun");
            return;
        }
        
        Gun gun = modManager.getGunInHand(player);
        if (gun == null) {
            plugin.getLanguageManager().send(player, "exfil.weapon.invalid_gun");
            return;
        }
        
        ItemStack gunItem = player.getInventory().getItemInMainHand();
        if (!QualityArmory.isGun(gunItem)) {
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (QualityArmory.isGun(offHand)) {
                gunItem = offHand;
            }
        }
        
        final ItemStack finalGunItem = gunItem;
        
        ChestGui gui = new ChestGui(6, "武器改装 - " + gun.getDisplayName());
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        
        StaticPane pane = new StaticPane(0, 0, 9, 6);
        
        // 显示当前武器信息
        ItemStack weaponDisplay = finalGunItem.clone();
        pane.addItem(new GuiItem(weaponDisplay), 4, 0);
        
        // 获取已安装的配件
        Map<WeaponModificationManager.AttachmentType, WeaponModificationManager.Attachment> installed = 
            modManager.getInstalledAttachments(finalGunItem);
        
        // 显示配件槽位
        int slotX = 1;
        int slotY = 2;
        
        for (WeaponModificationManager.AttachmentType type : WeaponModificationManager.AttachmentType.values()) {
            WeaponModificationManager.Attachment installedAttachment = installed.get(type);
            
            ItemStack slotItem;
            if (installedAttachment != null) {
                // 已安装配件
                slotItem = new ItemStack(installedAttachment.getType().getIcon());
                org.bukkit.inventory.meta.ItemMeta meta = slotItem.getItemMeta();
                meta.displayName(Component.text(installedAttachment.getDisplayName(), NamedTextColor.GREEN));
                
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("类型: " + installedAttachment.getType().getDisplayName(), NamedTextColor.GRAY));
                lore.add(Component.text("", NamedTextColor.GRAY));
                lore.add(Component.text("属性加成:", NamedTextColor.YELLOW));
                for (Map.Entry<String, Double> stat : installedAttachment.getStats().entrySet()) {
                    String statName = getStatDisplayName(stat.getKey());
                    double value = stat.getValue();
                    String sign = value > 0 ? "+" : "";
                    NamedTextColor color = value > 0 ? NamedTextColor.GREEN : NamedTextColor.RED;
                    lore.add(Component.text(sign + String.format("%.0f%%", value * 100), color)
                        .append(Component.text(" " + statName, NamedTextColor.GRAY)));
                }
                lore.add(Component.text("", NamedTextColor.GRAY));
                lore.add(Component.text("右键点击卸载", NamedTextColor.RED));
                meta.lore(lore);
                slotItem.setItemMeta(meta);
                
                pane.addItem(new GuiItem(slotItem, event -> {
                    if (event.isRightClick()) {
                        UIHelper.playClickSound(player);
                        modManager.removeAttachment(player, finalGunItem, type);
                        open(player); // 刷新界面
                    }
                }), slotX, slotY);
            } else {
                // 空槽位
                slotItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                org.bukkit.inventory.meta.ItemMeta meta = slotItem.getItemMeta();
                meta.displayName(Component.text(type.getDisplayName() + "槽位", NamedTextColor.GRAY));
                meta.lore(List.of(
                    Component.text("空", NamedTextColor.DARK_GRAY),
                    Component.text("左键点击安装配件", NamedTextColor.YELLOW)
                ));
                slotItem.setItemMeta(meta);
                
                pane.addItem(new GuiItem(slotItem, event -> {
                    if (event.isLeftClick()) {
                        UIHelper.playClickSound(player);
                        openAttachmentSelection(player, finalGunItem, type);
                    }
                }), slotX, slotY);
            }
            
            slotX++;
            if (slotX > 7) {
                slotX = 1;
                slotY++;
            }
        }
        
        // 返回按钮
        pane.addItem(UIHelper.createBackButton(), 0, 5);
        
        gui.addPane(pane);
        gui.show(player);
    }
    
    /**
     * 打开配件选择界面
     */
    private void openAttachmentSelection(Player player, final ItemStack gunItem, WeaponModificationManager.AttachmentType type) {
        ChestGui gui = new ChestGui(6, "选择" + type.getDisplayName());
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        
        StaticPane pane = new StaticPane(0, 0, 9, 6);
        
        List<WeaponModificationManager.Attachment> attachments = modManager.getAvailableAttachments(type);
        
        int slot = 0;
        for (WeaponModificationManager.Attachment attachment : attachments) {
            ItemStack item = new ItemStack(attachment.getType().getIcon());
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(attachment.getDisplayName(), NamedTextColor.YELLOW));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("类型: " + attachment.getType().getDisplayName(), NamedTextColor.GRAY));
            lore.add(Component.text("", NamedTextColor.GRAY));
            lore.add(Component.text("属性加成:", NamedTextColor.YELLOW));
            for (Map.Entry<String, Double> stat : attachment.getStats().entrySet()) {
                String statName = getStatDisplayName(stat.getKey());
                double value = stat.getValue();
                String sign = value > 0 ? "+" : "";
                NamedTextColor color = value > 0 ? NamedTextColor.GREEN : NamedTextColor.RED;
                lore.add(Component.text(sign + String.format("%.0f%%", value * 100), color)
                    .append(Component.text(" " + statName, NamedTextColor.GRAY)));
            }
            lore.add(Component.text("", NamedTextColor.GRAY));
            lore.add(Component.text("左键点击安装", NamedTextColor.GREEN));
            meta.lore(lore);
            item.setItemMeta(meta);
            
            final ItemStack finalGunItemForSelection = gunItem;
            pane.addItem(new GuiItem(item, event -> {
                UIHelper.playClickSound(player);
                if (modManager.installAttachment(player, finalGunItemForSelection, attachment)) {
                    open(player); // 返回主界面
                }
            }), slot % 9, slot / 9);
            
            slot++;
            if (slot >= 45) break;
        }
        
        // 返回按钮
        pane.addItem(UIHelper.createBackButton(p -> open(player)), 0, 5);
        
        gui.addPane(pane);
        gui.show(player);
    }
    
    /**
     * 获取属性显示名称
     */
    private String getStatDisplayName(String key) {
        switch (key) {
            case "damage": return "伤害";
            case "recoil": return "后坐力";
            case "accuracy": return "精度";
            case "range": return "射程";
            case "sound": return "声音";
            case "capacity": return "容量";
            default: return key;
        }
    }
}

