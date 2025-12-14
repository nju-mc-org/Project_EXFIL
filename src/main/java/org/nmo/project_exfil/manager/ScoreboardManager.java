package org.nmo.project_exfil.manager;

import org.nmo.project_exfil.util.DependencyHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.nmo.project_exfil.ProjectEXFILPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public class ScoreboardManager {

    private final ProjectEXFILPlugin plugin;
    private final Map<UUID, Long> combatStartTimes = new HashMap<>();
    private final Map<UUID, CachedStatic> staticCache = new HashMap<>();
    private static final long STATIC_CACHE_MS = 5000L;

    private static class CachedStatic {
        final long ts;
        final String balanceStr;
        final String mapStr;
        final String rankStr;

        CachedStatic(long ts, String balanceStr, String mapStr, String rankStr) {
            this.ts = ts;
            this.balanceStr = balanceStr;
            this.mapStr = mapStr;
            this.rankStr = rankStr;
        }
    }

    public ScoreboardManager(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
        startScoreboardTask();
    }

    public void startCombat(Player player) {
        combatStartTimes.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void endCombat(Player player) {
        combatStartTimes.remove(player.getUniqueId());
        // Force update to clear the timer immediately
        updateScoreboard(player);
    }

    private void startScoreboardTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateScoreboard(player);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void updateScoreboard(Player player) {
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = player.getScoreboard();
        if (board.equals(manager.getMainScoreboard())) {
            board = manager.getNewScoreboard();
            player.setScoreboard(board);
        }

        Objective objective = board.getObjective("exfil_board");
        if (objective == null) {
            objective = board.registerNewObjective("exfil_board", Criteria.DUMMY, plugin.getLanguageManager().getMessage("exfil.scoreboard.title"));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        long now = System.currentTimeMillis();
        CachedStatic cached = staticCache.get(player.getUniqueId());
        if (cached == null || (now - cached.ts) > STATIC_CACHE_MS) {
            String balanceStr;
            if (DependencyHelper.isXConomyEnabled()) {
                balanceStr = "$" + String.format("%.2f", DependencyHelper.getBalance(player));
            } else {
                balanceStr = DependencyHelper.parsePlaceholders(player, "%vaultunlocked_balanceformatted%");
            }

            String mapStr = DependencyHelper.getWorldAlias(player.getWorld());
            String rank = DependencyHelper.getPlayerGroup(player);
            cached = new CachedStatic(now, balanceStr, mapStr, rank);
            staticCache.put(player.getUniqueId(), cached);
        }

        // Balance
        updateTeam(board, objective, "balance", plugin.getLanguageManager().getMessage("exfil.scoreboard.balance"), Component.text(cached.balanceStr, NamedTextColor.GREEN), 4);

        // Map
        updateTeam(board, objective, "map", plugin.getLanguageManager().getMessage("exfil.scoreboard.map"), Component.text(cached.mapStr, NamedTextColor.YELLOW), 3);

        // Rank
        updateTeam(board, objective, "rank", plugin.getLanguageManager().getMessage("exfil.scoreboard.rank"), Component.text(cached.rankStr, NamedTextColor.AQUA), 5);

        // Combat Time
        String timeStr;
        NamedTextColor timeColor;
        if (combatStartTimes.containsKey(player.getUniqueId())) {
            long startTime = combatStartTimes.get(player.getUniqueId());
            long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
            long minutes = elapsedSeconds / 60;
            long seconds = elapsedSeconds % 60;
            timeStr = String.format("%02d:%02d", minutes, seconds);
            timeColor = NamedTextColor.RED;
        } else {
            timeStr = "--:--";
            timeColor = NamedTextColor.GRAY;
        }
        updateTeam(board, objective, "raidtime", plugin.getLanguageManager().getMessage("exfil.scoreboard.raid_time"), Component.text(timeStr, timeColor), 2);
        
        String footerStr = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(plugin.getLanguageManager().getMessage("exfil.scoreboard.footer"));
        Score footerScore = objective.getScore(footerStr);
        footerScore.setScore(1);
    }
 
    private void updateTeam(Scoreboard board, Objective objective, String teamName, Component prefix, Component suffix, int score) {
        Team team = board.getTeam(teamName);
        String entry = "§" + Integer.toHexString(score);
        
        if (team == null) {
            team = board.registerNewTeam(teamName);
            team.addEntry(entry);
        }
        
        // Ensure the score is set
        if (!objective.getScore(entry).isScoreSet()) {
            objective.getScore(entry).setScore(score);
        }

        team.prefix(prefix);
        team.suffix(suffix);
    }

}
