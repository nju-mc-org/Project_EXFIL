package org.nmo.project_exfil.ui;

import com.alessiodp.parties.api.interfaces.PartyPlayer;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

import org.nmo.project_exfil.manager.PartyManager;
import org.nmo.project_exfil.ui.framework.UIHelper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.nmo.project_exfil.ProjectEXFILPlugin;

public class MainMenuView {

    private final MapSelectionView mapSelectionView;
    private final PartyManager partyManager;
    private final StashView stashView;
    private org.nmo.project_exfil.ui.TraderView traderView;
    private org.nmo.project_exfil.ui.TaskView taskView;
    private org.nmo.project_exfil.ui.AchievementView achievementView;
    private org.nmo.project_exfil.ui.LeaderboardView leaderboardView;
    private TeamMenuView teamMenuView;
    private final ProjectEXFILPlugin plugin = ProjectEXFILPlugin.getPlugin();

    public MainMenuView(MapSelectionView mapSelectionView, PartyManager partyManager, StashView stashView) {
        this.mapSelectionView = mapSelectionView;
        this.partyManager = partyManager;
        this.stashView = stashView;
    }
    
    public void setTraderView(org.nmo.project_exfil.ui.TraderView traderView) {
        this.traderView = traderView;
    }
    
    public void setTaskView(org.nmo.project_exfil.ui.TaskView taskView) {
        this.taskView = taskView;
    }
    
    public void setAchievementView(org.nmo.project_exfil.ui.AchievementView achievementView) {
        this.achievementView = achievementView;
    }
    
    public void setLeaderboardView(org.nmo.project_exfil.ui.LeaderboardView leaderboardView) {
        this.leaderboardView = leaderboardView;
    }

    public void setTeamMenuView(TeamMenuView teamMenuView) {
        this.teamMenuView = teamMenuView;
    }

    public void open(Player player) {
        ChestGui gui = new ChestGui(5, "Main Menu - EXFIL");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        StaticPane pane = new StaticPane(0, 0, 9, 5);

        // 第一行 - 主要功能
        // Deploy Button
        ItemStack deployItem = new ItemStack(Material.IRON_SWORD);
        ItemMeta deployMeta = deployItem.getItemMeta();
        deployMeta.displayName(plugin.getLanguageManager().getMessage("exfil.menu.deploy"));
        deployMeta.lore(List.of(plugin.getLanguageManager().getMessage("exfil.menu.click_select_map")));
        deployItem.setItemMeta(deployMeta);

        pane.addItem(new GuiItem(deployItem, event -> {
            UIHelper.playClickSound(player);
            mapSelectionView.open(player);
        }), 1, 1);

        // Team Button
        ItemStack teamItem = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta teamMeta = teamItem.getItemMeta();
        teamMeta.displayName(plugin.getLanguageManager().getMessage("exfil.menu.team"));
        
        Component statusComp = plugin.getLanguageManager().getMessage("exfil.menu.status.not_in_party");
        if (partyManager.isEnabled()) {
             try {
                 PartyPlayer partyPlayer = partyManager.getPartyPlayer(player.getUniqueId());
                 if (partyPlayer != null && partyPlayer.isInParty()) {
                     statusComp = plugin.getLanguageManager().getMessage("exfil.menu.status.in_party", Placeholder.unparsed("party", partyPlayer.getPartyName()));
                 }
             } catch (Exception e) {
                 statusComp = plugin.getLanguageManager().getMessage("exfil.menu.status.error");
                 e.printStackTrace();
             }
        }
        
        teamMeta.lore(List.of(
            statusComp,
            plugin.getLanguageManager().getMessage("exfil.menu.click_manage_team")
        ));
        teamItem.setItemMeta(teamMeta);

        pane.addItem(new GuiItem(teamItem, event -> {
            UIHelper.playClickSound(player);
            gui.getInventory().close();
            if (teamMenuView != null) {
                teamMenuView.open(player);
            } else {
                openPartiesInterface(player);
            }
        }), 4, 1);

        // Stash Button
        ItemStack stashItem = new ItemStack(Material.CHEST);
        ItemMeta stashMeta = stashItem.getItemMeta();
        stashMeta.displayName(plugin.getLanguageManager().getMessage("exfil.menu.stash"));
        stashMeta.lore(List.of(plugin.getLanguageManager().getMessage("exfil.menu.open_stash")));
        stashItem.setItemMeta(stashMeta);

        pane.addItem(new GuiItem(stashItem, event -> {
            UIHelper.playClickSound(player);
            gui.getInventory().close();
            stashView.open(player);
        }), 7, 1);

        // 第二行 - 特勤处、任务、成就按钮
        if (traderView != null) {
            ItemStack traderItem = new ItemStack(Material.EMERALD);
            ItemMeta traderMeta = traderItem.getItemMeta();
            traderMeta.displayName(Component.text("特勤处", NamedTextColor.GREEN));
            traderMeta.lore(List.of(
                Component.text("交易与回收物品", NamedTextColor.GRAY),
                Component.text("点击打开特勤处", NamedTextColor.YELLOW)
            ));
            traderItem.setItemMeta(traderMeta);

            pane.addItem(new GuiItem(traderItem, event -> {
                UIHelper.playClickSound(player);
                gui.getInventory().close();
                traderView.open(player);
            }), 1, 2);
        }
        
        if (taskView != null) {
            ItemStack taskItem = new ItemStack(Material.BOOK);
            ItemMeta taskMeta = taskItem.getItemMeta();
            taskMeta.displayName(Component.text("任务", NamedTextColor.BLUE));
            taskMeta.lore(List.of(
                Component.text("查看和完成任务", NamedTextColor.GRAY),
                Component.text("点击打开任务界面", NamedTextColor.YELLOW)
            ));
            taskItem.setItemMeta(taskMeta);

            pane.addItem(new GuiItem(taskItem, event -> {
                UIHelper.playClickSound(player);
                gui.getInventory().close();
                taskView.open(player);
            }), 4, 2);
        }
        
        if (achievementView != null) {
            ItemStack achievementItem = new ItemStack(Material.GOLD_BLOCK);
            ItemMeta achievementMeta = achievementItem.getItemMeta();
            achievementMeta.displayName(Component.text("成就", NamedTextColor.GOLD));
            achievementMeta.lore(List.of(
                Component.text("查看已解锁的成就", NamedTextColor.GRAY),
                Component.text("点击打开成就界面", NamedTextColor.YELLOW)
            ));
            achievementItem.setItemMeta(achievementMeta);

            pane.addItem(new GuiItem(achievementItem, event -> {
                UIHelper.playClickSound(player);
                gui.getInventory().close();
                achievementView.open(player);
            }), 7, 2);
        }
        
        // 第三行 - 排行榜和改枪按钮
        if (leaderboardView != null) {
            ItemStack leaderboardItem = new ItemStack(Material.BEACON);
            ItemMeta leaderboardMeta = leaderboardItem.getItemMeta();
            leaderboardMeta.displayName(Component.text("排行榜", NamedTextColor.AQUA));
            leaderboardMeta.lore(List.of(
                Component.text("查看服务器排行榜", NamedTextColor.GRAY),
                Component.text("点击打开排行榜", NamedTextColor.YELLOW)
            ));
            leaderboardItem.setItemMeta(leaderboardMeta);

            pane.addItem(new GuiItem(leaderboardItem, event -> {
                UIHelper.playClickSound(player);
                gui.getInventory().close();
                leaderboardView.open(player);
            }), 4, 3);
        }
        
        // 改枪按钮（需要持有武器）
        if (plugin.getWeaponModificationManager() != null && 
            plugin.getWeaponModificationManager().isHoldingGun(player)) {
            ItemStack modItem = new ItemStack(Material.ANVIL);
            ItemMeta modMeta = modItem.getItemMeta();
            modMeta.displayName(Component.text("武器改装", NamedTextColor.LIGHT_PURPLE));
            modMeta.lore(List.of(
                Component.text("改装你的武器", NamedTextColor.GRAY),
                Component.text("安装配件提升性能", NamedTextColor.GRAY),
                Component.text("点击打开改装界面", NamedTextColor.YELLOW)
            ));
            modItem.setItemMeta(modMeta);
            
            pane.addItem(new GuiItem(modItem, event -> {
                UIHelper.playClickSound(player);
                gui.getInventory().close();
                org.nmo.project_exfil.ui.WeaponModificationView modView = 
                    new org.nmo.project_exfil.ui.WeaponModificationView(plugin, plugin.getWeaponModificationManager());
                modView.open(player);
            }), 7, 3);
        }

        gui.addPane(pane);
        gui.show(player);
    }

    private void openPartiesInterface(Player player) {
        if (!partyManager.isEnabled()) {
            plugin.getLanguageManager().send(player, "exfil.error.parties_disabled");
            return;
        }
        player.performCommand("party");
    }
}
