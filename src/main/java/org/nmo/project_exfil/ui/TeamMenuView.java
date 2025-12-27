package org.nmo.project_exfil.ui;

import com.alessiodp.parties.api.interfaces.Party;
import com.alessiodp.parties.api.interfaces.PartyPlayer;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.nmo.project_exfil.ProjectEXFILPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.nmo.project_exfil.manager.PartyManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public class TeamMenuView {

    private final MainMenuView mainMenuView;
    private final PlayerSelectionView playerSelectionView;
    private final PartyManager partyManager;
    private final ProjectEXFILPlugin plugin = ProjectEXFILPlugin.getPlugin();

    public TeamMenuView(MainMenuView mainMenuView, PartyManager partyManager) {
        this.mainMenuView = mainMenuView;
        this.partyManager = partyManager;
        this.playerSelectionView = new PlayerSelectionView(this, partyManager);
    }

    public void open(Player player) {
        if (!partyManager.isEnabled()) {
            plugin.getLanguageManager().send(player, "exfil.error.parties_disabled");
            return;
        }

        PartyPlayer partyPlayer = partyManager.getPartyPlayer(player.getUniqueId());

        if (partyPlayer == null || !partyPlayer.isInParty()) {
            openNoPartyView(player, partyPlayer);
        } else {
            openPartyView(player, partyPlayer);
        }
    }

    private void openNoPartyView(Player player, PartyPlayer partyPlayer) {
        ChestGui gui = new ChestGui(3, "Squad Management");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        StaticPane pane = new StaticPane(0, 0, 9, 3);

        // Create Squad Button
        ItemStack createItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta createMeta = createItem.getItemMeta();
        createMeta.displayName(plugin.getLanguageManager().getMessage("exfil.team.create"));
        createMeta.lore(List.of(plugin.getLanguageManager().getMessage("exfil.team.click_create")));
        createItem.setItemMeta(createMeta);

        pane.addItem(new GuiItem(createItem, event -> {
            gui.getInventory().close();
            // Auto-generate name: "Player's Squad"
            String partyName = player.getName() + "'s Squad";
            // Check if name exists or just try create
            boolean success = partyManager.createParty(partyName, partyPlayer);
            if (success) {
                plugin.getLanguageManager().send(player, "exfil.party_create");
                // Re-open view
                Bukkit.getScheduler().runTaskLater(ProjectEXFILPlugin.getPlugin(), () -> open(player), 5L);
            } else {
                plugin.getLanguageManager().send(player, "exfil.team.create_fail");
            }
        }), 4, 1);

        // Back Button
        addBackButton(pane, player, 0, 2);

        gui.addPane(pane);
        gui.show(player);
    }

    private void openPartyView(Player player, PartyPlayer partyPlayer) {
        ChestGui gui = new ChestGui(3, "Squad: " + partyPlayer.getPartyName());
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        StaticPane pane = new StaticPane(0, 0, 9, 3);
        Party party = partyManager.getParty(partyPlayer.getPartyId());
        
        if (party == null) {
            plugin.getLanguageManager().send(player, "exfil.team.error_not_found");
            return;
        }

        List<UUID> members = new ArrayList<>(party.getMembers());
        // Ensure current player is in the center or list logic
        // Let's just list them in slots: 10, 11, 12, 13, 14, 15, 16 (Row 2)
        // Or 5 slots: 11, 12, 13, 14, 15
        
        // Max party size usually 5 for FPS squads? Let's assume 5.
        int[] slots = {12, 13, 14};
        
        for (int i = 0; i < slots.length; i++) {
            if (i < members.size()) {
                // Show Member
                UUID memberUUID = members.get(i);
                OfflinePlayer memberOffline = Bukkit.getOfflinePlayer(memberUUID);
                
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                meta.setOwningPlayer(memberOffline);
                
                String name = memberOffline.getName() != null ? memberOffline.getName() : "Unknown";
                boolean isLeader = party.getLeader().equals(memberUUID);
                Component role = isLeader ? plugin.getLanguageManager().getMessage("exfil.team.role.leader") : plugin.getLanguageManager().getMessage("exfil.team.role.member");
                
                meta.displayName(Component.text(name, NamedTextColor.YELLOW));
                List<Component> lore = new ArrayList<>();
                lore.add(role);
                
                boolean isSelf = memberUUID.equals(player.getUniqueId());
                
                if (isLeader && !isSelf) {
                    lore.add(plugin.getLanguageManager().getMessage("exfil.team.click_kick"));
                }
                
                meta.lore(lore);
                head.setItemMeta(meta);
                
                pane.addItem(new GuiItem(head, event -> {
                    if (isLeader && !isSelf) {
                        gui.getInventory().close();
                        partyManager.removePlayerFromParty(partyManager.getPartyPlayer(memberUUID));
                        plugin.getLanguageManager().send(player, "exfil.party_kick", Placeholder.unparsed("player", name));
                        Bukkit.getScheduler().runTaskLater(ProjectEXFILPlugin.getPlugin(), () -> open(player), 5L);
                    }
                }), slots[i] % 9, slots[i] / 9);
                
            } else {
                // Show Invite Button (Empty Slot)
                ItemStack add = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                ItemMeta meta = add.getItemMeta();
                meta.displayName(plugin.getLanguageManager().getMessage("exfil.team.invite"));
                add.setItemMeta(meta);
                
                pane.addItem(new GuiItem(add, event -> {
                    playerSelectionView.open(player);
                }), slots[i] % 9, slots[i] / 9);
            }
        }

        // Leave / Disband Button
        boolean isLeader = party.getLeader().equals(player.getUniqueId());
        ItemStack actionItem = new ItemStack(isLeader ? Material.TNT : Material.RED_BED);
        ItemMeta actionMeta = actionItem.getItemMeta();
        actionMeta.displayName(isLeader ? plugin.getLanguageManager().getMessage("exfil.team.disband") : plugin.getLanguageManager().getMessage("exfil.team.leave"));
        actionItem.setItemMeta(actionMeta);
        
        pane.addItem(new GuiItem(actionItem, event -> {
            gui.getInventory().close();
            if (isLeader) {
                partyManager.deleteParty(party);
                plugin.getLanguageManager().send(player, "exfil.party_disband");
            } else {
                partyManager.removePlayerFromParty(partyPlayer);
                plugin.getLanguageManager().send(player, "exfil.party_leave");
            }
            Bukkit.getScheduler().runTaskLater(ProjectEXFILPlugin.getPlugin(), () -> open(player), 5L);
        }), 4, 2);

        // Back Button
        addBackButton(pane, player, 0, 2);

        gui.addPane(pane);
        gui.show(player);
    }

    private void addBackButton(StaticPane pane, Player player, int x, int y) {
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.displayName(plugin.getLanguageManager().getMessage("exfil.team.back"));
        backItem.setItemMeta(backMeta);

        pane.addItem(new GuiItem(backItem, event -> {
            mainMenuView.open(player);
        }), x, y);
    }
}
