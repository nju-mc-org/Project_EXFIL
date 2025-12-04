package org.nmo.project_exfil.ChestUIHelper;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.nmo.project_exfil.ProjectEXFILPlugin;

import java.util.Map;

public class ChestUIHelper implements Listener {

    private Map<String, ChestUI> ChestUI;

    public ChestUIHelper() {
        ProjectEXFILPlugin plugin = ProjectEXFILPlugin.getPlugin();
        if (plugin == null) { return; }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Event handler for when a button is clicked in the chest UI
     * @param event The event that was triggered
     */
    @EventHandler
    public void onUIButtonClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        Player player = (Player) event.getWhoClicked();

    }

    /**
     * Closes the player's inventory next tick
     * @param player The player whose inventory should be closed
     */
    public static void closeInventoryNextTick(@NotNull Player player) {
        ProjectEXFILPlugin plugin = ProjectEXFILPlugin.getPlugin();
        if (plugin == null) { return; }
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                player.closeInventory();
            }
        };
        task.runTaskLater(plugin, 1L);
    }
}
