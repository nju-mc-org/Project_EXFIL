package org.nmo.project_exfil.ChestUIHelper;

import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public interface ChestUI {
    /**
     * Composes the inventory.
     * @return The composed inventory.
     */
    @NotNull Inventory compose();
}
