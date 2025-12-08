package org.nmo.project_exfil.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.manager.RegionManager;
import org.nmo.project_exfil.ui.MainMenuView;
import org.nmo.project_exfil.ui.StashView;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ExfilCommand implements CommandExecutor {

    private final ProjectEXFILPlugin plugin;
    private final RegionManager regionManager;
    private final MainMenuView mainMenuView;
    private final StashView stashView;

    public ExfilCommand(ProjectEXFILPlugin plugin, RegionManager regionManager, MainMenuView mainMenuView, StashView stashView) {
        this.plugin = plugin;
        this.regionManager = regionManager;
        this.mainMenuView = mainMenuView;
        this.stashView = stashView;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLanguageManager().send(sender, "exfil.command.players_only");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Open UI
            mainMenuView.open(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("stash")) {
            stashView.open(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("setextract")) {
            if (!player.hasPermission("exfil.admin")) {
                plugin.getLanguageManager().send(player, "exfil.command.no_permission");
                return true;
            }
            if (args.length < 2) {
                plugin.getLanguageManager().send(player, "exfil.command.usage_setextract");
                return true;
            }
            regionManager.saveExtractionPoint(player, args[1]);
            return true;
        }

        return false;
    }
}
