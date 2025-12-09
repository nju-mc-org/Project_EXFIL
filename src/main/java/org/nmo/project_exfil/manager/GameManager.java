package org.nmo.project_exfil.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nmo.project_exfil.ProjectEXFILPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GameManager {

    private final ProjectEXFILPlugin plugin;
    private final SlimeWorldManagerIntegration slimeManager;
    private final Map<String, List<GameInstance>> activeInstances = new HashMap<>();
    private final Map<UUID, GameInstance> playerInstances = new HashMap<>();

    public GameManager(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
        this.slimeManager = new SlimeWorldManagerIntegration(plugin);
        
        // Start a ticker to check game times
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickInstances, 20L, 20L);
    }

    public void joinQueue(Player player, String mapName) {
        plugin.getLanguageManager().send(player, "exfil.game.deploying", Placeholder.unparsed("map", mapName));
        
        // Check for existing joinable instance
        List<GameInstance> instances = activeInstances.getOrDefault(mapName, new ArrayList<>());
        for (GameInstance instance : instances) {
            if (instance.canJoin()) {
                sendPlayerToInstance(player, instance);
                return;
            }
        }
        
        // No joinable instance found, create new one
        createNewInstance(player, mapName);
    }

    private void createNewInstance(Player player, String mapName) {
        plugin.getLanguageManager().send(player, "exfil.game.creating_instance");
        
        slimeManager.createInstance(mapName).thenAccept(worldInstance -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                GameInstance game = new GameInstance(worldInstance.getBukkitWorld().getName(), mapName, worldInstance.getBukkitWorld());
                
                activeInstances.computeIfAbsent(mapName, k -> new ArrayList<>()).add(game);
                sendPlayerToInstance(player, game);
            });
        }).exceptionally(e -> {
            plugin.getLanguageManager().send(player, "exfil.game.map_not_found", Placeholder.unparsed("map", mapName));
            e.printStackTrace();
            return null;
        });
    }

    private void sendPlayerToInstance(Player player, GameInstance instance) {
        if (player.isOnline()) {
            instance.addPlayer(player);
            playerInstances.put(player.getUniqueId(), instance);
            player.teleport(instance.getBukkitWorld().getSpawnLocation());
            plugin.getLanguageManager().send(player, "exfil.deploy");
            plugin.getScoreboardManager().startCombat(player);
        }
    }

    public void teleportToMap(Player player, String mapName) {
        joinQueue(player, mapName);
    }
    
    public void teleportToLobby(Player player) {
        GameInstance current = playerInstances.remove(player.getUniqueId());
        if (current != null) {
            current.removePlayer(player);
        }

        // Assuming lobby is a standard world or also managed by Slime
        org.bukkit.World lobby = Bukkit.getWorld("lobby");
        if (lobby != null) {
            player.teleport(lobby.getSpawnLocation());
            plugin.getScoreboardManager().endCombat(player);
        } else {
             slimeManager.loadWorld("lobby", true).thenAccept(instance -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.teleport(instance.getBukkitWorld().getSpawnLocation());
                        plugin.getScoreboardManager().endCombat(player);
                    }
                });
            });
        }
    }
    
    public void unloadInstance(GameInstance instance) {
        activeInstances.get(instance.getTemplateName()).remove(instance);
        slimeManager.unloadWorld(instance.getBukkitWorld().getName());
    }
    
    private void tickInstances() {
        for (List<GameInstance> list : activeInstances.values()) {
            // Create a copy to avoid CME if instance removes itself
            new ArrayList<>(list).forEach(GameInstance::checkTime);
        }
    }
    
    public GameInstance getPlayerInstance(Player player) {
        return playerInstances.get(player.getUniqueId());
    }
    
    public SlimeWorldManagerIntegration getSlimeManager() {
        return slimeManager;
    }
}
