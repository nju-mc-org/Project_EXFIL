package org.nmo.project_exfil.manager;

import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nmo.project_exfil.ProjectEXFILPlugin;

public class GameManager {

    private final ProjectEXFILPlugin plugin;
    private MultiverseCoreApi mvApi;

    public GameManager(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
        // Initialize MV API safely
        try {
            this.mvApi = MultiverseCoreApi.get();
        } catch (IllegalStateException e) {
            plugin.getLogger().severe("Multiverse-Core API not found! Is it installed?");
        }
    }

    public void joinQueue(Player player, String mapName) {
        player.sendMessage("§eDeploying to " + mapName + " in 5 seconds...");
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                teleportToMap(player, mapName);
            }
        }, 100L);
    }

    public void teleportToMap(Player player, String mapName) {
        if (mvApi == null) return;
        
        mvApi.getWorldManager().getWorld(mapName).peek(world -> {
             mvApi.getSafetyTeleporter().to(world.getSpawnLocation()).teleport(player);
             player.sendMessage("§aDeployed!");
        }).onEmpty(() -> {
             player.sendMessage("§cMap world not found: " + mapName);
        });
    }
    
    public void teleportToLobby(Player player) {
        if (mvApi == null) return;

        mvApi.getWorldManager().getWorld("lobby").peek(world -> {
            mvApi.getSafetyTeleporter().to(world.getSpawnLocation()).teleport(player);
        });
    }
}
