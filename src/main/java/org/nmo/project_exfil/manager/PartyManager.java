package org.nmo.project_exfil.manager;

import com.alessiodp.parties.api.Parties;
import com.alessiodp.parties.api.interfaces.PartiesAPI;
import com.alessiodp.parties.api.interfaces.Party;
import com.alessiodp.parties.api.interfaces.PartyPlayer;
import org.bukkit.Bukkit;
import org.nmo.project_exfil.ProjectEXFILPlugin;

import java.util.UUID;

public class PartyManager {

    private PartiesAPI api;
    private boolean enabled = false;

    public PartyManager(ProjectEXFILPlugin plugin) {
        if (Bukkit.getPluginManager().getPlugin("Parties") != null && Bukkit.getPluginManager().getPlugin("Parties").isEnabled()) {
            this.api = Parties.getApi();
            this.enabled = true;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public PartyPlayer getPartyPlayer(UUID uuid) {
        if (!enabled) return null;
        return api.getPartyPlayer(uuid);
    }

    public Party getParty(UUID partyId) {
        if (!enabled) return null;
        return api.getParty(partyId);
    }

    public boolean createParty(String name, PartyPlayer leader) {
        if (!enabled) return false;
        return api.createParty(name, leader);
    }

    public void deleteParty(Party party) {
        if (!enabled) return;
        party.delete();
    }

    public void removePlayerFromParty(PartyPlayer player) {
        if (!enabled) return;
        if (player.isInParty()) {
            Party party = api.getParty(player.getPartyId());
            if (party != null) {
                party.removeMember(player);
            }
        }
    }

    public boolean invitePlayer(Party party, PartyPlayer target) {
        if (!enabled) return false;
        if (party.getMembers().size() >= 3) {
            return false;
        }
        party.invitePlayer(target);
        return true;
    }
    
    public PartiesAPI getApi() {
        return api;
    }
}
