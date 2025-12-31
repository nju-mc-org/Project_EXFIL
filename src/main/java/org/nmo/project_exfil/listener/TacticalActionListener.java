package org.nmo.project_exfil.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.manager.TacticalActionManager;

/**
 * 战术动作监听器
 * 处理玩家输入，触发战术动作
 */
public class TacticalActionListener implements Listener {
    
    private final ProjectEXFILPlugin plugin;
    private final TacticalActionManager tacticalManager;
    
    public TacticalActionListener(ProjectEXFILPlugin plugin, TacticalActionManager tacticalManager) {
        this.plugin = plugin;
        this.tacticalManager = tacticalManager;
    }
    
    /**
     * 处理玩家交互（用于快速换弹）
     * 右键点击空气或方块时，如果满足条件则触发快速换弹
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // 检查是否在游戏中
        if (plugin.getGameManager().getPlayerInstance(player) == null) {
            return;
        }
        
        // 快速换弹：Shift + 右键
        if (player.isSneaking() && 
            (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR ||
             event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)) {
            
            if (tacticalManager.tryFastReload(player)) {
                event.setCancelled(true);
            }
        }
    }
    
    /**
     * 处理玩家交换主副手物品（用于侧身）
     * F键（默认）可以触发侧身
     */
    @EventHandler
    public void onPlayerSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        
        // 检查是否在游戏中
        if (plugin.getGameManager().getPlayerInstance(player) == null) {
            return;
        }
        
        // 侧身：F键（交换主副手）
        if (player.isSneaking()) {
            // Shift + F = 左侧身
            event.setCancelled(true);
            tacticalManager.toggleLean(player, TacticalActionManager.LeanState.LEFT);
        } else {
            // F = 右侧身
            event.setCancelled(true);
            tacticalManager.toggleLean(player, TacticalActionManager.LeanState.RIGHT);
        }
    }
    
    /**
     * 处理滑铲
     * 滑铲通过移动时按潜行键触发（在TacticalActionManager中处理）
     * 这里可以添加额外的触发条件
     */
}

