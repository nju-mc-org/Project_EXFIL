package org.nmo.project_exfil;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.nmo.project_exfil.command.ExfilCommand;
import org.nmo.project_exfil.manager.GameManager;
import org.nmo.project_exfil.manager.LootManager;
import org.nmo.project_exfil.manager.PartyManager;
import org.nmo.project_exfil.manager.RegionManager;
import org.nmo.project_exfil.manager.StashManager;
import org.nmo.project_exfil.manager.LanguageManager;
import org.nmo.project_exfil.manager.MapManager;
import org.nmo.project_exfil.task.ExtractionTask;
import org.nmo.project_exfil.ui.MapSelectionView;
import org.nmo.project_exfil.ui.MainMenuView;
import org.nmo.project_exfil.ui.TeamMenuView;
import org.nmo.project_exfil.ui.StashView;
import org.nmo.project_exfil.manager.ScoreboardManager;
import org.nmo.project_exfil.manager.ReviveManager;
import org.nmo.project_exfil.manager.NametagManager;
import org.nmo.project_exfil.placeholder.ExfilExpansion;
import org.nmo.project_exfil.util.DependencyHelper;

import org.nmo.project_exfil.listener.ConnectionListener;
import org.nmo.project_exfil.listener.DeathListener;
import org.nmo.project_exfil.listener.LobbyListener;
import org.nmo.project_exfil.listener.LootListener;
import org.nmo.project_exfil.listener.ReviveListener;

public final class ProjectEXFILPlugin extends JavaPlugin {
    private static ProjectEXFILPlugin plugin = null;
    private static YamlConfiguration config = null;
    
    private GameManager gameManager;
    private RegionManager regionManager;
    private LootManager lootManager;
    private ScoreboardManager scoreboardManager;
    private PartyManager partyManager;
    private StashManager stashManager;
    private MapManager mapManager;
    private LanguageManager languageManager;
    private ReviveManager reviveManager;
    private NametagManager nametagManager;
    private MapSelectionView mapSelectionView;
    private MainMenuView mainMenuView;
    private TeamMenuView teamMenuView;
    private StashView stashView;

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
     * Plugin startup logic
     */
    @Override
    public void onEnable() {
        plugin = this;

        this.saveDefaultConfig();
        config = (YamlConfiguration) this.getConfig();

        // Initialize Managers
        this.gameManager = new GameManager(this);
        this.regionManager = new RegionManager(this);
        this.lootManager = new LootManager(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.partyManager = new PartyManager(this);
        this.stashManager = new StashManager(this);
        this.mapManager = new MapManager(this);
        this.languageManager = new LanguageManager(this);
        this.reviveManager = new ReviveManager(this);
        this.nametagManager = new NametagManager(this);
        
        // Initialize UI
        this.mapSelectionView = new MapSelectionView(gameManager);
        this.stashView = new StashView(stashManager);
        this.mainMenuView = new MainMenuView(mapSelectionView, partyManager, stashView);
        this.teamMenuView = new TeamMenuView(mainMenuView, partyManager);
        this.mainMenuView.setTeamMenuView(teamMenuView);
        
        // Register Commands
        ExfilCommand exfilCommand = new ExfilCommand(this, regionManager, mapManager, mainMenuView, stashView);
        getCommand("exfil").setExecutor(exfilCommand);
        getCommand("exfil").setTabCompleter(exfilCommand);
        
        // Register Listeners
        getServer().getPluginManager().registerEvents(new DeathListener(this, gameManager), this);
        getServer().getPluginManager().registerEvents(new ConnectionListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new LobbyListener(), this);
        getServer().getPluginManager().registerEvents(new ReviveListener(this, reviveManager, gameManager), this);
        getServer().getPluginManager().registerEvents(new LootListener(this), this);
        

        
        // Start Tasks
        new ExtractionTask(gameManager, regionManager).runTaskTimer(this, 20L, 20L);
        
        // Register Placeholders
        if (DependencyHelper.isPlaceholderAPIEnabled()) {
            new ExfilExpansion(this).register();
        }
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public MapManager getMapManager() {
        return mapManager;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public LootManager getLootManager() {
        return lootManager;
    }

    public ReviveManager getReviveManager() {
        return reviveManager;
    }

    public NametagManager getNametagManager() {
        return nametagManager;
    }

    public MainMenuView getMainMenuView() {
        return mainMenuView;
    }

    /**
     * Plugin shutdown logic
     */
    @Override
    public void onDisable() {
        plugin = null;
    }
}
