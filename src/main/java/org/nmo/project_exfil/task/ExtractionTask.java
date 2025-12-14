package org.nmo.project_exfil.task;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.nmo.project_exfil.manager.GameInstance;
import org.nmo.project_exfil.manager.GameManager;
import org.nmo.project_exfil.manager.RegionManager;
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
    private final RegionManager regionManager;
    private final Map<UUID, Integer> extractionTimers = new HashMap<>();
    private final ProjectEXFILPlugin plugin = ProjectEXFILPlugin.getPlugin();

    public ExtractionTask(GameManager gameManager, RegionManager regionManager) {
        this.gameManager = gameManager;
        this.regionManager = regionManager;
    }

    @Override
    public void run() {
        for (Map.Entry<UUID, GameInstance> entry : gameManager.getPlayerInstancesView().entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                extractionTimers.remove(entry.getKey());
                continue;
            }
            checkExtraction(player, entry.getValue());
        }
    }

    private void checkExtraction(Player player, GameInstance instance) {
        
        boolean inExtractionZone = regionManager.isPlayerInExtractionPoint(player, instance.getTemplateName());

        if (inExtractionZone) {
            handleExtractionProcess(player);
        } else {
            if (extractionTimers.containsKey(player.getUniqueId())) {
                extractionTimers.remove(player.getUniqueId());
                
                Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(500));
                Title title = Title.title(Component.empty(), plugin.getLanguageManager().getMessage("exfil.extract.cancelled"), times);
                player.showTitle(title);
                
                // DependencyHelper.removeExtractionHologram(player);
                DependencyHelper.setExtractionHeader(player, false);
            }
        }
    }

    private void handleExtractionProcess(Player player) {
        int timeLeft = extractionTimers.getOrDefault(player.getUniqueId(), 6); // 5 seconds + 1 buffer
        timeLeft--;
        
        if (timeLeft <= 0) {
            extractionTimers.remove(player.getUniqueId());
            // DependencyHelper.removeExtractionHologram(player);
            DependencyHelper.setExtractionHeader(player, false);
            
            Component subComp = plugin.getLanguageManager().getMessage("exfil.extract.subtitle", Placeholder.unparsed("player", player.getName()));
            
            Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofMillis(2000), Duration.ofMillis(500));
            Title title = Title.title(plugin.getLanguageManager().getMessage("exfil.extract.title"), subComp, times);
            player.showTitle(title);
            
            gameManager.extractToLobby(player);
        } else {
            extractionTimers.put(player.getUniqueId(), timeLeft);
            
            Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ZERO);
            Title title = Title.title(plugin.getLanguageManager().getMessage("exfil.extract.extracting"), plugin.getLanguageManager().getMessage("exfil.extract.seconds", Placeholder.unparsed("time", String.valueOf(timeLeft))), times);
            player.showTitle(title);
            
            // DependencyHelper.createExtractionHologram(player, timeLeft);
            DependencyHelper.setExtractionHeader(player, true);
        }
    }
}
