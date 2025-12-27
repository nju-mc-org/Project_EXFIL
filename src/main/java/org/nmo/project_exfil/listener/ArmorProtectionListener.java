package org.nmo.project_exfil.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.manager.GameManager;

/**
 * 护甲防弹系统监听器
 * 处理原版盔甲抵挡子弹伤害并掉耐久
 */
public class ArmorProtectionListener implements Listener {
    
    private final ProjectEXFILPlugin plugin;
    private final GameManager gameManager;
    
    // 护甲减伤配置（从配置文件加载）
    private double[] armorDamageReduction = {
        0.0,    // 无护甲
        0.08,   // 皮革 (8%)
        0.12,   // 金 (12%)
        0.15,   // 链甲 (15%)
        0.20,   // 铁 (20%)
        0.24,   // 钻石 (24%)
        0.30    // 下界合金 (30%)
    };
    
    // 护甲耐久消耗倍率（从配置文件加载）
    private double armorDurabilityDamageMultiplier = 2.0;
    
    private boolean enabled = true;
    
    public ArmorProtectionListener(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
        loadConfig();
    }
    
    /**
     * 从配置文件加载设置
     */
    private void loadConfig() {
        org.bukkit.configuration.file.YamlConfiguration config = 
            (org.bukkit.configuration.file.YamlConfiguration) plugin.getConfig();
        
        enabled = config.getBoolean("armor-protection.enabled", true);
        armorDurabilityDamageMultiplier = config.getDouble("armor-protection.durability-damage-multiplier", 2.0);
        
        // 加载护甲减伤配置
        org.bukkit.configuration.ConfigurationSection reductionSection = 
            config.getConfigurationSection("armor-protection.reduction");
        if (reductionSection != null) {
            armorDamageReduction[1] = reductionSection.getDouble("leather", 0.08);
            armorDamageReduction[2] = reductionSection.getDouble("gold", 0.12);
            armorDamageReduction[3] = reductionSection.getDouble("chainmail", 0.15);
            armorDamageReduction[4] = reductionSection.getDouble("iron", 0.20);
            armorDamageReduction[5] = reductionSection.getDouble("diamond", 0.24);
            armorDamageReduction[6] = reductionSection.getDouble("netherite", 0.30);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!enabled) return;
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        
        // 只处理游戏实例中的玩家
        if (gameManager.getPlayerInstance(player) == null) return;
        
        // 检查是否是子弹伤害（QualityArmory的伤害）
        if (isBulletDamage(event)) {
            handleBulletDamage(player, event);
        }
    }
    
    /**
     * 检查是否是子弹伤害
     */
    private boolean isBulletDamage(EntityDamageEvent event) {
        // QualityArmory通常使用PROJECTILE伤害类型
        if (event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
            // 进一步检查是否是QualityArmory的子弹
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                
                // 检查是否有QualityArmory的元数据
                if (e.getDamager().hasMetadata("qa_weapon") || 
                    e.getDamager().hasMetadata("QualityArmory")) {
                    return true;
                }
                
                // 检查伤害来源的名称（QualityArmory的子弹实体通常有特定名称）
                String entityType = e.getDamager().getType().name();
                if (entityType.contains("ARROW") || entityType.contains("SNOWBALL")) {
                    // 可能是QualityArmory的子弹，检查伤害值
                    double damage = event.getDamage();
                    // QualityArmory的伤害通常在合理范围内
                    if (damage > 0.5 && damage < 100.0) {
                        return true;
                    }
                }
            } else {
                // 只有PROJECTILE类型，也认为是子弹（可能是其他枪械插件）
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 处理子弹伤害
     */
    private void handleBulletDamage(Player player, EntityDamageEvent event) {
        double originalDamage = event.getDamage();
        double totalReduction = 0.0;
        
        // 计算护甲减伤
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ItemStack piece : armor) {
            if (piece != null && !piece.getType().isAir()) {
                double reduction = getArmorReduction(piece.getType());
                totalReduction += reduction;
                
                // 消耗护甲耐久
                damageArmor(piece, originalDamage * armorDurabilityDamageMultiplier);
            }
        }
        
        // 限制总减伤不超过80%
        totalReduction = Math.min(totalReduction, 0.80);
        
        // 应用减伤
        double newDamage = originalDamage * (1.0 - totalReduction);
        event.setDamage(newDamage);
        
        // 如果减伤超过50%，给玩家一个提示（可选）
        if (totalReduction > 0.5) {
            player.sendActionBar(
                net.kyori.adventure.text.Component.text(
                    String.format("§a护甲抵挡了 %.0f%% 伤害", totalReduction * 100)
                )
            );
        }
    }
    
    /**
     * 获取护甲的减伤百分比
     */
    private double getArmorReduction(Material armorType) {
        if (armorType.name().contains("LEATHER")) {
            return armorDamageReduction[1];
        } else if (armorType.name().contains("GOLD")) {
            return armorDamageReduction[2];
        } else if (armorType.name().contains("CHAINMAIL")) {
            return armorDamageReduction[3];
        } else if (armorType.name().contains("IRON")) {
            return armorDamageReduction[4];
        } else if (armorType.name().contains("DIAMOND")) {
            return armorDamageReduction[5];
        } else if (armorType.name().contains("NETHERITE")) {
            return armorDamageReduction[6];
        }
        return armorDamageReduction[0];
    }
    
    /**
     * 消耗护甲耐久
     */
    private void damageArmor(ItemStack armor, double damage) {
        if (armor == null || armor.getType().isAir()) return;
        
        ItemMeta meta = armor.getItemMeta();
        if (!(meta instanceof Damageable)) return;
        
        Damageable damageable = (Damageable) meta;
        
        // 计算耐久消耗（基于伤害值，已经乘以倍率）
        int durabilityDamage = (int) damage;
        
        // 获取当前耐久
        int currentDurability = damageable.getDamage();
        int maxDurability = armor.getType().getMaxDurability();
        
        // 增加耐久值（耐久值越高，物品越破）
        int newDurability = Math.min(currentDurability + durabilityDamage, maxDurability);
        damageable.setDamage(newDurability);
        
        // 如果耐久耗尽，移除物品
        if (newDurability >= maxDurability) {
            armor.setAmount(0); // 使用setAmount(0)代替setType(Material.AIR)
        } else {
            armor.setItemMeta(meta);
        }
    }
}

