package org.nmo.project_exfil.manager;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.EnumWrappers.ChatFormatting;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import com.alessiodp.parties.api.interfaces.PartyPlayer;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class NametagManager {

    private final ProjectEXFILPlugin plugin;
    private final ProtocolManager protocolManager;
    private final PartyManager partyManager;

    private static final String TEAM_ALLY = "998_ALLY";
    private static final String TEAM_ENEMY = "999_ENEMY";

    public NametagManager(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
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

        // Send packets
        // We use Mode 0 (Create) to ensure the team exists and has correct settings.
        // If it already exists, the client usually updates it.
        sendTeamPacket(viewer, TEAM_ALLY, NamedTextColor.GREEN, "always", allies);
        sendTeamPacket(viewer, TEAM_ENEMY, NamedTextColor.WHITE, "never", enemies);
    }

    private void sendTeamPacket(Player viewer, String teamName, NamedTextColor color, String visibility, List<String> players) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
        packet.getModifier().writeDefaults();
        
        packet.getStrings().write(0, teamName);
        packet.getIntegers().write(0, 0); // Mode 0: Create

        // Set Color
        try {
            if (packet.getSpecificModifier(ChatFormatting.class).size() > 0) {
                try {
                    ChatFormatting format = ChatFormatting.valueOf(color.toString().toUpperCase());
                    packet.getSpecificModifier(ChatFormatting.class).write(0, format);
                } catch (IllegalArgumentException e) {
                    // Fallback or ignore if color doesn't match
                    packet.getSpecificModifier(ChatFormatting.class).write(0, ChatFormatting.WHITE);
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        
        // Set Visibility and Collision
        // Indices might vary, but usually:
        // 0: Name
        // 1: Visibility
        // 2: Collision
        // 3: Prefix (String in old, but Component in new) - wait, Strings structure usually has Visibility at 1.
        if (packet.getStrings().size() >= 3) {
            packet.getStrings().write(1, visibility);
            packet.getStrings().write(2, "never"); // Collision Rule
        }

        // Set Players
        packet.getSpecificModifier(Collection.class).write(0, players);

        try {
            protocolManager.sendServerPacket(viewer, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
