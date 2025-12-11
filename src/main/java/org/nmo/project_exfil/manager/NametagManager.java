package org.nmo.project_exfil.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import com.alessiodp.parties.api.interfaces.PartyPlayer;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NametagManager {

    private final ProjectEXFILPlugin plugin;
    private final PartyManager partyManager;

    private static final String TEAM_ALLY = "998_ALLY";
    private static final String TEAM_ENEMY = "999_ENEMY";

    public NametagManager(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
        this.partyManager = plugin.getPartyManager();
        
        // Start periodic update task to ensure tags stay correct
        // We run this synchronously to safely access Bukkit/Parties API
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 20L, 40L); // Every 2 seconds
    }

    public void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateNametags(player);
        }
    }

    public void updateNametags(Player viewer) {
        if (!partyManager.isEnabled()) return;

        List<String> allies = new ArrayList<>();
        List<String> enemies = new ArrayList<>();

        PartyPlayer viewerPartyPlayer = partyManager.getPartyPlayer(viewer.getUniqueId());
        UUID viewerPartyId = (viewerPartyPlayer != null && viewerPartyPlayer.isInParty()) ? viewerPartyPlayer.getPartyId() : null;

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(viewer)) continue;

            boolean isAlly = false;
            if (viewerPartyId != null) {
                PartyPlayer targetPartyPlayer = partyManager.getPartyPlayer(target.getUniqueId());
                if (targetPartyPlayer != null && targetPartyPlayer.isInParty() && targetPartyPlayer.getPartyId().equals(viewerPartyId)) {
                    isAlly = true;
                }
            }

            if (isAlly) {
                allies.add(target.getName());
            } else {
                enemies.add(target.getName());
            }
        }

        // Use Bukkit Scoreboard API instead of ProtocolLib to avoid packet errors
        updateTeam(viewer, TEAM_ALLY, NamedTextColor.GREEN, Team.OptionStatus.ALWAYS, allies);
        updateTeam(viewer, TEAM_ENEMY, NamedTextColor.WHITE, Team.OptionStatus.NEVER, enemies);
    }

    private void updateTeam(Player viewer, String teamName, NamedTextColor color, Team.OptionStatus visibility, List<String> players) {
        Scoreboard board = viewer.getScoreboard();
        
        // Ensure player has a private scoreboard (ScoreboardManager also does this, but safety first)
        if (board.equals(Bukkit.getScoreboardManager().getMainScoreboard())) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            viewer.setScoreboard(board);
        }

        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }

        // Update team properties
        if (!team.hasColor() || !team.color().equals(color)) {
            team.color(color);
        }
        
        if (team.getOption(Team.Option.NAME_TAG_VISIBILITY) != visibility) {
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, visibility);
        }
        
        if (team.getOption(Team.Option.COLLISION_RULE) != Team.OptionStatus.NEVER) {
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }

        // Update players
        // Remove players not in the list
        for (String entry : new ArrayList<>(team.getEntries())) {
            if (!players.contains(entry)) {
                team.removeEntry(entry);
            }
        }
        // Add players in the list
        for (String player : players) {
            if (!team.hasEntry(player)) {
                team.addEntry(player);
            }
        }
    }
}
