package org.nmo.project_exfil.ui;

import com.alessiodp.parties.api.Parties;
import com.alessiodp.parties.api.interfaces.PartiesAPI;
import com.alessiodp.parties.api.interfaces.PartyPlayer;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

import org.nmo.project_exfil.manager.PartyManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.nmo.project_exfil.ProjectEXFILPlugin;

public class MainMenuView {

    private final MapSelectionView mapSelectionView;
    private final PartyManager partyManager;
    private final StashView stashView;
    private TeamMenuView teamMenuView;
    private final ProjectEXFILPlugin plugin = ProjectEXFILPlugin.getPlugin();

    public MainMenuView(MapSelectionView mapSelectionView, PartyManager partyManager, StashView stashView) {
        this.mapSelectionView = mapSelectionView;
        this.partyManager = partyManager;
        this.stashView = stashView;
    }

    public void setTeamMenuView(TeamMenuView teamMenuView) {
        this.teamMenuView = teamMenuView;
    }

    public void open(Player player) {
        ChestGui gui = new ChestGui(5, "Main Menu - EXFIL");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        StaticPane pane = new StaticPane(0, 0, 9, 5);

        // 第一行 - 主要功能
        // Deploy Button
        ItemStack deployItem = new ItemStack(Material.IRON_SWORD);
        ItemMeta deployMeta = deployItem.getItemMeta();
        deployMeta.displayName(plugin.getLanguageManager().getMessage("exfil.menu.deploy"));
        deployMeta.lore(List.of(plugin.getLanguageManager().getMessage("exfil.menu.click_select_map")));
        deployItem.setItemMeta(deployMeta);

        pane.addItem(new GuiItem(deployItem, event -> {
            mapSelectionView.open(player);
        }), 1, 1);

        // Team Button
        ItemStack teamItem = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta teamMeta = teamItem.getItemMeta();
        teamMeta.displayName(plugin.getLanguageManager().getMessage("exfil.menu.team"));
        
        Component statusComp = plugin.getLanguageManager().getMessage("exfil.menu.status.not_in_party");
        if (partyManager.isEnabled()) {
             try {
                 PartyPlayer partyPlayer = partyManager.getPartyPlayer(player.getUniqueId());
                 if (partyPlayer != null && partyPlayer.isInParty()) {
                     statusComp = plugin.getLanguageManager().getMessage("exfil.menu.status.in_party", Placeholder.unparsed("party", partyPlayer.getPartyName()));
                 }
             } catch (Exception e) {
                 statusComp = plugin.getLanguageManager().getMessage("exfil.menu.status.error");
                 e.printStackTrace();
             }
        }
        
        teamMeta.lore(List.of(
            statusComp,
            plugin.getLanguageManager().getMessage("exfil.menu.click_manage_team")
        ));
        teamItem.setItemMeta(teamMeta);

        pane.addItem(new GuiItem(teamItem, event -> {
            gui.getInventory().close();
            if (teamMenuView != null) {
                teamMenuView.open(player);
            } else {
                openPartiesInterface(player);
            }
        }), 4, 1);

        // Stash Button
        ItemStack stashItem = new ItemStack(Material.CHEST);
        ItemMeta stashMeta = stashItem.getItemMeta();
        stashMeta.displayName(plugin.getLanguageManager().getMessage("exfil.menu.stash"));
        stashMeta.lore(List.of(plugin.getLanguageManager().getMessage("exfil.menu.open_stash")));
        stashItem.setItemMeta(stashMeta);

        pane.addItem(new GuiItem(stashItem, event -> {
            gui.getInventory().close();
            stashView.open(player);
        }), 7, 1);

        gui.addPane(pane);
        gui.show(player);
    }

    private void openPartiesInterface(Player player) {
        if (!partyManager.isEnabled()) {
            plugin.getLanguageManager().send(player, "exfil.error.parties_disabled");
            return;
        }
        player.performCommand("party");
    }
}
