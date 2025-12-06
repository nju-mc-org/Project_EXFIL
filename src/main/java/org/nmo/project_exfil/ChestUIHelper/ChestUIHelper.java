package org.nmo.project_exfil.ChestUIHelper;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.nmo.project_exfil.ChestUIHelper.UIStates.ChestUIState;
import org.nmo.project_exfil.ProjectEXFILPlugin;

import java.util.ArrayList;
import java.util.List;

public class ChestUIHelper implements Listener {
    private long updateInterval = 5;
    private final List<ChestUIStateStack> uiStateStacks = new ArrayList<>();

    public ChestUIHelper() {
        ProjectEXFILPlugin plugin = ProjectEXFILPlugin.getPlugin();
        if (plugin == null) { return; }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        YamlConfiguration config = ProjectEXFILPlugin.getDefaultConfig();
        if (config == null) { return; }
        if (config.contains("chest-ui-update-interval")) {
            this.updateInterval = config.getLong("chest-ui-update-interval");
        }

        // General update task for all UI
        BukkitRunnable updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (ChestUIStateStack stateStack : uiStateStacks) {
                    stateStack.update(updateInterval);
                }
                uiStateStacks.removeIf(stateStack -> {
                    if (stateStack.isEmpty()) {
                        if (stateStack.getPlayer() != null) {
                            closeInventoryLater(stateStack.getPlayer());
                        }
                        return true;
                    }
                    return false;
                });
            }
        };
        updateTask.runTaskTimer(plugin, 0, this.updateInterval);
    }

    public void createStateForPlayer(@NotNull Player player, @NotNull ChestUIState state) {
        ChestUIStateStack stateStack = new ChestUIStateStack(player, state);
        uiStateStacks.add(stateStack);
    }

    /**
     * Event handler for when a button is clicked in the chest UI
     * @param event The event that was triggered
     */
    @EventHandler
    public void onItemClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        int slot = event.getSlot();
        for (ChestUIStateStack stateStack : this.uiStateStacks) {
            if (stateStack.matchInventory(inventory)) {
                stateStack.onClick(slot / 9, slot % 9);
                event.setCancelled(true);
                break;
            }
        }
    }

    /**
     * Closes the player's inventory next tick
     * @param player The player whose inventory should be closed
     */
    public static void closeInventoryLater(@NotNull Player player) {
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

    /**
     * Open the inventory for player later
     * @param player The player to open the inventory for
     * @param inventory The inventory to be opened
     * @param delay The ticks delayed before opening the inventory
     */
    public static void openInventoryLater(@NotNull Player player, @NotNull Inventory inventory, @NotNull Long delay) {
        ProjectEXFILPlugin plugin = ProjectEXFILPlugin.getPlugin();
        if (plugin == null) { return; }
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                player.openInventory(inventory);
            }
        };
        task.runTaskLater(plugin, delay);
    }
}
