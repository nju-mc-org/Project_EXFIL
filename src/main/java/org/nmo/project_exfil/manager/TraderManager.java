package org.nmo.project_exfil.manager;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.util.DependencyHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 特勤处管理器 - 处理交易和回收系统
 */
public class TraderManager {
    
    private final ProjectEXFILPlugin plugin;
    // 物品回收价格映射 (物品类型 -> 回收价格)
    private final Map<Material, Double> recyclePrices = new HashMap<>();
    // 玩家交易历史
    private final Map<UUID, TraderStats> playerStats = new HashMap<>();
    
    public TraderManager(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
        initializeRecyclePrices();
    }
    
    /**
     * 初始化回收价格表
     */
    private void initializeRecyclePrices() {
        // 示例：设置一些常见物品的回收价格
        recyclePrices.put(Material.IRON_INGOT, 50.0);
        recyclePrices.put(Material.GOLD_INGOT, 100.0);
        recyclePrices.put(Material.DIAMOND, 500.0);
        recyclePrices.put(Material.EMERALD, 300.0);
        // 可以根据需要添加更多物品
    }
    
    /**
     * 回收物品
     * @param player 玩家
     * @param item 要回收的物品
     * @return 回收是否成功
     */
    public boolean recycleItem(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        
        Material material = item.getType();
        Double price = recyclePrices.get(material);
        
        if (price == null) {
            plugin.getLanguageManager().send(player, "exfil.trader.item_not_recyclable");
            return false;
        }
        
        // 计算总价值（考虑数量）
        double totalValue = price * item.getAmount();
        
        // 从玩家背包移除物品
        item.setAmount(0);
        
        // 给予玩家货币
        if (DependencyHelper.isXConomyEnabled()) {
            DependencyHelper.depositMoney(player, totalValue);
        } else if (DependencyHelper.isVaultEnabled()) {
            DependencyHelper.depositMoney(player, totalValue);
        }
        
        // 更新统计
        TraderStats stats = playerStats.computeIfAbsent(player.getUniqueId(), k -> new TraderStats());
        stats.totalRecycled += totalValue;
        stats.itemsRecycled += item.getAmount();
        
        plugin.getLanguageManager().send(player, "exfil.trader.recycled", 
            net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("value", String.format("%.2f", totalValue)));
        
        return true;
    }
    
    /**
     * 获取物品的回收价格
     * @param material 物品材质
     * @return 回收价格，如果不可回收则返回null
     */
    public Double getRecyclePrice(Material material) {
        return recyclePrices.get(material);
    }
    
    /**
     * 设置物品的回收价格
     * @param material 物品材质
     * @param price 价格
     */
    public void setRecyclePrice(Material material, double price) {
        recyclePrices.put(material, price);
    }
    
    /**
     * 获取玩家的交易统计
     * @param player 玩家
     * @return 交易统计
     */
    public TraderStats getPlayerStats(Player player) {
        return playerStats.computeIfAbsent(player.getUniqueId(), k -> new TraderStats());
    }
    
    /**
     * 交易统计类
     */
    public static class TraderStats {
        public double totalRecycled = 0.0;
        public int itemsRecycled = 0;
        public int itemsBought = 0;
        public double totalSpent = 0.0;
    }
}

