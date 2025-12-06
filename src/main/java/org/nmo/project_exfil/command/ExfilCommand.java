package org.nmo.project_exfil.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.manager.RegionManager;
import org.nmo.project_exfil.ui.MapSelectionView;

public class ExfilCommand implements CommandExecutor {

    private final ProjectEXFILPlugin plugin;
    private final RegionManager regionManager;
    private final MapSelectionView mapSelectionView;

    public ExfilCommand(ProjectEXFILPlugin plugin, RegionManager regionManager, MapSelectionView mapSelectionView) {
        this.plugin = plugin;
        this.regionManager = regionManager;
        this.mapSelectionView = mapSelectionView;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Open UI
            mapSelectionView.open(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("setextract")) {
            if (!player.hasPermission("exfil.admin")) {
                player.sendMessage("§cNo permission.");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage("§cUsage: /exfil setextract <name>");
                return true;
            }
            regionManager.saveExtractionPoint(player, args[1]);
            return true;
        }

        return false;
    }
}
