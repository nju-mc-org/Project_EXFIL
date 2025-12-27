package org.nmo.project_exfil.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.integration.slime.SlimeWorldManagerIntegration;
import org.nmo.project_exfil.region.SpawnRegion;
import org.nmo.project_exfil.region.ExtractionRegion;

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
            
            Location spawnLoc = findValidSpawnLocation(instance);
            if (spawnLoc == null) {
                spawnLoc = instance.getBukkitWorld().getSpawnLocation();
            }
            
            player.teleport(spawnLoc);
            plugin.getLanguageManager().send(player, "exfil.deploy");
            plugin.getScoreboardManager().startCombat(player);
        }
    }

    private Location findValidSpawnLocation(GameInstance instance) {
        RegionManager regionManager = plugin.getRegionManager();
        SpawnRegion spawnRegion = regionManager.getSpawnRegion(instance.getTemplateName());
        
        if (spawnRegion == null) {
            return null;
        }
        
        World world = instance.getBukkitWorld();
        Map<String, ExtractionRegion> extractions = regionManager.getExtractionRegions(instance.getTemplateName());
        
        // Try 20 times to find a valid spot
        for (int i = 0; i < 20; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double dist = Math.random() * spawnRegion.getRadius();
            double x = spawnRegion.getX() + dist * Math.cos(angle);
            double z = spawnRegion.getZ() + dist * Math.sin(angle);
            
            // Get highest block at x, z
            int highestY = world.getHighestBlockYAt((int)x, (int)z);
            Block blockUnder = world.getBlockAt((int)x, highestY, (int)z);
            
            // Check if the block is safe to stand on
            if (!isSafeSpawnBlock(blockUnder)) {
                continue;
            }
            
            Location candidate = new Location(world, x, highestY + 1, z);
            
            if (isValidSpawn(candidate, instance, extractions)) {
                return candidate;
            }
        }
        
        // Fallback: just return center (adjusted for Y)
        int centerY = world.getHighestBlockYAt((int)spawnRegion.getX(), (int)spawnRegion.getZ());
        return new Location(world, spawnRegion.getX(), centerY + 1, spawnRegion.getZ());
    }
    
    private boolean isSafeSpawnBlock(Block block) {
        if (block == null) return false;
        Material type = block.getType();
        
        // Must be solid
        if (!type.isSolid()) return false;
        
        // Must not be dangerous
        if (type == Material.LAVA || type == Material.MAGMA_BLOCK || type == Material.CACTUS || type == Material.FIRE || type == Material.CAMPFIRE || type == Material.SOUL_CAMPFIRE || type == Material.SWEET_BERRY_BUSH) {
            return false;
        }
        
        // Prefer not leaves if possible, but for now allow them as they are standable.
        // Avoid fences/walls/panes as they are annoying to spawn on? Bukkit handles Y adjustment usually but let's be safe.
        
        return true;
    }
    
    private boolean isValidSpawn(Location loc, GameInstance instance, Map<String, ExtractionRegion> extractions) {
        // Check distance to other players
        for (UUID uuid : instance.getPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.getWorld().equals(loc.getWorld())) {
                if (p.getLocation().distanceSquared(loc) < 400) { // 20 blocks
                    return false;
                }
            }
        }
        
        // Check distance to extraction points
        for (ExtractionRegion region : extractions.values()) {
            // Simple check: distance to center of extraction box
            double centerX = region.getBox().getCenterX();
            double centerZ = region.getBox().getCenterZ();
            double distSq = Math.pow(loc.getX() - centerX, 2) + Math.pow(loc.getZ() - centerZ, 2);
            if (distSq < 2500) { // 50 blocks
                return false;
            }
        }
        
        return true;
    }

    public void teleportToMap(Player player, String mapName) {
        joinQueue(player, mapName);
    }
    
    public void removePlayerFromGame(Player player) {
        GameInstance current = playerInstances.remove(player.getUniqueId());
        if (current != null) {
            current.removePlayer(player);
        }
        plugin.getScoreboardManager().endCombat(player);
    }

    public void extractToLobby(Player player) {
        depositInventoryToStash(player);
        plugin.getLanguageManager().send(player, "exfil.extract_success");
        
        // 更新任务和成就
        if (plugin.getTaskManager() != null) {
            plugin.getTaskManager().updateProgress(player, org.nmo.project_exfil.manager.TaskManager.TaskTarget.EXTRACT, 1);
        }
        if (plugin.getAchievementManager() != null) {
            plugin.getAchievementManager().updateProgress(player, org.nmo.project_exfil.manager.AchievementManager.AchievementType.EXTRACT, 1);
        }
        
        // 更新统计数据
        if (plugin.getPlayerDataManager() != null) {
            org.nmo.project_exfil.data.PlayerDataManager.PlayerData data = 
                plugin.getPlayerDataManager().getPlayerData(player);
            data.extracts++;
            // 计算本次行动的价值（简化版，可以改进）
            double value = 0.0;
            for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
                if (item != null && !item.getType().isAir()) {
                    // 简单的价值计算，可以根据实际需求改进
                    value += item.getAmount() * 10.0;
                }
            }
            data.totalValue += value;
            plugin.getPlayerDataManager().savePlayerData(player.getUniqueId(), true);
        }
        
        teleportToLobby(player);
    }

    public void failToLobby(Player player) {
        clearPlayerInventory(player);
        plugin.getLanguageManager().send(player, "exfil.raid.failed");
        teleportToLobby(player);
    }

    public void handleDisconnect(Player player) {
        if (getPlayerInstance(player) != null) {
            clearPlayerInventory(player);
        }
        removePlayerFromGame(player);
    }

    private void clearPlayerInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new org.bukkit.inventory.ItemStack[4]);
        player.getInventory().setItemInOffHand(null);
    }

    private void depositInventoryToStash(Player player) {
        java.util.List<org.bukkit.inventory.ItemStack> items = new java.util.ArrayList<>();
        for (org.bukkit.inventory.ItemStack it : player.getInventory().getStorageContents()) {
            if (it != null && !it.getType().isAir()) items.add(it.clone());
        }
        for (org.bukkit.inventory.ItemStack it : player.getInventory().getArmorContents()) {
            if (it != null && !it.getType().isAir()) items.add(it.clone());
        }
        org.bukkit.inventory.ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && !offhand.getType().isAir()) items.add(offhand.clone());

        clearPlayerInventory(player);

        if (!items.isEmpty()) {
            plugin.getStashManager().depositItemsAsync(player, items);
        }
    }

    public void teleportToLobby(Player player) {
        removePlayerFromGame(player);

        // Use default world "world" as lobby
        org.bukkit.World lobby = Bukkit.getWorld("world");
        if (lobby == null && !Bukkit.getWorlds().isEmpty()) {
            lobby = Bukkit.getWorlds().get(0);
        }

        if (lobby != null) {
            player.teleport(lobby.getSpawnLocation());
        } else {
             plugin.getLogger().severe("Could not find main world 'world' to teleport player to lobby!");
        }
    }
    
    public void unloadInstance(GameInstance instance) {
        List<GameInstance> list = activeInstances.get(instance.getTemplateName());
        if (list != null) {
            list.remove(instance);
        }
        slimeManager.unloadWorld(instance.getBukkitWorld().getName());
    }
    
    private void tickInstances() {
        for (List<GameInstance> list : activeInstances.values()) {
            // Create a copy to avoid CME if instance removes itself
            new ArrayList<>(list).forEach(GameInstance::checkTime);
        }
    }
    
    public GameInstance getGameInstance(Player player) {
        return playerInstances.get(player.getUniqueId());
    }

    public GameInstance getPlayerInstance(Player player) {
        return playerInstances.get(player.getUniqueId());
    }

    public java.util.Map<java.util.UUID, GameInstance> getPlayerInstancesView() {
        return java.util.Collections.unmodifiableMap(playerInstances);
    }
    
    public SlimeWorldManagerIntegration getSlimeManager() {
        return slimeManager;
    }

    public String getTemplateName(World world) {
        for (List<GameInstance> instances : activeInstances.values()) {
            for (GameInstance instance : instances) {
                if (instance.getBukkitWorld().equals(world)) {
                    return instance.getTemplateName();
                }
            }
        }
        return world.getName();
    }
}
