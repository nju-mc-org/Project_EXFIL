package org.nmo.project_exfil.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.data.PlayerDataManager;

/**
 * 玩家数据监听器 - 处理玩家数据的加载和保存
 */
public class PlayerDataListener implements Listener {
    
    private final PlayerDataManager dataManager;
    
    public PlayerDataListener(ProjectEXFILPlugin plugin) {
        this.dataManager = plugin.getPlayerDataManager();
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 加载玩家数据
        dataManager.loadPlayerData(event.getPlayer().getUniqueId());
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 保存并卸载玩家数据
        dataManager.unloadPlayerData(event.getPlayer().getUniqueId());
    }
}

