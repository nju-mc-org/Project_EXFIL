package org.nmo.project_exfil.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.command.SubCommand;
import org.nmo.project_exfil.manager.RegionManager;

import java.util.ArrayList;
import java.util.List;

public class SetCommand implements SubCommand {

    private final ProjectEXFILPlugin plugin;

    public SetCommand(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLanguageManager().send(sender, "exfil.command.players_only");
            return;
        }
        Player player = (Player) sender;
        RegionManager regionManager = plugin.getRegionManager();

        if (args.length < 2) {
            plugin.getLanguageManager().send(player, "exfil.command.usage_setextract"); // Generic usage or specific?
            // Maybe add a generic usage message for set
            return;
        }

        if (args[1].equalsIgnoreCase("extract")) {
            if (args.length < 3) {
                plugin.getLanguageManager().send(player, "exfil.command.usage_setextract");
                return;
            }
            regionManager.saveExtractionPoint(player, args[2]);
            return;
        }

        if (args[1].equalsIgnoreCase("spawn")) {
            if (args.length < 3) {
                plugin.getLanguageManager().send(player, "exfil.command.usage_setspawn");
                return;
            }
            try {
                double radius = Double.parseDouble(args[2]);
                regionManager.saveSpawnRegion(player, radius);
            } catch (NumberFormatException e) {
                plugin.getLanguageManager().send(player, "exfil.command.invalid_number");
            }
            return;
        }

        if (args[1].equalsIgnoreCase("combat")) {
            regionManager.saveCombatRegion(player);
            return;
        }

        if (args[1].equalsIgnoreCase("npc")) {
            if (args.length < 4) {
                plugin.getLanguageManager().send(player, "exfil.command.usage_setnpc");
                return;
            }
            String name = args[2];
            try {
                int count = Integer.parseInt(args[3]);
                regionManager.saveNPCRegion(player, name, count);
            } catch (NumberFormatException e) {
                plugin.getLanguageManager().send(player, "exfil.command.invalid_number");
            }
            return;
        }

        plugin.getLanguageManager().send(player, "exfil.command.usage_setextract");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2) {
            List<String> subCmds = new ArrayList<>();
            subCmds.add("extract");
            subCmds.add("spawn");
            subCmds.add("combat");
            subCmds.add("npc");
            
            String input = args[1].toLowerCase();
            for (String cmd : subCmds) {
                if (cmd.startsWith(input)) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 3) {
            if (args[1].equalsIgnoreCase("extract")) {
                completions.add("<name>");
            } else if (args[1].equalsIgnoreCase("spawn")) {
                completions.add("<radius>");
            } else if (args[1].equalsIgnoreCase("npc")) {
                completions.add("<name>");
            }
        } else if (args.length == 4) {
            if (args[1].equalsIgnoreCase("npc")) {
                completions.add("<count>");
            }
        }
        return completions;
    }

    @Override
    public String getPermission() {
        return "exfil.admin";
    }
}
