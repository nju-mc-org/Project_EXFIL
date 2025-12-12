package org.nmo.project_exfil.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.command.sub.*;
import org.nmo.project_exfil.manager.MapManager;
import org.nmo.project_exfil.manager.RegionManager;
import org.nmo.project_exfil.ui.MainMenuView;
import org.nmo.project_exfil.ui.StashView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExfilCommand implements CommandExecutor, TabCompleter {

    private final ProjectEXFILPlugin plugin;
    private final MainMenuView mainMenuView;
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public ExfilCommand(ProjectEXFILPlugin plugin, RegionManager regionManager, MapManager mapManager, MainMenuView mainMenuView, StashView stashView) {
        this.plugin = plugin;
        this.mainMenuView = mainMenuView;
        
        registerSubCommands(stashView);
    }

    private void registerSubCommands(StashView stashView) {
        subCommands.put("stash", new StashCommand(plugin, stashView));
        subCommands.put("set", new SetCommand(plugin));
        subCommands.put("list", new ListCommand(plugin));
        subCommands.put("delete", new DeleteCommand(plugin));
        subCommands.put("importmap", new ImportMapCommand(plugin));
        subCommands.put("loot", new LootCommand(plugin));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLanguageManager().send(sender, "exfil.command.players_only");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            mainMenuView.open(player);
            return true;
        }

        String subCmdName = args[0].toLowerCase();
        SubCommand subCmd = subCommands.get(subCmdName);

        if (subCmd == null) {
            return false;
        }

        if (subCmd.getPermission() != null && !player.hasPermission(subCmd.getPermission())) {
            plugin.getLanguageManager().send(player, "exfil.command.no_permission");
            return true;
        }

        subCmd.execute(sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (Map.Entry<String, SubCommand> entry : subCommands.entrySet()) {
                String name = entry.getKey();
                SubCommand cmd = entry.getValue();
                if (name.startsWith(input)) {
                    if (cmd.getPermission() == null || sender.hasPermission(cmd.getPermission())) {
                        completions.add(name);
                    }
                }
            }
            return completions;
        }
        
        String subCmdName = args[0].toLowerCase();
        SubCommand subCmd = subCommands.get(subCmdName);
        
        if (subCmd != null) {
            if (subCmd.getPermission() == null || sender.hasPermission(subCmd.getPermission())) {
                return subCmd.tabComplete(sender, args);
            }
        }
        
        return completions;
    }
}

