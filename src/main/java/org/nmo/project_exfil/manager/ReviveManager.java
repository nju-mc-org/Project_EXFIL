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
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReviveManager {

    private static final int TASK_INTERVAL_TICKS = 5;

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
        }.runTaskTimer(plugin, TASK_INTERVAL_TICKS, TASK_INTERVAL_TICKS);
    }

    private void tick() {
        java.util.Iterator<Map.Entry<UUID, DownedData>> it = downedPlayers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, DownedData> entry = it.next();
            UUID uuid = entry.getKey();
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                it.remove();
                continue;
            }

            DownedData data = entry.getValue();

            // Apply effects - 使用更稳定的方式
            int effDuration = (int) (TASK_INTERVAL_TICKS * 4);
            // 使用GLIDING状态代替SWIMMING，更稳定
            if (!player.isGliding()) {
                try {
                    player.setGliding(true);
                } catch (Exception e) {
                    // If gliding fails, we can't use deprecated setSwimming
                    // The player will remain in normal state
                }
            }
            // 添加效果 - 使用更合理的等级
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, effDuration, 3, false, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, effDuration, 1, false, false, false));
            // 移除失明效果，改为使用屏幕效果提示
            // player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, effDuration, 0, false, false));

            // Check for reviver
            Player reviver = findReviver(player);

            if (reviver != null) {
                // Being revived
                data.reviveProgress += TASK_INTERVAL_TICKS;
                
                // Notify
                sendActionBar(player, "§aBeing revived: " + getProgressBar(data.reviveProgress, REVIVE_TIME));
                sendActionBar(reviver, "§aReviving " + player.getName() + ": " + getProgressBar(data.reviveProgress, REVIVE_TIME));
                
                if (data.reviveProgress >= REVIVE_TIME) {
                    revive(player);
                }
            } else {
                // Not being revived
                data.reviveProgress = 0; // Reset progress if interrupted
                data.timeLeft -= TASK_INTERVAL_TICKS;
                
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
        
        // 设置生命值为1.0（0.5颗心）
        player.setHealth(1.0);
        
        // 使用GLIDING状态（更稳定）
        try {
            player.setGliding(true);
        } catch (Exception e) {
            // If gliding fails, we can't use deprecated setSwimming
            // The player will remain in normal state
        }
        
        // 立即应用效果
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 3, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1, false, false, false));
        
        // 禁用飞行（如果启用）
        if (player.getAllowFlight()) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
        
        // 显示标题
        Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000));
        Title title = Title.title(
            LegacyComponentSerializer.legacySection().deserialize("§c§l濒死状态"), 
            LegacyComponentSerializer.legacySection().deserialize("§7等待队友救援或使用医疗包"), 
            times
        );
        player.showTitle(title);
        
        // 发送消息
        player.sendMessage(LegacyComponentSerializer.legacySection().deserialize("§c§l你已进入濒死状态！"));
        player.sendMessage(LegacyComponentSerializer.legacySection().deserialize("§7- 移动速度大幅降低"));
        player.sendMessage(LegacyComponentSerializer.legacySection().deserialize("§7- 无法攻击"));
        player.sendMessage(LegacyComponentSerializer.legacySection().deserialize("§7- 等待队友救援或使用医疗包"));
        
        // 播放声音效果
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_HURT, 1.0f, 0.5f);
    }

    public void revive(Player player) {
        downedPlayers.remove(player.getUniqueId());
        
        // 恢复状态
        try {
            player.setGliding(false);
        } catch (Exception e) {
            // setSwimming is deprecated, but gliding should work
            // If gliding fails, the player will naturally stop gliding
        }
        
        // 恢复生命值到3颗心
        player.setHealth(6.0);
        
        // 移除所有效果
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        
        // 添加短暂的治疗效果
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0, false, false, false));
        
        // 显示标题
        Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(1000));
        Title title = Title.title(
            LegacyComponentSerializer.legacySection().deserialize("§a§l已恢复！"), 
            LegacyComponentSerializer.legacySection().deserialize("§7继续战斗！"), 
            times
        );
        player.showTitle(title);
        
        // 播放声音
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
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
