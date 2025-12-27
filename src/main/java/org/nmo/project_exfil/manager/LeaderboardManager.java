package org.nmo.project_exfil.manager;

import org.bukkit.entity.Player;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.data.PlayerDataManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 排行榜管理器
 */
public class LeaderboardManager {
    
    private final ProjectEXFILPlugin plugin;
    private final PlayerDataManager dataManager;
    private final Map<LeaderboardType, List<LeaderboardEntry>> cache = new HashMap<>();
    private long lastUpdate = 0;
    private static final long CACHE_DURATION = 60_000; // 1分钟缓存
    
    public LeaderboardManager(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getPlayerDataManager();
        // 定期更新排行榜
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::updateAll, 0L, 1200L); // 每60秒
    }
    
    /**
     * 更新所有排行榜
     */
    private void updateAll() {
        updateLeaderboard(LeaderboardType.KILLS);
        updateLeaderboard(LeaderboardType.EXTRACTS);
        updateLeaderboard(LeaderboardType.VALUE);
        updateLeaderboard(LeaderboardType.PLAY_TIME);
        lastUpdate = System.currentTimeMillis();
    }
    
    /**
     * 更新特定排行榜
     */
    private void updateLeaderboard(LeaderboardType type) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        
        // 获取所有玩家数据文件
        java.io.File dataFolder = new java.io.File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            cache.put(type, entries);
            return;
        }
        
        java.io.File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            cache.put(type, entries);
            return;
        }
        
        for (java.io.File file : files) {
            try {
                String uuidStr = file.getName().replace(".yml", "");
                UUID uuid = UUID.fromString(uuidStr);
                PlayerDataManager.PlayerData data = dataManager.loadPlayerData(uuid);
                
                double value = 0.0;
                switch (type) {
                    case KILLS:
                        value = data.kills;
                        break;
                    case EXTRACTS:
                        value = data.extracts;
                        break;
                    case VALUE:
                        value = data.totalValue;
                        break;
                    case PLAY_TIME:
                        value = data.playTime / 3600.0; // 转换为小时
                        break;
                }
                
                if (value > 0) {
                    entries.add(new LeaderboardEntry(uuid, value));
                }
            } catch (Exception e) {
                // 忽略无效文件
            }
        }
        
        // 排序并取前10
        entries.sort((a, b) -> Double.compare(b.value, a.value));
        cache.put(type, entries.stream().limit(10).collect(Collectors.toList()));
    }
    
    /**
     * 获取排行榜
     */
    public List<LeaderboardEntry> getLeaderboard(LeaderboardType type) {
        // 如果缓存过期，更新
        if (System.currentTimeMillis() - lastUpdate > CACHE_DURATION) {
            updateLeaderboard(type);
        }
        return new ArrayList<>(cache.getOrDefault(type, new ArrayList<>()));
    }
    
    /**
     * 获取玩家排名
     */
    public int getPlayerRank(Player player, LeaderboardType type) {
        List<LeaderboardEntry> leaderboard = getLeaderboard(type);
        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).uuid.equals(player.getUniqueId())) {
                return i + 1;
            }
        }
        return -1; // 未上榜
    }
    
    /**
     * 排行榜类型
     */
    public enum LeaderboardType {
        KILLS,      // 击杀数
        EXTRACTS,   // 撤离次数
        VALUE,      // 总价值
        PLAY_TIME   // 游戏时间
    }
    
    /**
     * 排行榜条目
     */
    public static class LeaderboardEntry {
        public final UUID uuid;
        public final double value;
        
        public LeaderboardEntry(UUID uuid, double value) {
            this.uuid = uuid;
            this.value = value;
        }
    }
}

