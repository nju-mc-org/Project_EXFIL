package org.nmo.project_exfil.manager;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.manager.gamemodule.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class GameInstance {

    private final String instanceId;
    private final String templateName;
    private final World bukkitWorld;
    private final long startTime;
    private final Set<UUID> players = new HashSet<>();
    private final List<GameModule> modules = new ArrayList<>();
    private GameState state;
    
    // Configurable settings (could be passed in)
    private static final long JOIN_WINDOW_MS = 5 * 60 * 1000; // 5 minutes
    private static final long MATCH_DURATION_MS = 20 * 60 * 1000; // 20 minutes

    public GameInstance(String instanceId, String templateName, World bukkitWorld) {
        this.instanceId = instanceId;
        this.templateName = templateName;
        this.bukkitWorld = bukkitWorld;
        this.startTime = System.currentTimeMillis();
        this.state = GameState.WAITING;
        
        registerModules();
        modules.forEach(m -> m.onStart(this));
    }

    private void registerModules() {
        modules.add(new NPCModule());
        modules.add(new HologramModule());
        modules.add(new BossBarModule());
        modules.add(new BoundaryModule());
        modules.add(new LootModule());
    }

    public boolean canJoin() {
        if (state == GameState.ENDING) return false;
        long elapsed = System.currentTimeMillis() - startTime;
        return elapsed < JOIN_WINDOW_MS;
    }

    public void addPlayer(Player player) {
        players.add(player.getUniqueId());
        if (state == GameState.WAITING) {
            state = GameState.PLAYING;
        }
        
        modules.forEach(m -> m.onPlayerJoin(this, player));
    }

    public void removePlayer(Player player) {
        players.remove(player.getUniqueId());
        
        modules.forEach(m -> m.onPlayerQuit(this, player));
        
        // If no players left and game has started (or even if waiting), unload it to save resources
        if (players.isEmpty()) {
            // Schedule unload with a small delay to allow for immediate reconnects if needed, 
            // but user requested deletion if "eventually no players".
            // Delay is crucial to ensure player has fully left the world context before unloading
            Bukkit.getScheduler().runTaskLater(ProjectEXFILPlugin.getPlugin(), this::endGame, 40L);
        }
    }

    public void checkTime() {
        if (state == GameState.ENDING) return;
        
        modules.forEach(m -> m.onTick(this));
        
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= MATCH_DURATION_MS) {
            endGame();
        }
    }

    public void endGame() {
        state = GameState.ENDING;
        
        modules.forEach(m -> m.onEnd(this));

        // Kick all players to lobby
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                ProjectEXFILPlugin.getPlugin().getGameManager().teleportToLobby(p);
                ProjectEXFILPlugin.getPlugin().getLanguageManager().send(p, "exfil.game.ended");
            }
        }
        // Notify manager to unload (handled in GameManager usually)
        ProjectEXFILPlugin.getPlugin().getGameManager().unloadInstance(this);
    }

    public <T extends GameModule> T getModule(Class<T> clazz) {
        for (GameModule module : modules) {
            if (clazz.isInstance(module)) {
                return clazz.cast(module);
            }
        }
        return null;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public World getBukkitWorld() {
        return bukkitWorld;
    }
    
    public Set<UUID> getPlayers() {
        return players;
    }

    public enum GameState {
        WAITING,
        PLAYING,
        ENDING
    }
}
