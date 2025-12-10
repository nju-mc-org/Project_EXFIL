package org.nmo.project_exfil.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
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
                attacker.sendMessage("§cYou cannot attack while downed!");
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
}
