package org.nmo.project_exfil;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.nmo.project_exfil.ChestUIHelper.ChestUIHelper;
import org.nmo.project_exfil.command.ExfilCommand;
import org.nmo.project_exfil.manager.GameManager;
import org.nmo.project_exfil.manager.RegionManager;
import org.nmo.project_exfil.task.ExtractionTask;
import org.nmo.project_exfil.ui.MapSelectionView;

public final class ProjectEXFILPlugin extends JavaPlugin {
    private static ProjectEXFILPlugin plugin = null;
    private static ChestUIHelper chestUIHelper = null;
    private static YamlConfiguration config = null;
    
    private GameManager gameManager;
    private RegionManager regionManager;
    private MapSelectionView mapSelectionView;

    /**
     * Get the plugin instance
     * @return The plugin instance
     */
    public static @Nullable ProjectEXFILPlugin getPlugin() {
        return plugin;
    }

    /**
     * Get the default plugin config
     * @return The plugin config
     */
    public static @Nullable YamlConfiguration getDefaultConfig() {
        return config;
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

        this.saveDefaultConfig();
        config = (YamlConfiguration) this.getConfig();

        // Initialize ChestUIHelper
        chestUIHelper = new ChestUIHelper();
        
        // Initialize Managers
        this.gameManager = new GameManager(this);
        this.regionManager = new RegionManager(this);
        
        // Initialize UI
        this.mapSelectionView = new MapSelectionView(gameManager);
        
        // Register Commands
        getCommand("exfil").setExecutor(new ExfilCommand(this, regionManager, mapSelectionView));
        
        // Start Tasks
        new ExtractionTask(gameManager).runTaskTimer(this, 20L, 20L);
    }

    /**
     * Plugin shutdown logic
     */
    @Override
    public void onDisable() {
        plugin = null;
    }
}
