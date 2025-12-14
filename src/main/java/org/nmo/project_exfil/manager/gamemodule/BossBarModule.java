package org.nmo.project_exfil.manager.gamemodule;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.manager.GameInstance;
import org.nmo.project_exfil.region.ExtractionRegion;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.nmo.project_exfil.manager.LanguageManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class BossBarModule implements GameModule {

    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private BukkitTask bossBarTask;

    @Override
    public void onStart(GameInstance game) {
        this.bossBarTask = Bukkit.getScheduler().runTaskTimer(ProjectEXFILPlugin.getPlugin(), () -> updateBossBars(game), 0L, 10L);
    }

    @Override
    public void onEnd(GameInstance game) {
        if (bossBarTask != null && !bossBarTask.isCancelled()) {
            bossBarTask.cancel();
        }
        for (UUID uuid : bossBars.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.hideBossBar(bossBars.get(uuid));
        }
        bossBars.clear();
    }

    @Override
    public void onPlayerJoin(GameInstance game, Player player) {
        LanguageManager lang = ProjectEXFILPlugin.getPlugin().getLanguageManager();
        BossBar bar = BossBar.bossBar(lang.getMessage("exfil.bossbar.searching"), 1.0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
        player.showBossBar(bar);
        bossBars.put(player.getUniqueId(), bar);
    }

    @Override
    public void onPlayerQuit(GameInstance game, Player player) {
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) player.hideBossBar(bar);
    }

    private void updateBossBars(GameInstance game) {
        Map<String, ExtractionRegion> regions = ProjectEXFILPlugin.getPlugin().getRegionManager().getExtractionRegions(game.getTemplateName());
        if (regions.isEmpty()) return;

        LanguageManager lang = ProjectEXFILPlugin.getPlugin().getLanguageManager();

        for (UUID uuid : game.getPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            BossBar bar = bossBars.get(uuid);
            if (bar == null) continue;

            Location pLoc = p.getLocation();
            ExtractionRegion nearest = null;
            String nearestName = "";
            double minDstSq = Double.MAX_VALUE;

            for (Map.Entry<String, ExtractionRegion> entry : regions.entrySet()) {
                ExtractionRegion r = entry.getValue();
                double cx = r.getBox().getCenterX();
                double cy = r.getBox().getCenterY();
                double cz = r.getBox().getCenterZ();

                double dx = pLoc.getX() - cx;
                double dy = pLoc.getY() - cy;
                double dz = pLoc.getZ() - cz;
                double dstSq = dx * dx + dy * dy + dz * dz;

                if (dstSq < minDstSq) {
                    minDstSq = dstSq;
                    nearest = r;
                    nearestName = entry.getKey();
                }
            }

            if (nearest != null) {
                double cx = nearest.getBox().getCenterX();
                double cy = nearest.getBox().getCenterY();
                double cz = nearest.getBox().getCenterZ();
                
                double dist = Math.sqrt(minDstSq);
                
                // Calculate relative angle for compass
                double dx = cx - pLoc.getX();
                double dz = cz - pLoc.getZ();
                double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
                double playerYaw = pLoc.getYaw();
                double relativeAngle = targetYaw - playerYaw;
                
                // Normalize to -180 to 180
                while (relativeAngle > 180) relativeAngle -= 360;
                while (relativeAngle < -180) relativeAngle += 360;
                
                // Create visual compass bar
                // Range: -60 to +60 degrees visible? Or full 360?
                // Let's do a simple bar: [ . . . . * . . . . ]
                // where * moves based on relative angle.
                // If angle is 0, * is in center.
                // If angle is -90 (left), * is to the left.
                
                String compass = getCompassBar(relativeAngle);
                
                String vertKey = "";
                double dy = cy - pLoc.getY();
                if (dy > 5) vertKey = "exfil.bossbar.vert.up";
                else if (dy < -5) vertKey = "exfil.bossbar.vert.down";
                
                Component vertComp = vertKey.isEmpty() ? Component.empty() : lang.getMessage(vertKey);
                
                Component title = lang.getMessage("exfil.bossbar.format",
                    Placeholder.unparsed("name", nearestName),
                    Placeholder.unparsed("dist", String.format("%.1f", dist)),
                    Placeholder.component("dir", LegacyComponentSerializer.legacySection().deserialize(compass)),
                    Placeholder.component("vert", vertComp)
                );
                
                bar.name(title);
                
                if (dist < 10) {
                    bar.color(BossBar.Color.GREEN);
                } else {
                    bar.color(BossBar.Color.BLUE);
                }
            }
        }
    }

    private String getCompassBar(double relativeAngle) {
        // Bar length: 21 chars (odd number for center)
        // Center index: 10
        // Visible range: +/- 90 degrees?
        
        int halfLen = 15;
        int len = halfLen * 2 + 1;
        StringBuilder sb = new StringBuilder();
        
        // Map angle (-180 to 180) to position (0 to len-1)
        // But we want to show direction relative to center.
        // If angle is 0, pos is center.
        // If angle is -90, pos is center - halfLen/2?
        
        // Let's clamp angle to visible range for the bar
        double visibleRange = 120.0; // +/- 60 degrees
        
        if (Math.abs(relativeAngle) > visibleRange) {
            // Out of view
            sb.append("§8[");
            for (int i = 0; i < len; i++) sb.append("-");
            sb.append("]");
            // Add arrow indicating direction?
            if (relativeAngle > 0) return "§8[§7<-- " + sb.toString().substring(5) + "§8]"; // Target is right (angle > 0 means right in Minecraft yaw?)
            // Wait, Minecraft Yaw: South=0, West=90, North=180/-180, East=-90
            // atan2(-dx, dz) gives: 0 for South (dx=0, dz=1), 90 for West (dx=-1, dz=0), etc.
            // So targetYaw follows Minecraft convention.
            // relative = target - player.
            // If player facing South (0), target West (90). relative = 90. Target is to the RIGHT.
            // So positive angle = Right.
            
            if (relativeAngle > 0) return "§8[" + "§7".repeat(len) + "§e»§8]"; // Right
            else return "§8[§e«" + "§7".repeat(len) + "§8]"; // Left
        }
        
        // Calculate position
        // -visibleRange -> 0
        // 0 -> halfLen
        // +visibleRange -> len-1
        
        double ratio = (relativeAngle + visibleRange) / (visibleRange * 2);
        int pos = (int) (ratio * len);
        pos = Math.max(0, Math.min(len - 1, pos));
        
        sb.append("§8[");
        for (int i = 0; i < len; i++) {
            if (i == pos) {
                sb.append("§e●"); // The moving point
            } else if (i == halfLen) {
                sb.append("§7|"); // Center marker
            } else {
                sb.append("§8-");
            }
        }
        sb.append("§8]");
        
        return sb.toString();
    }

    private String getCardinalDirectionKey(double x1, double z1, double x2, double z2) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        double angleDeg = Math.toDegrees(Math.atan2(dx, -dz));
        if (angleDeg < 0) angleDeg += 360;
        
        String[] keys = {
            "exfil.bossbar.dir.n", "exfil.bossbar.dir.ne", "exfil.bossbar.dir.e", "exfil.bossbar.dir.se", 
            "exfil.bossbar.dir.s", "exfil.bossbar.dir.sw", "exfil.bossbar.dir.w", "exfil.bossbar.dir.nw"
        };
        int index = (int) Math.round(angleDeg / 45.0) % 8;
        return keys[index];
    }
}
