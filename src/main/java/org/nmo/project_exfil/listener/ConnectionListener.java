package org.nmo.project_exfil.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.nmo.project_exfil.manager.GameManager;

public class ConnectionListener implements Listener {

    private final GameManager gameManager;

    public ConnectionListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // If player disconnects during raid, treat as failed raid to avoid logout saving
        gameManager.handleDisconnect(event.getPlayer());
    }
}
