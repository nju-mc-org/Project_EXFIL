package org.nmo.project_exfil.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.nmo.project_exfil.ProjectEXFILPlugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * API管理器
 * 提供API访问控制和事件通知
 */
public class ExfilAPIManager implements Listener {
    
    private final ConcurrentMap<UUID, Long> playerJoinTimes = new ConcurrentHashMap<>();
    
    public ExfilAPIManager(ProjectEXFILPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * 获取玩家游戏时长（当前会话）
     */
    public long getPlayerSessionTime(Player player) {
        Long joinTime = playerJoinTimes.get(player.getUniqueId());
        if (joinTime == null) {
            return 0;
        }
        return System.currentTimeMillis() - joinTime;
    }
    
    /**
     * 触发API事件（供外部监听）
     */
    public void firePlayerGameStartEvent(Player player) {
        // 可以在这里实现事件系统，供外部插件监听
        // 例如：使用Bukkit的Event系统
    }
    
    /**
     * 触发API事件：玩家完成撤离
     */
    public void firePlayerExtractEvent(Player player, double value) {
        // 可以在这里实现事件系统
    }
    
    /**
     * 触发API事件：玩家击杀
     */
    public void firePlayerKillEvent(Player killer, Player victim) {
        // 可以在这里实现事件系统
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        playerJoinTimes.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerJoinTimes.remove(event.getPlayer().getUniqueId());
    }
}

