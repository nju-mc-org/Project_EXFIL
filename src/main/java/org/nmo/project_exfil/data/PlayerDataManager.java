package org.nmo.project_exfil.data;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.nmo.project_exfil.ProjectEXFILPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一的玩家数据管理器
 * 负责保存和加载所有玩家相关数据
 */
public class PlayerDataManager {
    
    private final ProjectEXFILPlugin plugin;
    private final File dataFolder;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Object> locks = new ConcurrentHashMap<>();
    
    public PlayerDataManager(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }
    
    private Object getLock(UUID uuid) {
        return locks.computeIfAbsent(uuid, k -> new Object());
    }
    
    /**
     * 加载玩家数据
     */
    public PlayerData loadPlayerData(UUID uuid) {
        // 先从缓存获取
        PlayerData cached = cache.get(uuid);
        if (cached != null) {
            return cached;
        }
        
        File file = new File(dataFolder, uuid + ".yml");
        if (!file.exists()) {
            PlayerData newData = new PlayerData(uuid);
            cache.put(uuid, newData);
            return newData;
        }
        
        synchronized (getLock(uuid)) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                PlayerData data = new PlayerData(uuid);
                
                // 加载统计数据
                data.kills = config.getInt("stats.kills", 0);
                data.deaths = config.getInt("stats.deaths", 0);
                data.extracts = config.getInt("stats.extracts", 0);
                data.totalValue = config.getDouble("stats.total_value", 0.0);
                data.playTime = config.getLong("stats.play_time", 0);
                
                // 加载任务数据
                if (config.isList("tasks.completed")) {
                    data.completedTasks = new HashSet<>(config.getStringList("tasks.completed"));
                }
                
                // 加载成就数据
                if (config.isList("achievements.completed")) {
                    data.completedAchievements = new HashSet<>(config.getStringList("achievements.completed"));
                }
                
                // 加载成就进度
                if (config.isConfigurationSection("achievements.progress")) {
                    for (String key : config.getConfigurationSection("achievements.progress").getKeys(false)) {
                        data.achievementProgress.put(key, config.getInt("achievements.progress." + key, 0));
                    }
                }
                
                // 加载交易统计
                data.itemsRecycled = config.getInt("trader.items_recycled", 0);
                data.totalRecycled = config.getDouble("trader.total_recycled", 0.0);
                
                cache.put(uuid, data);
                return data;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load player data for " + uuid + ": " + e.getMessage());
                PlayerData newData = new PlayerData(uuid);
                cache.put(uuid, newData);
                return newData;
            }
        }
    }
    
    /**
     * 保存玩家数据
     */
    public void savePlayerData(UUID uuid, boolean async) {
        PlayerData data = cache.get(uuid);
        if (data == null) return;
        
        Runnable saveTask = () -> {
            synchronized (getLock(uuid)) {
                File file = new File(dataFolder, uuid + ".yml");
                try {
                    YamlConfiguration config = new YamlConfiguration();
                    
                    // 保存统计数据
                    config.set("stats.kills", data.kills);
                    config.set("stats.deaths", data.deaths);
                    config.set("stats.extracts", data.extracts);
                    config.set("stats.total_value", data.totalValue);
                    config.set("stats.play_time", data.playTime);
                    
                    // 保存任务数据
                    config.set("tasks.completed", new ArrayList<>(data.completedTasks));
                    
                    // 保存成就数据
                    config.set("achievements.completed", new ArrayList<>(data.completedAchievements));
                    for (Map.Entry<String, Integer> entry : data.achievementProgress.entrySet()) {
                        config.set("achievements.progress." + entry.getKey(), entry.getValue());
                    }
                    
                    // 保存交易统计
                    config.set("trader.items_recycled", data.itemsRecycled);
                    config.set("trader.total_recycled", data.totalRecycled);
                    
                    config.save(file);
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to save player data for " + uuid + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        
        if (async) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, saveTask);
        } else {
            saveTask.run();
        }
    }
    
    /**
     * 保存所有玩家数据
     */
    public void saveAll(boolean async) {
        for (UUID uuid : new HashSet<>(cache.keySet())) {
            savePlayerData(uuid, async);
        }
    }
    
    /**
     * 获取玩家数据（如果不存在则加载）
     */
    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }
    
    public PlayerData getPlayerData(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadPlayerData);
    }
    
    /**
     * 移除缓存（玩家离线时）
     */
    public void unloadPlayerData(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) {
            savePlayerData(uuid, true);
        }
    }
    
    /**
     * 玩家数据类
     */
    public static class PlayerData {
        public final UUID uuid;
        
        // 统计数据
        public int kills = 0;
        public int deaths = 0;
        public int extracts = 0;
        public double totalValue = 0.0;
        public long playTime = 0;
        
        // 任务数据
        public Set<String> completedTasks = new HashSet<>();
        
        // 成就数据
        public Set<String> completedAchievements = new HashSet<>();
        public Map<String, Integer> achievementProgress = new HashMap<>();
        
        // 交易统计
        public int itemsRecycled = 0;
        public double totalRecycled = 0.0;
        
        public PlayerData(UUID uuid) {
            this.uuid = uuid;
        }
    }
}

