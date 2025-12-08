package org.nmo.project_exfil.listener;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.manager.GameManager;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public class DeathListener implements Listener {

    private final ProjectEXFILPlugin plugin;
    private final GameManager gameManager;

    public DeathListener(ProjectEXFILPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        World world = player.getWorld();

        // Check if player is in a game world (not lobby)
        // Assuming lobby world is named "lobby"
        if (!world.getName().equalsIgnoreCase("lobby")) {
            // It's a raid world
            event.deathMessage(null); // Hide default message
            
            // Send custom message
            plugin.getLanguageManager().send(player, "exfil.death_raid");
            plugin.getLanguageManager().send(player, "exfil.death_lost");
            
            // Ensure drops happen (default behavior)
            // But we might want to clear drops if we want "secure container" logic later
            // For now, let it drop.
            
            // We can also announce to others in that world
            for (Player p : world.getPlayers()) {
                p.sendMessage("§c☠ " + player.getName() + " was killed.");
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // If they died in a raid, force them back to lobby
        // We can check if the respawn location is NOT lobby, then force lobby
        if (!event.getRespawnLocation().getWorld().getName().equalsIgnoreCase("lobby")) {
            // We need to find the lobby world
            World lobby = Bukkit.getWorld("lobby");
            if (lobby != null) {
                event.setRespawnLocation(lobby.getSpawnLocation());
                player.sendMessage("§eReturned to lobby.");
            }
        }
    }
}
