package org.nmo.project_exfil.api;

import org.bukkit.entity.Player;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.data.PlayerDataManager;
import org.nmo.project_exfil.manager.*;

import java.util.UUID;

/**
 * Project EXFIL 公开API
 * 供外部插件和bot使用
 * 
 * 参考: 
 * - https://minecraft.wiki/w/Java_Edition_protocol/Packets (Minecraft协议文档)
 * - https://dmulloy2.net/ProtocolLib/javadoc/ (ProtocolLib文档)
 * 
 * 使用示例:
 * <pre>
 * ExfilAPI api = ExfilAPI.getInstance();
 * if (api.isPlayerInGame(player)) {
 *     ExfilAPI.PlayerStats stats = api.getPlayerStats(player);
 *     System.out.println("击杀: " + stats.kills);
 * }
 * </pre>
 */
public class ExfilAPI {
    
    private static ExfilAPI instance;
    private final ProjectEXFILPlugin plugin;
    
    private ExfilAPI(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 获取API实例
     * 这是访问API的主要入口点
     * 
     * @return ExfilAPI实例，如果插件未启用则返回null
     */
    public static ExfilAPI getInstance() {
        if (instance == null && ProjectEXFILPlugin.getPlugin() != null) {
            instance = new ExfilAPI(ProjectEXFILPlugin.getPlugin());
        }
        return instance;
    }
    
    /**
     * 检查玩家是否在游戏中
     */
    public boolean isPlayerInGame(Player player) {
        return plugin.getGameManager().getPlayerInstance(player) != null;
    }
    
    /**
     * 检查玩家是否在游戏中（通过UUID）
     */
    public boolean isPlayerInGame(UUID uuid) {
        Player player = plugin.getServer().getPlayer(uuid);
        return player != null && isPlayerInGame(player);
    }
    
    /**
     * 获取玩家当前所在的地图名称
     */
    public String getPlayerMap(Player player) {
        GameInstance instance = plugin.getGameManager().getPlayerInstance(player);
        if (instance == null) {
            return null;
        }
        return instance.getTemplateName();
    }
    
    /**
     * 获取玩家统计数据
     */
    public PlayerStats getPlayerStats(Player player) {
        if (plugin.getPlayerDataManager() == null) {
            return null;
        }
        
        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) {
            return null;
        }
        
        return new PlayerStats(
            data.kills,
            data.deaths,
            data.extracts,
            data.totalValue,
            data.playTime
        );
    }
    
    /**
     * 获取玩家任务完成情况
     */
    public java.util.Set<String> getCompletedTasks(Player player) {
        if (plugin.getTaskManager() == null) {
            return java.util.Collections.emptySet();
        }
        
        java.util.List<TaskManager.PlayerTask> tasks = plugin.getTaskManager().getPlayerTasks(player);
        java.util.Set<String> completed = new java.util.HashSet<>();
        for (TaskManager.PlayerTask task : tasks) {
            if (task.completed) {
                completed.add(task.template.name);
            }
        }
        return completed;
    }
    
    /**
     * 获取玩家成就完成情况
     */
    public java.util.Set<String> getCompletedAchievements(Player player) {
        if (plugin.getAchievementManager() == null) {
            return java.util.Collections.emptySet();
        }
        return plugin.getAchievementManager().getPlayerAchievements(player);
    }
    
    /**
     * 检查玩家是否在小队中
     */
    public boolean isPlayerInParty(Player player) {
        if (plugin.getPartyManager() == null || !plugin.getPartyManager().isEnabled()) {
            return false;
        }
        
        com.alessiodp.parties.api.interfaces.PartyPlayer partyPlayer = 
            plugin.getPartyManager().getPartyPlayer(player.getUniqueId());
        return partyPlayer != null && partyPlayer.isInParty();
    }
    
    /**
     * 获取玩家的小队成员列表
     */
    public java.util.List<UUID> getPartyMembers(Player player) {
        if (!isPlayerInParty(player)) {
            return java.util.Collections.emptyList();
        }
        
        com.alessiodp.parties.api.interfaces.PartyPlayer partyPlayer = 
            plugin.getPartyManager().getPartyPlayer(player.getUniqueId());
        if (partyPlayer == null) {
            return java.util.Collections.emptyList();
        }
        
        com.alessiodp.parties.api.interfaces.Party party = 
            plugin.getPartyManager().getParty(partyPlayer.getPartyId());
        if (party == null) {
            return java.util.Collections.emptyList();
        }
        
        return new java.util.ArrayList<>(party.getMembers());
    }
    
    /**
     * 获取排行榜数据
     */
    public java.util.List<LeaderboardEntry> getLeaderboard(LeaderboardManager.LeaderboardType type, int limit) {
        if (plugin.getLeaderboardManager() == null) {
            return java.util.Collections.emptyList();
        }
        
        java.util.List<LeaderboardManager.LeaderboardEntry> entries = 
            plugin.getLeaderboardManager().getLeaderboard(type);
        
        java.util.List<LeaderboardEntry> result = new java.util.ArrayList<>();
        for (int i = 0; i < Math.min(limit, entries.size()); i++) {
            LeaderboardManager.LeaderboardEntry entry = entries.get(i);
            result.add(new LeaderboardEntry(
                entry.uuid,
                entry.value,
                i + 1
            ));
        }
        
        return result;
    }
    
    /**
     * 获取玩家在排行榜中的排名
     */
    public int getPlayerRank(Player player, LeaderboardManager.LeaderboardType type) {
        if (plugin.getLeaderboardManager() == null) {
            return -1;
        }
        return plugin.getLeaderboardManager().getPlayerRank(player, type);
    }
    
    /**
     * 获取可用地图列表
     */
    public java.util.List<String> getAvailableMaps() {
        java.util.List<String> maps = new java.util.ArrayList<>();
        for (MapManager.GameMap map : plugin.getMapManager().getMaps()) {
            maps.add(map.getDisplayName());
        }
        return maps;
    }
    
    /**
     * 玩家统计数据类
     */
    public static class PlayerStats {
        public final int kills;
        public final int deaths;
        public final int extracts;
        public final double totalValue;
        public final long playTime;
        
        public PlayerStats(int kills, int deaths, int extracts, double totalValue, long playTime) {
            this.kills = kills;
            this.deaths = deaths;
            this.extracts = extracts;
            this.totalValue = totalValue;
            this.playTime = playTime;
        }
    }
    
    /**
     * 排行榜条目类
     */
    public static class LeaderboardEntry {
        public final UUID playerUuid;
        public final double value;
        public final int rank;
        
        public LeaderboardEntry(UUID playerUuid, double value, int rank) {
            this.playerUuid = playerUuid;
            this.value = value;
            this.rank = rank;
        }
    }
}

