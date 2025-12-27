package org.nmo.project_exfil.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import com.alessiodp.parties.api.interfaces.PartyPlayer;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;

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
        if (!partyManager.isEnabled()) return;

        java.util.List<Player> online = new java.util.ArrayList<>(Bukkit.getOnlinePlayers());
        java.util.Map<java.util.UUID, java.util.UUID> partyIds = new java.util.HashMap<>();
        for (Player p : online) {
            PartyPlayer pp = partyManager.getPartyPlayer(p.getUniqueId());
            partyIds.put(p.getUniqueId(), (pp != null && pp.isInParty()) ? pp.getPartyId() : null);
        }

        for (Player viewer : online) {
            if (plugin.getGameManager().getPlayerInstance(viewer) == null) continue; // only update raid players
            updateNametags(viewer, online, partyIds);
        }
    }

    public void updateNametags(Player viewer) {
        updateNametags(viewer, new java.util.ArrayList<>(Bukkit.getOnlinePlayers()), null);
    }

    private void updateNametags(Player viewer, java.util.List<Player> online, java.util.Map<java.util.UUID, java.util.UUID> partyIds) {
        if (!partyManager.isEnabled()) return;

        java.util.UUID viewerPartyId = null;
        if (partyIds != null) {
            viewerPartyId = partyIds.get(viewer.getUniqueId());
        } else {
            PartyPlayer viewerPartyPlayer = partyManager.getPartyPlayer(viewer.getUniqueId());
            viewerPartyId = (viewerPartyPlayer != null && viewerPartyPlayer.isInParty()) ? viewerPartyPlayer.getPartyId() : null;
        }

        java.util.Set<String> allies = new java.util.HashSet<>();
        java.util.Set<String> enemies = new java.util.HashSet<>();

        for (Player target : online) {
            if (target.equals(viewer)) continue;

            boolean isAlly = false;
            if (viewerPartyId != null) {
                java.util.UUID targetPartyId = (partyIds != null) ? partyIds.get(target.getUniqueId()) : null;
                if (targetPartyId == null && partyIds == null) {
                    PartyPlayer targetPartyPlayer = partyManager.getPartyPlayer(target.getUniqueId());
                    targetPartyId = (targetPartyPlayer != null && targetPartyPlayer.isInParty()) ? targetPartyPlayer.getPartyId() : null;
                }
                isAlly = viewerPartyId.equals(targetPartyId);
            }

            if (isAlly) allies.add(target.getName());
            else enemies.add(target.getName());
        }

        // Use Bukkit Scoreboard API instead of ProtocolLib to avoid packet errors
        updateTeam(viewer, TEAM_ALLY, NamedTextColor.GREEN, Team.OptionStatus.ALWAYS, allies);
        updateTeam(viewer, TEAM_ENEMY, NamedTextColor.WHITE, Team.OptionStatus.NEVER, enemies);
    }

    private void updateTeam(Player viewer, String teamName, NamedTextColor color, Team.OptionStatus visibility, java.util.Set<String> players) {
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
        for (String entry : new ArrayList<>(team.getEntries())) {
            if (!players.contains(entry)) {
                team.removeEntry(entry);
            }
        }
        for (String player : players) {
            if (!team.hasEntry(player)) {
                team.addEntry(player);
            }
        }
    }
}
