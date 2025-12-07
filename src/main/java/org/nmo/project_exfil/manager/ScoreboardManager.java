package org.nmo.project_exfil.manager;

import org.nmo.project_exfil.util.DependencyHelper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.nmo.project_exfil.ProjectEXFILPlugin;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {

    private final ProjectEXFILPlugin plugin;
    // private Economy economy;
    private final Map<UUID, Long> combatStartTimes = new HashMap<>();

    public ScoreboardManager(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
        // setupEconomy();
        startScoreboardTask();
    }

    /*
    private void setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
        }
    }
    */

    public void startCombat(Player player) {
        combatStartTimes.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void endCombat(Player player) {
        combatStartTimes.remove(player.getUniqueId());
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
            objective = board.registerNewObjective("exfil_board", Criteria.DUMMY, ChatColor.GOLD + "" + ChatColor.BOLD + "PROJECT EXFIL");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        // Balance
        String balanceStr;
        if (DependencyHelper.isXConomyEnabled()) {
             balanceStr = "$" + String.format("%.2f", DependencyHelper.getBalance(player));
        } else {
             balanceStr = DependencyHelper.parsePlaceholders(player, "%vaultunlocked_balanceformatted%");
        }
        updateTeam(board, objective, "balance", ChatColor.WHITE + "Balance: " + ChatColor.GREEN, balanceStr, 4);

        // Map
        String mapStr = DependencyHelper.getWorldAlias(player.getWorld());
        updateTeam(board, objective, "map", ChatColor.WHITE + "Map: " + ChatColor.YELLOW, mapStr, 3);

        // Rank
        String rank = DependencyHelper.getPlayerGroup(player);
        updateTeam(board, objective, "rank", ChatColor.WHITE + "Rank: " + ChatColor.AQUA, rank, 5);

        // Combat Time
        String timeStr;
        ChatColor timeColor;
        if (combatStartTimes.containsKey(player.getUniqueId())) {
            long startTime = combatStartTimes.get(player.getUniqueId());
            long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
            long minutes = elapsedSeconds / 60;
            long seconds = elapsedSeconds % 60;
            timeStr = String.format("%02d:%02d", minutes, seconds);
            timeColor = ChatColor.RED;
        } else {
            timeStr = "--:--";
            timeColor = ChatColor.GRAY;
        }
        updateTeam(board, objective, "raidtime", ChatColor.WHITE + "Raid Time: " + timeColor, timeStr, 2);
        
        Score footer = objective.getScore(ChatColor.GRAY + "nmo.net.cn");
        footer.setScore(1);
    }

    private void updateTeam(Scoreboard board, Objective objective, String teamName, String prefix, String suffix, int score) {
        Team team = board.getTeam(teamName);
        String entry = ChatColor.values()[score].toString();
        
        if (team == null) {
            team = board.registerNewTeam(teamName);
            team.addEntry(entry);
        }
        
        // Ensure the score is set
        if (!objective.getScore(entry).isScoreSet()) {
            objective.getScore(entry).setScore(score);
        }

        team.setPrefix(prefix);
        team.setSuffix(suffix);
    }

}
