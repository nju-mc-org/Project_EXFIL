package org.nmo.project_exfil.integration.itemsadder;

import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nmo.project_exfil.ProjectEXFILPlugin;

/**
 * ItemsAdder loads asynchronously; wait for ItemsAdderLoadDataEvent before using API.
 */
public final class ItemsAdderIntegration implements Listener {

    private static volatile boolean ready = false;

    public static boolean isReady() {
        return ready;
    }

    public static void init(ProjectEXFILPlugin plugin) {
        if (ready) return;

        Bukkit.getPluginManager().registerEvents(new ItemsAdderIntegration(), plugin);
        
        // ItemsAdder loads asynchronously, wait for ItemsAdderLoadDataEvent
        // The deprecated areItemsLoaded() method is replaced by listening to the event
    }

    @EventHandler
    public void onItemsAdderLoad(ItemsAdderLoadDataEvent event) {
        ready = true;
        ProjectEXFILPlugin plugin = ProjectEXFILPlugin.getPlugin();
        if (plugin != null) {
            plugin.getLogger().info("ItemsAdder items loaded.");
        }
    }
}
