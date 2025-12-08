package org.nmo.project_exfil.ChestUIHelper.Components;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for components of ChestUI
 */
public abstract class ChestUIComponent {
    /**
     * Return the actual position in inventory
     * @param row The required row
     * @param col The required col
     * @return The actual index in inventory
     */
    private static Integer toPosition(int row, int col) {
        return row * 9 + col;
    }

    /**
     * Returns the number of rows the component takes up
     * @return The number of rows the component takes up
     */
    Integer nRows() {
        return 1;
    }

    /**   * Returns the number of columns the component takes up
     * @return The number of columns the component takes up
     */
    Integer nCols() {
        return 1;
    }

    /**
     * Renders the component onto the given inventory
     * @param inventory The inventory to render onto
     * @param row The row to render onto
     * @param col The column to render onto
     */
    void render(@NotNull Inventory inventory, @NotNull Integer row, @NotNull Integer col) {
        // Do nothing by default
    }

    /**
     * Updates the component
     * @param inventory The inventory to render onto
     * @param row The row to render onto
     * @param col The column to render onto
     */
    void update(@NotNull Inventory inventory, @NotNull Integer row, @NotNull Integer col) {
        // Do nothing by default
    }

    /**
     * The callback function when a component is clicked on
     * @param inventory The inventory that was clicked on
     * @param player The player that clicked on the component
     * @param relRow The relative row of the component* @param relCol The relative column of the component
     */
    void onClick(@NotNull Inventory inventory, @NotNull Player player, @NotNull Integer relRow, @NotNull Integer relCol) {
        // Do nothing by default
    }
}
