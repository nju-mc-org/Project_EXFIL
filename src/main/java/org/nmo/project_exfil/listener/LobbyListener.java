package org.nmo.project_exfil.listener;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class LobbyListener implements Listener {

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        World world = player.getWorld();

        // Check if in lobby
        if (world.getName().equalsIgnoreCase("world")) {
            // Cancel all damage
            event.setCancelled(true);

            // If void damage, teleport to spawn
            if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                player.teleport(world.getSpawnLocation());
            }
        }
    }
}
