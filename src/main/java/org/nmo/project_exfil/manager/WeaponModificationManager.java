package org.nmo.project_exfil.manager;

import me.zombie_striker.qg.api.QualityArmory;
import me.zombie_striker.qg.guns.Gun;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.nmo.project_exfil.ProjectEXFILPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 武器改装管理器
 * 基于QualityArmory API实现武器配件系统
 */
public class WeaponModificationManager {
    
    private final ProjectEXFILPlugin plugin;
    
    // 配件类型枚举
    public enum AttachmentType {
        SIGHT("瞄具", Material.ENDER_EYE),
        MUZZLE("枪口", Material.IRON_INGOT),
        GRIP("握把", Material.STICK),
        STOCK("枪托", Material.LEATHER),
        MAGAZINE("弹匣", Material.PAPER);
        
        private final String displayName;
        private final Material icon;
        
        AttachmentType(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public Material getIcon() {
            return icon;
        }
    }
    
    // 配件数据类
    public static class Attachment {
        private final String id;
        private final String displayName;
        private final AttachmentType type;
        private final Map<String, Double> stats; // 属性加成
        
        public Attachment(String id, String displayName, AttachmentType type, Map<String, Double> stats) {
            this.id = id;
            this.displayName = displayName;
            this.type = type;
            this.stats = stats != null ? stats : new HashMap<>();
        }
        
        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public AttachmentType getType() { return type; }
        public Map<String, Double> getStats() { return stats; }
    }
    
    // 预设配件列表（可以从配置文件加载）
    private final Map<String, Attachment> availableAttachments = new HashMap<>();
    
    public WeaponModificationManager(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
        initializeDefaultAttachments();
    }
    
    /**
     * 初始化默认配件
     */
    private void initializeDefaultAttachments() {
        // 瞄具
        Map<String, Double> redDotStats = new HashMap<>();
        redDotStats.put("accuracy", 0.1); // 精度+10%
        redDotStats.put("recoil", -0.05); // 后坐力-5%
        availableAttachments.put("red_dot", new Attachment("red_dot", "红点瞄具", AttachmentType.SIGHT, redDotStats));
        
        Map<String, Double> scopeStats = new HashMap<>();
        scopeStats.put("accuracy", 0.2); // 精度+20%
        scopeStats.put("recoil", -0.1); // 后坐力-10%
        scopeStats.put("range", 0.3); // 射程+30%
        availableAttachments.put("scope_4x", new Attachment("scope_4x", "4倍镜", AttachmentType.SIGHT, scopeStats));
        
        // 枪口
        Map<String, Double> suppressorStats = new HashMap<>();
        suppressorStats.put("sound", -0.8); // 声音-80%
        suppressorStats.put("damage", -0.1); // 伤害-10%
        availableAttachments.put("suppressor", new Attachment("suppressor", "消音器", AttachmentType.MUZZLE, suppressorStats));
        
        Map<String, Double> compensatorStats = new HashMap<>();
        compensatorStats.put("recoil", -0.15); // 后坐力-15%
        availableAttachments.put("compensator", new Attachment("compensator", "补偿器", AttachmentType.MUZZLE, compensatorStats));
        
        // 握把
        Map<String, Double> verticalGripStats = new HashMap<>();
        verticalGripStats.put("recoil", -0.1); // 后坐力-10%
        availableAttachments.put("vertical_grip", new Attachment("vertical_grip", "垂直握把", AttachmentType.GRIP, verticalGripStats));
        
        // 弹匣
        Map<String, Double> extendedMagStats = new HashMap<>();
        extendedMagStats.put("capacity", 0.5); // 容量+50%
        availableAttachments.put("extended_mag", new Attachment("extended_mag", "扩容弹匣", AttachmentType.MAGAZINE, extendedMagStats));
    }
    
    /**
     * 检查玩家手中的物品是否是QualityArmory武器
     */
    public boolean isHoldingGun(Player player) {
        try {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();
            
            return (mainHand != null && QualityArmory.isGun(mainHand)) ||
                   (offHand != null && QualityArmory.isGun(offHand));
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取玩家手中的武器
     */
    public Gun getGunInHand(Player player) {
        try {
            return QualityArmory.getGunInHand(player);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 获取武器的配件槽位信息
     * 返回当前已安装的配件
     */
    public Map<AttachmentType, Attachment> getInstalledAttachments(ItemStack gunItem) {
        Map<AttachmentType, Attachment> installed = new HashMap<>();
        
        if (!QualityArmory.isGun(gunItem)) {
            return installed;
        }
        
        // 从NBT数据中读取配件信息
        // 注意：这需要根据QualityArmory的实际NBT结构来实现
        // 这里提供一个基础框架
        
        return installed;
    }
    
    /**
     * 安装配件到武器
     */
    public boolean installAttachment(Player player, ItemStack gunItem, Attachment attachment) {
        if (!QualityArmory.isGun(gunItem)) {
            return false;
        }
        
        Gun gun = QualityArmory.getGun(gunItem);
        if (gun == null) {
            return false;
        }
        
        // 检查是否已有同类型配件
        Map<AttachmentType, Attachment> installed = getInstalledAttachments(gunItem);
        if (installed.containsKey(attachment.getType())) {
            plugin.getLanguageManager().send(player, "exfil.weapon.attachment.already_installed");
            return false;
        }
        
        // 安装配件（需要修改ItemStack的NBT数据）
        // 这里需要根据QualityArmory的实际实现来修改
        // 暂时返回成功
        
        plugin.getLanguageManager().send(player, "exfil.weapon.attachment.installed", 
            net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("name", attachment.getDisplayName()));
        return true;
    }
    
    /**
     * 卸载配件
     */
    public boolean removeAttachment(Player player, ItemStack gunItem, AttachmentType type) {
        if (!QualityArmory.isGun(gunItem)) {
            return false;
        }
        
        Map<AttachmentType, Attachment> installed = getInstalledAttachments(gunItem);
        if (!installed.containsKey(type)) {
            plugin.getLanguageManager().send(player, "exfil.weapon.attachment.not_installed");
            return false;
        }
        
        // 卸载配件
        // 需要修改ItemStack的NBT数据
        
        plugin.getLanguageManager().send(player, "exfil.weapon.attachment.removed");
        return true;
    }
    
    /**
     * 获取所有可用配件
     */
    public List<Attachment> getAvailableAttachments() {
        return new ArrayList<>(availableAttachments.values());
    }
    
    /**
     * 根据类型获取可用配件
     */
    public List<Attachment> getAvailableAttachments(AttachmentType type) {
        List<Attachment> result = new ArrayList<>();
        for (Attachment attachment : availableAttachments.values()) {
            if (attachment.getType() == type) {
                result.add(attachment);
            }
        }
        return result;
    }
    
    /**
     * 根据ID获取配件
     */
    public Attachment getAttachment(String id) {
        return availableAttachments.get(id);
    }
    
    /**
     * 计算武器的最终属性（基础属性 + 配件加成）
     */
    public Map<String, Double> calculateWeaponStats(Gun gun, Map<AttachmentType, Attachment> attachments) {
        Map<String, Double> stats = new HashMap<>();
        
        if (gun == null) {
            return stats;
        }
        
        // 基础属性
        stats.put("damage", (double) gun.getDamage());
        stats.put("recoil", 1.0); // 基础后坐力
        stats.put("accuracy", 1.0); // 基础精度
        stats.put("range", 1.0); // 基础射程
        
        // 应用配件加成
        for (Attachment attachment : attachments.values()) {
            for (Map.Entry<String, Double> entry : attachment.getStats().entrySet()) {
                String key = entry.getKey();
                Double value = entry.getValue();
                
                if (stats.containsKey(key)) {
                    stats.put(key, stats.get(key) + value);
                } else {
                    stats.put(key, value);
                }
            }
        }
        
        return stats;
    }
}

