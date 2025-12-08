package org.nmo.project_exfil.ui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nmo.project_exfil.manager.GameManager;

import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.nmo.project_exfil.ProjectEXFILPlugin;

public class MapSelectionView {

    private final GameManager gameManager;
    private final ProjectEXFILPlugin plugin = ProjectEXFILPlugin.getPlugin();

    public MapSelectionView(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void open(Player player) {
        ChestGui gui = new ChestGui(3, "Select Operation Map");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        StaticPane pane = new StaticPane(0, 0, 9, 3);

        // Map A: Zero Dam
        ItemStack zeroDamItem = new ItemStack(Material.MAP);
        ItemMeta zeroDamMeta = zeroDamItem.getItemMeta();
        zeroDamMeta.displayName(plugin.getLanguageManager().getMessage("exfil.map.zero_dam"));
        zeroDamMeta.lore(List.of(
            plugin.getLanguageManager().getMessage("exfil.map.difficulty").append(plugin.getLanguageManager().getMessage("exfil.map.difficulty.hard")),
            plugin.getLanguageManager().getMessage("exfil.map.click_deploy")
        ));
        zeroDamItem.setItemMeta(zeroDamMeta);

        pane.addItem(new GuiItem(zeroDamItem, event -> {
            plugin.getLanguageManager().send(player, "exfil.map.matchmaking", Placeholder.unparsed("map", "Zero Dam"));
            gui.getInventory().close();
            gameManager.joinQueue(player, "Zero-Dam");
        }), 2, 1); // Slot 11 (row 2, col 3 -> x=2, y=1)

        // Map B: Longbow Valley
        ItemStack longbowItem = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta longbowMeta = longbowItem.getItemMeta();
        longbowMeta.displayName(plugin.getLanguageManager().getMessage("exfil.map.longbow_valley"));
        longbowMeta.lore(List.of(
            plugin.getLanguageManager().getMessage("exfil.map.difficulty").append(plugin.getLanguageManager().getMessage("exfil.map.difficulty.normal")),
            plugin.getLanguageManager().getMessage("exfil.map.click_deploy")
        ));
        longbowItem.setItemMeta(longbowMeta);

        pane.addItem(new GuiItem(longbowItem, event -> {
            plugin.getLanguageManager().send(player, "exfil.map.matchmaking", Placeholder.unparsed("map", "Longbow Valley"));
            gui.getInventory().close();
            gameManager.joinQueue(player, "Longbow-Valley");
        }), 6, 1); // Slot 15 (row 2, col 7 -> x=6, y=1)

        gui.addPane(pane);
        gui.show(player);
    }
}
