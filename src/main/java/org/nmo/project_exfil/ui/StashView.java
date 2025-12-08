package org.nmo.project_exfil.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.nmo.project_exfil.manager.StashManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public class StashView implements Listener {

    private final StashManager stashManager;
    private final org.nmo.project_exfil.ProjectEXFILPlugin plugin;

    public StashView(StashManager stashManager) {
        this.stashManager = stashManager;
        this.plugin = org.nmo.project_exfil.ProjectEXFILPlugin.getPlugin();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        // Create a standard Bukkit inventory
        Inventory inventory = Bukkit.createInventory(player, 54, plugin.getLanguageManager().getMessage("exfil.stash.title"));
        
        // Load items
        ItemStack[] stashItems = stashManager.loadStash(player);
        
        if (stashItems != null) {
            inventory.setContents(stashItems);
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Component expectedTitle = plugin.getLanguageManager().getMessage("exfil.stash.title");
        if (event.getView().title().equals(expectedTitle)) {
            if (event.getPlayer() instanceof Player) {
                Player player = (Player) event.getPlayer();
                stashManager.saveStash(player, event.getInventory().getContents());
                plugin.getLanguageManager().send(player, "exfil.stash_saved");
            }
        }
    }
}
