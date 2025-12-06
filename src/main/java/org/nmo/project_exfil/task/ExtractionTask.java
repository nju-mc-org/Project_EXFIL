package org.nmo.project_exfil.task;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.nmo.project_exfil.manager.GameManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ExtractionTask extends BukkitRunnable {

    private final GameManager gameManager;
    private final Map<UUID, Integer> extractionTimers = new HashMap<>();

    public ExtractionTask(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkExtraction(player);
        }
    }

    private void checkExtraction(Player player) {
        Location loc = player.getLocation();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        
        var set = query.getApplicableRegions(BukkitAdapter.adapt(loc));
        boolean inExtractionZone = false;

        for (ProtectedRegion region : set) {
            // Check if region ID starts with "extract_"
            if (region.getId().toLowerCase().startsWith("extract_")) {
                inExtractionZone = true;
                handleExtractionProcess(player);
                break;
            }
        }

        if (!inExtractionZone) {
            if (extractionTimers.containsKey(player.getUniqueId())) {
                extractionTimers.remove(player.getUniqueId());
                player.sendTitle("", "§cExtraction Cancelled", 0, 20, 10);
            }
        }
    }

    private void handleExtractionProcess(Player player) {
        int timeLeft = extractionTimers.getOrDefault(player.getUniqueId(), 6); // 5 seconds + 1 buffer
        timeLeft--;
        
        if (timeLeft <= 0) {
            extractionTimers.remove(player.getUniqueId());
            player.sendTitle("§aEXTRACTED", "", 0, 40, 10);
            gameManager.teleportToLobby(player);
        } else {
            extractionTimers.put(player.getUniqueId(), timeLeft);
            player.sendTitle("§aExtracting...", "§7" + timeLeft + " seconds", 0, 20, 0);
        }
    }
}
