package org.nmo.project_exfil.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import com.alessiodp.parties.api.interfaces.PartyPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReviveManager {

    private final ProjectEXFILPlugin plugin;
    private final Map<UUID, DownedData> downedPlayers = new HashMap<>();
    private final int MAX_DOWNED_TIME = 60 * 20; // 60 seconds in ticks
    private final int REVIVE_TIME = 10 * 20; // 10 seconds in ticks

    public ReviveManager(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
        startTask();
    }

    private void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void tick() {
        // Use a copy to avoid ConcurrentModificationException if we remove players
        for (UUID uuid : new HashMap<>(downedPlayers).keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                downedPlayers.remove(uuid);
                continue;
            }

            DownedData data = downedPlayers.get(uuid);
            
            // Apply effects
            if (!player.isSwimming()) {
                player.setSwimming(true);
            }
            // player.setGliding(true); // Optional: Try if swimming doesn't work visually
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 4, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0, false, false));

            // Check for reviver
            Player reviver = findReviver(player);
            
            if (reviver != null) {
                // Being revived
                data.reviveProgress++;
                
                // Notify
                sendActionBar(player, "§aBeing revived: " + getProgressBar(data.reviveProgress, REVIVE_TIME));
                sendActionBar(reviver, "§aReviving " + player.getName() + ": " + getProgressBar(data.reviveProgress, REVIVE_TIME));
                
                if (data.reviveProgress >= REVIVE_TIME) {
                    revive(player);
                }
            } else {
                // Not being revived
                data.reviveProgress = 0; // Reset progress if interrupted
                data.timeLeft--;
                
                sendActionBar(player, "§cDowned! Time left: " + (data.timeLeft / 20) + "s");
                
                if (data.timeLeft <= 0) {
                    eliminate(player);
                }
            }
        }
    }

    private Player findReviver(Player downed) {
        PartyManager partyManager = plugin.getPartyManager();
        if (!partyManager.isEnabled()) return null;

        PartyPlayer downedPartyPlayer = partyManager.getPartyPlayer(downed.getUniqueId());
        if (downedPartyPlayer == null || !downedPartyPlayer.isInParty()) return null;

        UUID partyId = downedPartyPlayer.getPartyId();
        Location loc = downed.getLocation();

        for (Player p : downed.getWorld().getPlayers()) {
            if (p.equals(downed)) continue;
            
            // Check distance (3 blocks)
            if (p.getLocation().distanceSquared(loc) > 9) continue;
            
            // Check sneaking
            if (!p.isSneaking()) continue;
            
            // Check party
            PartyPlayer pp = partyManager.getPartyPlayer(p.getUniqueId());
            if (pp != null && pp.isInParty() && pp.getPartyId().equals(partyId)) {
                return p;
            }
        }
        return null;
    }

    public void setDowned(Player player) {
        if (isDowned(player)) return;
        
        downedPlayers.put(player.getUniqueId(), new DownedData(MAX_DOWNED_TIME));
        
        player.setHealth(1.0); // 0.5 hearts
        player.setSwimming(true);
        
        Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000));
        Title title = Title.title(
            LegacyComponentSerializer.legacySection().deserialize("§cDOWNED!"), 
            LegacyComponentSerializer.legacySection().deserialize("§7Wait for a teammate to revive you"), 
            times
        );
        player.showTitle(title);
        
        player.sendMessage(LegacyComponentSerializer.legacySection().deserialize("§cYou are downed! Crawl to safety or wait for help."));
    }

    public void revive(Player player) {
        downedPlayers.remove(player.getUniqueId());
        player.setSwimming(false);
        // player.setGliding(false);
        player.setHealth(6.0); // Restore to 3 hearts
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        
        Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(1000));
        Title title = Title.title(
            LegacyComponentSerializer.legacySection().deserialize("§aREVIVED!"), 
            LegacyComponentSerializer.legacySection().deserialize("§7Get back in the fight!"), 
            times
        );
        player.showTitle(title);
    }

    public void eliminate(Player player) {
        downedPlayers.remove(player.getUniqueId());
        player.setMetadata("EXFIL_ELIMINATED", new FixedMetadataValue(plugin, true));
        player.setHealth(0); // Kill the player
    }

    public boolean isDowned(Player player) {
        return downedPlayers.containsKey(player.getUniqueId());
    }

    private void sendActionBar(Player player, String message) {
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(message));
    }

    private String getProgressBar(int current, int max) {
        int totalBars = 20;
        int filledBars = (int) ((double) current / max * totalBars);
        StringBuilder sb = new StringBuilder("§8[§a");
        for (int i = 0; i < totalBars; i++) {
            if (i == filledBars) sb.append("§7");
            sb.append("|");
        }
        sb.append("§8]");
        return sb.toString();
    }

    private static class DownedData {
        int timeLeft;
        int reviveProgress;

        DownedData(int timeLeft) {
            this.timeLeft = timeLeft;
            this.reviveProgress = 0;
        }
    }
}
