package org.nmo.project_exfil.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.manager.GameManager;
import org.nmo.project_exfil.manager.TaskManager;
import org.nmo.project_exfil.manager.AchievementManager;

/**
 * 监听器 - 跟踪任务和成就进度
 */
public class TaskAchievementListener implements Listener {
    
    private final GameManager gameManager;
    private final TaskManager taskManager;
    private final AchievementManager achievementManager;
    
    public TaskAchievementListener(ProjectEXFILPlugin plugin) {
        this.gameManager = plugin.getGameManager();
        this.taskManager = plugin.getTaskManager();
        this.achievementManager = plugin.getAchievementManager();
    }
    
    /**
     * 玩家死亡事件 - 更新击杀相关任务和成就
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        
        if (killer != null && killer instanceof Player) {
            // 更新击杀玩家相关的任务和成就
            taskManager.updateProgress(killer, TaskManager.TaskTarget.KILL_PLAYER, 1);
            achievementManager.updateProgress(killer, AchievementManager.AchievementType.KILL_PLAYER, 1);
            
            // 更新统计数据
            if (org.nmo.project_exfil.ProjectEXFILPlugin.getPlugin().getPlayerDataManager() != null) {
                org.nmo.project_exfil.data.PlayerDataManager.PlayerData data = 
                    org.nmo.project_exfil.ProjectEXFILPlugin.getPlugin().getPlayerDataManager().getPlayerData(killer);
                data.kills++;
                org.nmo.project_exfil.ProjectEXFILPlugin.getPlugin().getPlayerDataManager()
                    .savePlayerData(killer.getUniqueId(), true);
            }
        }
    }
    
    /**
     * 实体死亡事件 - 更新击杀NPC相关的任务和成就
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.Player)) {
            // 如果是NPC或其他实体
            Player killer = event.getEntity().getKiller();
            if (killer != null && killer instanceof Player) {
                // 检查是否在游戏实例中
                if (gameManager.getPlayerInstance(killer) != null) {
                    // 更新击杀NPC相关的任务和成就
                    taskManager.updateProgress(killer, TaskManager.TaskTarget.KILL_NPC, 1);
                    achievementManager.updateProgress(killer, AchievementManager.AchievementType.KILL_NPC, 1);
                }
            }
        }
    }
}

