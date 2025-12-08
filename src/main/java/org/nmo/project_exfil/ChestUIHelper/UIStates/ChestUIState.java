package org.nmo.project_exfil.ChestUIHelper.UIStates;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for UI Component
 */
public abstract class ChestUIState {
    /**
     * The inventory managed by this instance.
     */
    private Inventory inventory = null;

    /**
     * Initializes the ChestUIState with its managed inventory
     */
    public ChestUIState() {
        inventory = Bukkit.createInventory(null, 9);
    }

    /**
     * Get the inventory managed by this instance
     * @return The inventory managed
     */
    public @NotNull Inventory getInventory() {
        return inventory;
    }  /**
     * Updates the inventory
     * @param elapsedTicks Number of ticks passed after last update
     * @return If there is a change in the state, specifically pop the state if a BackState is returned
     */
    public @Nullable ChestUIState update(@NotNull Long elapsedTicks) {
        // Do nothing by default
        return null;
    }

    /**
     * Triggered when player clicks on the inventory
     * @param row The row clicked in the inventory
     * @param col The col clicked in the inventory
     * @return If there is a change in the state, specifically pop the state if a BackState is returned
     */
    public @Nullable ChestUIState onClick(@NotNull Integer row, @NotNull Integer col) {
        // Do nothing by default
        return null;
    }
}
