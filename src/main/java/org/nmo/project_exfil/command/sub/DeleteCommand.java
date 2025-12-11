package org.nmo.project_exfil.command.sub;

import org.bukkit.command.CommandSender;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.command.SubCommand;
import org.nmo.project_exfil.manager.MapManager.GameMap;

import java.util.ArrayList;
import java.util.List;

public class DeleteCommand implements SubCommand {

    private final ProjectEXFILPlugin plugin;

    public DeleteCommand(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.getLanguageManager().send(sender, "exfil.command.usage_delete");
            return;
        }

        if (args[1].equalsIgnoreCase("map")) {
            if (plugin.getMapManager().deleteMap(args[2])) {
                plugin.getLanguageManager().send(sender, "exfil.delete.map_success");
            } else {
                plugin.getLanguageManager().send(sender, "exfil.delete.map_not_found");
            }
            return;
        }

        if (args[1].equalsIgnoreCase("extract")) {
            if (plugin.getRegionManager().deleteExtractionRegion(args[2])) {
                plugin.getLanguageManager().send(sender, "exfil.delete.extract_success");
            } else {
                plugin.getLanguageManager().send(sender, "exfil.delete.extract_not_found");
            }
            return;
        }

        if (args[1].equalsIgnoreCase("combat")) {
            if (plugin.getRegionManager().deleteCombatRegion(args[2])) {
                plugin.getLanguageManager().send(sender, "exfil.delete.combat_success");
            } else {
                plugin.getLanguageManager().send(sender, "exfil.delete.combat_not_found");
            }
            return;
        }

        if (args[1].equalsIgnoreCase("npc")) {
            if (plugin.getRegionManager().deleteNPCRegion(args[2])) {
                plugin.getLanguageManager().send(sender, "exfil.delete.npc_success");
            } else {
                plugin.getLanguageManager().send(sender, "exfil.delete.npc_not_found");
            }
            return;
        }

        plugin.getLanguageManager().send(sender, "exfil.command.usage_delete");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2) {
            List<String> subCmds = new ArrayList<>();
            subCmds.add("map");
            subCmds.add("extract");
            subCmds.add("combat");
            subCmds.add("npc");
            
            String input = args[1].toLowerCase();
            for (String cmd : subCmds) {
                if (cmd.startsWith(input)) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 3) {
            if (args[1].equalsIgnoreCase("map")) {
                for (GameMap map : plugin.getMapManager().getMaps()) {
                    completions.add(map.getId());
                }
            } else if (args[1].equalsIgnoreCase("extract")) {
                for (String name : plugin.getRegionManager().getAllExtractionRegions().keySet()) {
                    completions.add(name);
                }
            } else if (args[1].equalsIgnoreCase("combat")) {
                for (String name : plugin.getRegionManager().getAllCombatRegions().keySet()) {
                    completions.add(name);
                }
            } else if (args[1].equalsIgnoreCase("npc")) {
                for (String name : plugin.getRegionManager().getAllNPCRegions().keySet()) {
                    completions.add(name);
                }
            }
        }
        return completions;
    }

    @Override
    public String getPermission() {
        return "exfil.admin";
    }
}
