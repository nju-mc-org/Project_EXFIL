package org.nmo.project_exfil.manager;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.nmo.project_exfil.ProjectEXFILPlugin;

import java.util.*;

/**
 * 成就管理器
 */
public class AchievementManager {
    
    private final ProjectEXFILPlugin plugin;
    // 玩家已完成的成就 (玩家UUID -> 成就ID集合)
    private final Map<UUID, Set<String>> playerAchievements = new HashMap<>();
    // 所有可用成就
    private final Map<String, Achievement> achievements = new HashMap<>();
    // 玩家成就进度 (玩家UUID -> 成就ID -> 进度)
    private final Map<UUID, Map<String, Integer>> playerProgress = new HashMap<>();
    
    public AchievementManager(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
        initializeDefaultAchievements();
    }
    
    /**
     * 初始化默认成就
     */
    private void initializeDefaultAchievements() {
        // 击杀类成就
        registerAchievement(new Achievement("first_kill", "首杀", 
            "击杀第一名玩家", AchievementType.KILL_PLAYER, 1, 
            500.0, Arrays.asList(new ItemStack(org.bukkit.Material.IRON_SWORD))));
        
        registerAchievement(new Achievement("killer", "杀手", 
            "累计击杀10名玩家", AchievementType.KILL_PLAYER, 10, 
            2000.0, Arrays.asList(new ItemStack(org.bukkit.Material.DIAMOND_SWORD))));
        
        // 撤离类成就
        registerAchievement(new Achievement("first_extract", "首次撤离", 
            "完成第一次撤离", AchievementType.EXTRACT, 1, 
            1000.0, Arrays.asList(new ItemStack(org.bukkit.Material.EMERALD, 5))));
        
        registerAchievement(new Achievement("veteran", "老兵", 
            "累计完成50次撤离", AchievementType.EXTRACT, 50, 
            10000.0, Arrays.asList(new ItemStack(org.bukkit.Material.NETHERITE_INGOT))));
        
        // 收集类成就
        registerAchievement(new Achievement("collector", "收藏家", 
            "累计收集1000件物品", AchievementType.COLLECT_ITEMS, 1000, 
            5000.0, Arrays.asList(new ItemStack(org.bukkit.Material.CHEST))));
        
        // 生存类成就
        registerAchievement(new Achievement("survivor", "生存者", 
            "在单次行动中存活20分钟", AchievementType.SURVIVE_TIME, 20, 
            3000.0, Arrays.asList(new ItemStack(org.bukkit.Material.GOLDEN_APPLE, 5))));
    }
    
    /**
     * 注册成就
     */
    public void registerAchievement(Achievement achievement) {
        achievements.put(achievement.id, achievement);
    }
    
    /**
     * 更新成就进度
     * @param player 玩家
     * @param type 成就类型
     * @param amount 增加的数量
     */
    public void updateProgress(Player player, AchievementType type, int amount) {
        UUID uuid = player.getUniqueId();
        Map<String, Integer> progress = playerProgress.computeIfAbsent(uuid, k -> new HashMap<>());
        Set<String> completed = playerAchievements.computeIfAbsent(uuid, k -> new HashSet<>());
        
        for (Achievement achievement : achievements.values()) {
            if (achievement.type == type && !completed.contains(achievement.id)) {
                int current = progress.getOrDefault(achievement.id, 0);
                current += amount;
                progress.put(achievement.id, current);
                
                if (current >= achievement.targetAmount) {
                    completeAchievement(player, achievement);
                }
            }
        }
    }
    
    /**
     * 完成成就
     */
    private void completeAchievement(Player player, Achievement achievement) {
        UUID uuid = player.getUniqueId();
        playerAchievements.computeIfAbsent(uuid, k -> new HashSet<>()).add(achievement.id);
        
        // 给予奖励
        if (achievement.moneyReward > 0) {
            org.nmo.project_exfil.util.DependencyHelper.depositMoney(player, achievement.moneyReward);
        }
        
        for (ItemStack item : achievement.itemRewards) {
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(item);
            } else {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }
        
        // 发送通知
        plugin.getLanguageManager().send(player, "exfil.achievement.unlocked", 
            net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("name", achievement.name));
        
        // 全服公告（可选）
        org.bukkit.Bukkit.broadcast(
            net.kyori.adventure.text.Component.text(player.getName() + " 解锁了成就: " + achievement.name)
                .color(net.kyori.adventure.text.format.NamedTextColor.GOLD)
        );
    }
    
    /**
     * 获取玩家已完成的成就
     */
    public Set<String> getPlayerAchievements(Player player) {
        return playerAchievements.getOrDefault(player.getUniqueId(), new HashSet<>());
    }
    
    /**
     * 获取玩家成就进度
     */
    public int getPlayerProgress(Player player, String achievementId) {
        return playerProgress.getOrDefault(player.getUniqueId(), new HashMap<>())
            .getOrDefault(achievementId, 0);
    }
    
    /**
     * 获取所有成就
     */
    public Collection<Achievement> getAllAchievements() {
        return achievements.values();
    }
    
    /**
     * 获取成就
     */
    public Achievement getAchievement(String id) {
        return achievements.get(id);
    }
    
    /**
     * 检查玩家是否已完成成就
     */
    public boolean hasAchievement(Player player, String achievementId) {
        return playerAchievements.getOrDefault(player.getUniqueId(), new HashSet<>())
            .contains(achievementId);
    }
    
    /**
     * 成就类
     */
    public static class Achievement {
        public final String id;
        public final String name;
        public final String description;
        public final AchievementType type;
        public final int targetAmount;
        public final double moneyReward;
        public final List<ItemStack> itemRewards;
        
        public Achievement(String id, String name, String description, AchievementType type, 
                          int targetAmount, double moneyReward, List<ItemStack> itemRewards) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.type = type;
            this.targetAmount = targetAmount;
            this.moneyReward = moneyReward;
            this.itemRewards = itemRewards;
        }
    }
    
    /**
     * 成就类型
     */
    public enum AchievementType {
        KILL_PLAYER,    // 击杀玩家
        KILL_NPC,       // 击杀NPC
        EXTRACT,        // 撤离
        COLLECT_ITEMS,  // 收集物品
        SURVIVE_TIME,   // 生存时间
        DEAL_DAMAGE,    // 造成伤害
        TAKE_DAMAGE     // 承受伤害
    }
}

