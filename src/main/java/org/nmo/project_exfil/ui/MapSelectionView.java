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

public class MapSelectionView {

    private final GameManager gameManager;

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
        zeroDamMeta.setDisplayName("§6Zero Dam");
        zeroDamMeta.setLore(List.of("§7Difficulty: §cHard", "§7Click to Deploy"));
        zeroDamItem.setItemMeta(zeroDamMeta);

        pane.addItem(new GuiItem(zeroDamItem, event -> {
            player.sendMessage("§aMatchmaking: Zero Dam...");
            gui.getInventory().close();
            gameManager.joinQueue(player, "Zero-Dam");
        }), 2, 1); // Slot 11 (row 2, col 3 -> x=2, y=1)

        // Map B: Longbow Valley
        ItemStack longbowItem = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta longbowMeta = longbowItem.getItemMeta();
        longbowMeta.setDisplayName("§aLongbow Valley");
        longbowMeta.setLore(List.of("§7Difficulty: §eNormal", "§7Click to Deploy"));
        longbowItem.setItemMeta(longbowMeta);

        pane.addItem(new GuiItem(longbowItem, event -> {
            player.sendMessage("§aMatchmaking: Longbow Valley...");
            gui.getInventory().close();
            gameManager.joinQueue(player, "Longbow-Valley");
        }), 6, 1); // Slot 15 (row 2, col 7 -> x=6, y=1)

        gui.addPane(pane);
        gui.show(player);
    }
}
