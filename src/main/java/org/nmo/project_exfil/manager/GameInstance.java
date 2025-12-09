package org.nmo.project_exfil.manager;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.nmo.project_exfil.ProjectEXFILPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GameInstance {

    private final String instanceId;
    private final String templateName;
    private final World bukkitWorld;
    private final long startTime;
    private final Set<UUID> players = new HashSet<>();
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
    }

    public void removePlayer(Player player) {
        players.remove(player.getUniqueId());
        // Logic to end game if empty? Or keep running until time?
        // For now, keep running to allow reconnects or new joins within window
    }

    public void checkTime() {
        if (state == GameState.ENDING) return;
        
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= MATCH_DURATION_MS) {
            endGame();
        }
    }

    public void endGame() {
        state = GameState.ENDING;
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
