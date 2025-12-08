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
import org.nmo.project_exfil.util.DependencyHelper;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import java.time.Duration;

public class ExtractionTask extends BukkitRunnable {

    private final GameManager gameManager;
    private final Map<UUID, Integer> extractionTimers = new HashMap<>();
    private final ProjectEXFILPlugin plugin = ProjectEXFILPlugin.getPlugin();

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
                
                Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(500));
                Title title = Title.title(Component.empty(), plugin.getLanguageManager().getMessage("exfil.extract.cancelled"), times);
                player.showTitle(title);
                
                DependencyHelper.removeExtractionHologram(player);
                DependencyHelper.setExtractionHeader(player, false);
            }
        }
    }

    private void handleExtractionProcess(Player player) {
        int timeLeft = extractionTimers.getOrDefault(player.getUniqueId(), 6); // 5 seconds + 1 buffer
        timeLeft--;
        
        if (timeLeft <= 0) {
            extractionTimers.remove(player.getUniqueId());
            DependencyHelper.removeExtractionHologram(player);
            DependencyHelper.setExtractionHeader(player, false);
            
            Component subComp = plugin.getLanguageManager().getMessage("exfil.extract.subtitle", Placeholder.unparsed("player", player.getName()));
            
            Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofMillis(2000), Duration.ofMillis(500));
            Title title = Title.title(plugin.getLanguageManager().getMessage("exfil.extract.title"), subComp, times);
            player.showTitle(title);
            
            gameManager.teleportToLobby(player);
        } else {
            extractionTimers.put(player.getUniqueId(), timeLeft);
            
            Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ZERO);
            Title title = Title.title(plugin.getLanguageManager().getMessage("exfil.extract.extracting"), plugin.getLanguageManager().getMessage("exfil.extract.seconds", Placeholder.unparsed("time", String.valueOf(timeLeft))), times);
            player.showTitle(title);
            
            DependencyHelper.createExtractionHologram(player, timeLeft);
            DependencyHelper.setExtractionHeader(player, true);
        }
    }
}
