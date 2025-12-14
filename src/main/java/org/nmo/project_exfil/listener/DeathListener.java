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

        if (gameManager.getPlayerInstance(player) != null) {
            // It's a raid world
            event.deathMessage(null);
            event.setKeepInventory(false);
            event.setKeepLevel(false);
            
            plugin.getLanguageManager().send(player, "exfil.death_raid");
            plugin.getLanguageManager().send(player, "exfil.death_lost");
            
            for (Player p : world.getPlayers()) {
                p.sendMessage("§c☠ " + player.getName() + " was killed.");
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is currently in a game instance
        if (gameManager.getPlayerInstance(player) != null) {
            World lobby = Bukkit.getWorld("world");
            if (lobby != null) {
                event.setRespawnLocation(lobby.getSpawnLocation());
                gameManager.removePlayerFromGame(player);
                player.sendMessage("§eReturned to lobby.");
            }
        }
    }
}
