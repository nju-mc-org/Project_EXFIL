package org.nmo.project_exfil.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.manager.GameManager;
import org.nmo.project_exfil.manager.ReviveManager;

public class ReviveListener implements Listener {

    private final ProjectEXFILPlugin plugin;
    private final ReviveManager reviveManager;
    private final GameManager gameManager;

    public ReviveListener(ProjectEXFILPlugin plugin, ReviveManager reviveManager, GameManager gameManager) {
        this.plugin = plugin;
        this.reviveManager = reviveManager;
        this.gameManager = gameManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        // Check if player is in a game
        if (gameManager.getPlayerInstance(player) == null) return;

        // Check if explicitly eliminated
        if (player.hasMetadata("EXFIL_ELIMINATED")) {
            player.removeMetadata("EXFIL_ELIMINATED", plugin);
            return;
        }

        // If already downed, cancel damage (lock health)
        if (reviveManager.isDowned(player)) {
            if (event.getCause() != EntityDamageEvent.DamageCause.VOID) {
                event.setCancelled(true);
            }
            return;
        }

        // If void damage, let them die (or handle differently if needed)
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) return;

        // Check if damage would be fatal or drop below 0.5 hearts (1.0 health)
        if (player.getHealth() - event.getFinalDamage() <= 1.0) {
            event.setCancelled(true);
            reviveManager.setDowned(player);
        }
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            if (reviveManager.isDowned(attacker)) {
                event.setCancelled(true);
                attacker.sendMessage("§c你无法在濒死状态下攻击！");
            }
        }
    }

    @EventHandler
    public void onRegen(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (reviveManager.isDowned(player)) {
                event.setCancelled(true);
            }
        }
    }
    
    /**
     * 限制濒死玩家的移动速度
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (reviveManager.isDowned(player)) {
            // 限制移动距离，模拟爬行
            org.bukkit.Location from = event.getFrom();
            org.bukkit.Location to = event.getTo();
            if (to != null && from.distanceSquared(to) > 0.25) { // 限制每次移动最多0.5格
                event.setCancelled(true);
            }
        }
    }
    
    /**
     * 处理救援交互
     */
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!event.isSneaking()) return; // 只处理开始潜行
        
        // 检查是否在游戏实例中
        if (gameManager.getPlayerInstance(player) == null) return;
        
        // 检查附近是否有濒死的队友
        for (Player nearby : player.getWorld().getPlayers()) {
            if (nearby.equals(player)) continue;
            if (!reviveManager.isDowned(nearby)) continue;
            
            // 检查距离
            if (player.getLocation().distanceSquared(nearby.getLocation()) <= 9) { // 3格内
                // 检查是否是队友
                if (plugin.getPartyManager().isEnabled()) {
                    com.alessiodp.parties.api.interfaces.PartyPlayer pp1 = 
                        plugin.getPartyManager().getPartyPlayer(player.getUniqueId());
                    com.alessiodp.parties.api.interfaces.PartyPlayer pp2 = 
                        plugin.getPartyManager().getPartyPlayer(nearby.getUniqueId());
                    
                    if (pp1 != null && pp2 != null && pp1.isInParty() && pp2.isInParty() &&
                        pp1.getPartyId().equals(pp2.getPartyId())) {
                        // 是队友，开始救援（ReviveManager会自动处理）
                        return;
                    }
                }
            }
        }
    }
}
