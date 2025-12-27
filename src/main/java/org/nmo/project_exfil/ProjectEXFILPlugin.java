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
import org.nmo.project_exfil.manager.SecureContainerManager;
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
import org.nmo.project_exfil.listener.SecureContainerListener;
import org.nmo.project_exfil.listener.StimListener;

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
    private SecureContainerManager secureContainerManager;
    private org.nmo.project_exfil.manager.TraderManager traderManager;
    private org.nmo.project_exfil.manager.TaskManager taskManager;
    private org.nmo.project_exfil.manager.AchievementManager achievementManager;
    private org.nmo.project_exfil.data.PlayerDataManager playerDataManager;
    private org.nmo.project_exfil.manager.LeaderboardManager leaderboardManager;
    private org.nmo.project_exfil.manager.LootPresetManager lootPresetManager;
    private MapSelectionView mapSelectionView;
    private MainMenuView mainMenuView;
    private TeamMenuView teamMenuView;
    private StashView stashView;
    private org.nmo.project_exfil.ui.TraderView traderView;

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
        this.secureContainerManager = new SecureContainerManager(this);
        this.traderManager = new org.nmo.project_exfil.manager.TraderManager(this);
        this.taskManager = new org.nmo.project_exfil.manager.TaskManager(this);
        this.achievementManager = new org.nmo.project_exfil.manager.AchievementManager(this);
        this.playerDataManager = new org.nmo.project_exfil.data.PlayerDataManager(this);
        this.leaderboardManager = new org.nmo.project_exfil.manager.LeaderboardManager(this);
        this.lootPresetManager = new org.nmo.project_exfil.manager.LootPresetManager(this);
        
        // Initialize NPC Performance Optimizer
        new org.nmo.project_exfil.manager.NPCPerformanceOptimizer(this);
        
        // Initialize UI
        this.mapSelectionView = new MapSelectionView(gameManager);
        this.stashView = new StashView(stashManager);
        this.traderView = new org.nmo.project_exfil.ui.TraderView(traderManager);
        org.nmo.project_exfil.ui.TaskView taskView = new org.nmo.project_exfil.ui.TaskView(taskManager);
        org.nmo.project_exfil.ui.AchievementView achievementView = new org.nmo.project_exfil.ui.AchievementView(achievementManager);
        org.nmo.project_exfil.ui.LeaderboardView leaderboardView = new org.nmo.project_exfil.ui.LeaderboardView(leaderboardManager);
        this.mainMenuView = new MainMenuView(mapSelectionView, partyManager, stashView);
        this.mainMenuView.setTraderView(traderView);
        this.mainMenuView.setTaskView(taskView);
        this.mainMenuView.setAchievementView(achievementView);
        this.mainMenuView.setLeaderboardView(leaderboardView);
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
        getServer().getPluginManager().registerEvents(new SecureContainerListener(this, secureContainerManager), this);
        getServer().getPluginManager().registerEvents(new StimListener(this), this);
        getServer().getPluginManager().registerEvents(new org.nmo.project_exfil.listener.TaskAchievementListener(this), this);
        getServer().getPluginManager().registerEvents(new org.nmo.project_exfil.listener.PlayerDataListener(this), this);
        getServer().getPluginManager().registerEvents(new org.nmo.project_exfil.listener.ArmorProtectionListener(this), this);
        
        // Register Footstep system
        org.nmo.project_exfil.footsteps.Footstep footstep = new org.nmo.project_exfil.footsteps.Footstep();
        footstep.init(); // Initialize and register

        // Start Tasks
        new ExtractionTask(gameManager, regionManager).runTaskTimer(this, 20L, 20L);
        
        // ItemsAdder (async loading)
        if (DependencyHelper.isItemsAdderEnabled()) {
            org.nmo.project_exfil.integration.itemsadder.ItemsAdderIntegration.init(this);
        }

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

    public StashManager getStashManager() {
        return stashManager;
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

    public SecureContainerManager getSecureContainerManager() {
        return secureContainerManager;
    }

    public org.nmo.project_exfil.manager.TraderManager getTraderManager() {
        return traderManager;
    }
    
    public org.nmo.project_exfil.ui.TraderView getTraderView() {
        return traderView;
    }

    public org.nmo.project_exfil.manager.TaskManager getTaskManager() {
        return taskManager;
    }
    
    public org.nmo.project_exfil.manager.AchievementManager getAchievementManager() {
        return achievementManager;
    }

    public org.nmo.project_exfil.data.PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
    
    public org.nmo.project_exfil.manager.LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }
    
    public org.nmo.project_exfil.manager.LootPresetManager getLootPresetManager() {
        return lootPresetManager;
    }

    /**
     * Plugin shutdown logic
     */
    @Override
    public void onDisable() {
        // 保存所有玩家数据
        if (playerDataManager != null) {
            playerDataManager.saveAll(false); // 同步保存，确保数据不丢失
        }
        
        plugin = null;
    }
}
