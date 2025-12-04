package org.nmo.project_exfil;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.nmo.project_exfil.ChestUIHelper.ChestUIHelper;

public final class ProjectEXFILPlugin extends JavaPlugin {
    private static ProjectEXFILPlugin plugin = null;
    private static ChestUIHelper chestUIHelper = null;

    /**
     * Get the plugin instance
     * @return The plugin instance
     */
    public static @Nullable ProjectEXFILPlugin getPlugin() {
        return plugin;
    }

    /**
     * Get the ChestUIHelper instance
     * @return The ChestUIHelper instance
     */
    public static @Nullable ChestUIHelper getChestUIHelper() {
        return chestUIHelper;
    }

    /**
     * Plugin startup logic
     */
    @Override
    public void onEnable() {
        plugin = this;

        // Initialize ChestUIHelper
        chestUIHelper = new ChestUIHelper();
    }

    /**
     * Plugin shutdown logic
     */
    @Override
    public void onDisable() {
        plugin = null;
    }
}
