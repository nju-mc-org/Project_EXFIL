package org.nmo.project_exfil.ui;

import com.alessiodp.parties.api.interfaces.PartyPlayer;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

import com.alessiodp.parties.api.interfaces.Party;

import org.nmo.project_exfil.manager.PartyManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.nmo.project_exfil.ProjectEXFILPlugin;

public class PlayerSelectionView {

    private final TeamMenuView teamMenuView;
    private final PartyManager partyManager;
    private final ProjectEXFILPlugin plugin = ProjectEXFILPlugin.getPlugin();

    public PlayerSelectionView(TeamMenuView teamMenuView, PartyManager partyManager) {
        this.teamMenuView = teamMenuView;
        this.partyManager = partyManager;
    }

    public void open(Player player) {
        ChestGui gui = new ChestGui(6, "Invite Agent");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        PaginatedPane pages = new PaginatedPane(0, 0, 9, 5);
        List<GuiItem> items = new ArrayList<>();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getUniqueId().equals(player.getUniqueId())) continue;

            PartyPlayer partyPlayer = partyManager.getPartyPlayer(onlinePlayer.getUniqueId());
            // Optional: Filter out players already in a party
            // if (partyPlayer != null && partyPlayer.isInParty()) continue;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(onlinePlayer);
            meta.displayName(Component.text(onlinePlayer.getName(), NamedTextColor.YELLOW));
            
            List<Component> lore = new ArrayList<>();
            if (partyPlayer != null && partyPlayer.isInParty()) {
                lore.add(plugin.getLanguageManager().getMessage("exfil.player_select.already_in_squad"));
            } else {
                lore.add(plugin.getLanguageManager().getMessage("exfil.player_select.click_invite"));
            }
            meta.lore(lore);
            head.setItemMeta(meta);

            items.add(new GuiItem(head, event -> {
                if (partyPlayer != null && partyPlayer.isInParty()) {
                    plugin.getLanguageManager().send(player, "exfil.player_select.target_in_squad");
                    return;
                }
                
                PartyPlayer inviter = partyManager.getPartyPlayer(player.getUniqueId());
                if (inviter != null && inviter.isInParty()) {
                    Party party = partyManager.getParty(inviter.getPartyId());
                    if (party != null) {
                        if (partyManager.invitePlayer(party, partyPlayer)) {
                            plugin.getLanguageManager().send(player, "exfil.party_invite", Placeholder.unparsed("player", onlinePlayer.getName()));
                        } else {
                            plugin.getLanguageManager().send(player, "exfil.party_full");
                        }
                    }
                }
                
                gui.getInventory().close();
                teamMenuView.open(player);
            }));
        }

        pages.populateWithGuiItems(items);
        gui.addPane(pages);

        // Navigation Pane
        StaticPane navPane = new StaticPane(0, 5, 9, 1);
        
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.displayName(plugin.getLanguageManager().getMessage("exfil.team.back"));
        backItem.setItemMeta(backMeta);
        navPane.addItem(new GuiItem(backItem, event -> teamMenuView.open(player)), 0, 0);

        if (pages.getPages() > 1) {
            ItemStack prevItem = new ItemStack(Material.PAPER);
            ItemMeta prevMeta = prevItem.getItemMeta();
            prevMeta.displayName(plugin.getLanguageManager().getMessage("exfil.player_select.prev_page"));
            prevItem.setItemMeta(prevMeta);
            navPane.addItem(new GuiItem(prevItem, event -> {
                if (pages.getPage() > 0) {
                    pages.setPage(pages.getPage() - 1);
                    gui.update();
                }
            }), 3, 0);

            ItemStack nextItem = new ItemStack(Material.PAPER);
            ItemMeta nextMeta = nextItem.getItemMeta();
            nextMeta.displayName(plugin.getLanguageManager().getMessage("exfil.player_select.next_page"));
            nextItem.setItemMeta(nextMeta);
            navPane.addItem(new GuiItem(nextItem, event -> {
                if (pages.getPage() < pages.getPages() - 1) {
                    pages.setPage(pages.getPage() + 1);
                    gui.update();
                }
            }), 5, 0);
        }

        gui.addPane(navPane);
        gui.show(player);
    }
}
