package org.nmo.project_exfil.ui.framework;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nmo.project_exfil.ui.framework.state.BackState;
import org.nmo.project_exfil.ui.framework.state.ChestUIState;

import java.util.Stack;

/**
 * The classic StateStack class for ChestUI
 */
public class ChestUIStateStack extends Stack<ChestUIState> {
    /**
     * The viewer of the stack
     */
    private final Player player;

    /**
     * Gets the viewer of the stack
     * @return The viewer of the stack
     */
    public @Nullable Player getPlayer() {
        return player;
    }

    /**
     * Whether the current inventory matches the given inventory
     * @param inventory The given inventory
     * @return Whether matches
     */
    public boolean matchInventory(@NotNull Inventory inventory) {
        Inventory currentInventory = this.peek().getInventory();
        return inventory.equals(currentInventory);
    }

    /**
     * Creates a ChestUIStateStack instance
     * @param player The player who sees the UI
     * @param baseState The very first UI State for player to see
     */
    public ChestUIStateStack(@NotNull Player player, @NotNull ChestUIState baseState) {
        super();
        this.push(baseState);
        this.player = player;
        player.openInventory(baseState.getInventory());
    }

    /**
     * Update the StateStack
     * @param elapsedTicks Number of ticks passed after last update
     */
    public void update(@NotNull Long elapsedTicks) {
        ChestUIState newState = this.peek().update(elapsedTicks);
        if (newState != null) {
            if (newState instanceof BackState) {
                this.pop();
            } else {
                this.push(newState);
                ChestUIHelper.closeInventoryLater(this.player);
                ChestUIHelper.openInventoryLater(this.player, newState.getInventory(), 2L);
            }
        }
    }

    /**
     * Handle the inventory click events
     * @param row The clicked row
     * @param col The clicked col
     */
    public void onClick(@NotNull Integer row, @NotNull Integer col) {
        ChestUIState newState = this.peek().onClick(row, col);
        if (newState != null) {
            if (newState instanceof BackState) {
                this.pop();
            } else {
                this.push(newState);
                ChestUIHelper.closeInventoryLater(this.player);
                ChestUIHelper.openInventoryLater(this.player, newState.getInventory(), 2L);
            }
        }
    }
}
