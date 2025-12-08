package org.nmo.project_exfil.manager;

import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nmo.project_exfil.ProjectEXFILPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

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
        plugin.getLanguageManager().send(player, "exfil.game.deploying", Placeholder.unparsed("map", mapName));
        
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
              plugin.getLanguageManager().send(player, "exfil.deploy");
              plugin.getScoreboardManager().startCombat(player);
        }).onEmpty(() -> {
              plugin.getLanguageManager().send(player, "exfil.game.map_not_found", Placeholder.unparsed("map", mapName));
        });
    }
    
    public void teleportToLobby(Player player) {
        if (mvApi == null) return;

        mvApi.getWorldManager().getWorld("lobby").peek(world -> {
            mvApi.getSafetyTeleporter().to(world.getSpawnLocation()).teleport(player);
            plugin.getScoreboardManager().endCombat(player);
        });
    }
}
