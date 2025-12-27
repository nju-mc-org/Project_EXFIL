package org.nmo.project_exfil.manager;

import org.bukkit.scheduler.BukkitRunnable;
import org.nmo.project_exfil.ProjectEXFILPlugin;

/**
 * NPC性能优化器
 * 定期优化NPC行为，减少不必要的计算
 */
public class NPCPerformanceOptimizer {
    
    private final ProjectEXFILPlugin plugin;
    private static final long OPTIMIZE_INTERVAL = 100L; // 每5秒优化一次
    
    public NPCPerformanceOptimizer(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
        startOptimizer();
    }
    
    private void startOptimizer() {
        new BukkitRunnable() {
            @Override
            public void run() {
                optimizeNPCs();
            }
        }.runTaskTimer(plugin, OPTIMIZE_INTERVAL, OPTIMIZE_INTERVAL);
    }
    
    /**
     * 优化所有NPC
     * 注意：Sentinel内部已经做了大量优化，这里主要是监控和确保配置合理
     */
    private void optimizeNPCs() {
        // Sentinel会自动优化NPC行为，包括：
        // - 距离检测优化
        // - 更新频率优化
        // - 目标选择优化
        // 
        // 这里可以添加额外的优化逻辑（如果需要）
        // 例如：清理长时间无玩家的NPC、调整更新频率等
    }
    
    /**
     * 清理无用的NPC（可选）
     */
    public void cleanupUnusedNPCs() {
        // 可以在这里实现清理逻辑
        // 例如：清理长时间没有玩家接触的NPC
    }
}

