package org.nmo.project_exfil.manager;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.region.ExtractionRegion;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 俯瞰地图管理器
 * 在游戏中显示地图概览，包括玩家位置、撤离点等信息
 */
public class MapOverviewManager {
    
    private final ProjectEXFILPlugin plugin;
    private final Map<UUID, BossBar> activeBossBars = new HashMap<>();
    private final Map<UUID, BukkitRunnable> updateTasks = new HashMap<>();
    
    public MapOverviewManager(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 为玩家显示俯瞰地图
     * @param player 玩家
     */
    public void showMapOverview(Player player) {
        GameInstance instance = plugin.getGameManager().getPlayerInstance(player);
        if (instance == null) {
            return;
        }
        
        // 创建BossBar显示地图信息
        BossBar bossBar = BossBar.bossBar(
            Component.text("地图概览 - 按M键查看", NamedTextColor.AQUA),
            1.0f,
            BossBar.Color.BLUE,
            BossBar.Overlay.PROGRESS
        );
        
        player.showBossBar(bossBar);
        activeBossBars.put(player.getUniqueId(), bossBar);
        
        // 启动更新任务
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || plugin.getGameManager().getPlayerInstance(player) == null) {
                    hideMapOverview(player);
                    return;
                }
                
                updateMapOverview(player, bossBar);
            }
        };
        task.runTaskTimer(plugin, 0L, 20L); // 每秒更新一次
        updateTasks.put(player.getUniqueId(), task);
    }
    
    /**
     * 隐藏玩家的俯瞰地图
     * @param player 玩家
     */
    public void hideMapOverview(Player player) {
        BossBar bossBar = activeBossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
        
        BukkitRunnable task = updateTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * 更新地图概览信息
     * @param player 玩家
     * @param bossBar BossBar
     */
    private void updateMapOverview(Player player, BossBar bossBar) {
        GameInstance instance = plugin.getGameManager().getPlayerInstance(player);
        if (instance == null) {
            return;
        }
        
        Location playerLoc = player.getLocation();
        RegionManager regionManager = plugin.getRegionManager();
        Map<String, ExtractionRegion> extractions = regionManager.getExtractionRegions(instance.getTemplateName());
        
        // 找到最近的撤离点
        ExtractionRegion nearestExtraction = null;
        String nearestExtractionName = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (Map.Entry<String, ExtractionRegion> entry : extractions.entrySet()) {
            ExtractionRegion extraction = entry.getValue();
            double centerX = extraction.getBox().getCenterX();
            double centerZ = extraction.getBox().getCenterZ();
            double dist = Math.sqrt(
                Math.pow(playerLoc.getX() - centerX, 2) + 
                Math.pow(playerLoc.getZ() - centerZ, 2)
            );
            
            if (dist < nearestDistance) {
                nearestDistance = dist;
                nearestExtraction = extraction;
                nearestExtractionName = entry.getKey();
            }
        }
        
        // 更新BossBar显示
        if (nearestExtraction != null && nearestExtractionName != null) {
            String extractionName = nearestExtractionName;
            int distance = (int) nearestDistance;
            
            Component text = Component.text()
                .append(Component.text("最近撤离点: ", NamedTextColor.GRAY))
                .append(Component.text(extractionName, NamedTextColor.GREEN))
                .append(Component.text(" | 距离: ", NamedTextColor.GRAY))
                .append(Component.text(distance + "m", NamedTextColor.YELLOW))
                .append(Component.text(" | 坐标: ", NamedTextColor.GRAY))
                .append(Component.text(
                    String.format("%.0f, %.0f", playerLoc.getX(), playerLoc.getZ()),
                    NamedTextColor.AQUA
                ))
                .build();
            
            bossBar.name(text);
            
            // 根据距离设置进度条（距离越近，进度越高）
            float progress = Math.max(0.0f, Math.min(1.0f, 1.0f - (float)(nearestDistance / 1000.0)));
            bossBar.progress(progress);
        } else {
            Component text = Component.text()
                .append(Component.text("地图概览", NamedTextColor.AQUA))
                .append(Component.text(" | 坐标: ", NamedTextColor.GRAY))
                .append(Component.text(
                    String.format("%.0f, %.0f", playerLoc.getX(), playerLoc.getZ()),
                    NamedTextColor.AQUA
                ))
                .build();
            
            bossBar.name(text);
            bossBar.progress(1.0f);
        }
    }
    
    /**
     * 清理所有俯瞰地图
     */
    public void cleanup() {
        for (UUID uuid : new java.util.HashSet<>(activeBossBars.keySet())) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                hideMapOverview(player);
            }
        }
    }
}

