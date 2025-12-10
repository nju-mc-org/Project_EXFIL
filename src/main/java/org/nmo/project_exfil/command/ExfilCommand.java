package org.nmo.project_exfil.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.manager.MapManager;
import org.nmo.project_exfil.manager.RegionManager;
import org.nmo.project_exfil.ui.MainMenuView;
import org.nmo.project_exfil.ui.StashView;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.format.NamedTextColor;

import org.nmo.project_exfil.manager.RegionManager.ExtractionRegion;
import org.nmo.project_exfil.manager.MapManager.GameMap;
import java.util.Map;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ExfilCommand implements CommandExecutor, TabCompleter {

    private final ProjectEXFILPlugin plugin;
    private final RegionManager regionManager;
    private final MapManager mapManager;
    private final MainMenuView mainMenuView;
    private final StashView stashView;

    public ExfilCommand(ProjectEXFILPlugin plugin, RegionManager regionManager, MapManager mapManager, MainMenuView mainMenuView, StashView stashView) {
        this.plugin = plugin;
        this.regionManager = regionManager;
        this.mapManager = mapManager;
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

        if (args[0].equalsIgnoreCase("set")) {
            if (!player.hasPermission("exfil.admin")) {
                plugin.getLanguageManager().send(player, "exfil.command.no_permission");
                return true;
            }
            
            if (args.length >= 2 && args[1].equalsIgnoreCase("extract")) {
                if (args.length < 3) {
                    plugin.getLanguageManager().send(player, "exfil.command.usage_setextract");
                    return true;
                }
                regionManager.saveExtractionPoint(player, args[2]);
                return true;
            }
            
            if (args.length >= 2 && args[1].equalsIgnoreCase("spawn")) {
                if (args.length < 3) {
                    plugin.getLanguageManager().send(player, "exfil.command.usage_setspawn");
                    return true;
                }
                try {
                    double radius = Double.parseDouble(args[2]);
                    regionManager.saveSpawnRegion(player, radius);
                } catch (NumberFormatException e) {
                    plugin.getLanguageManager().send(player, "exfil.command.invalid_number");
                }
                return true;
            }
            
            plugin.getLanguageManager().send(player, "exfil.command.usage_setextract");
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (!player.hasPermission("exfil.admin")) {
                plugin.getLanguageManager().send(player, "exfil.command.no_permission");
                return true;
            }
            if (args.length < 3) {
                plugin.getLanguageManager().send(player, "exfil.command.usage_importmap");
                return true;
            }
            String fileName = args[1];
            StringBuilder displayName = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                displayName.append(args[i]).append(" ");
            }
            
            mapManager.importMap(player, fileName, displayName.toString().trim());
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            if (!player.hasPermission("exfil.admin")) {
                plugin.getLanguageManager().send(player, "exfil.command.no_permission");
                return true;
            }
            if (args.length < 2) {
                plugin.getLanguageManager().send(player, "exfil.command.usage_list");
                return true;
            }
            
            if (args[1].equalsIgnoreCase("maps")) {
                plugin.getLanguageManager().send(player, "exfil.list.maps.header");
                for (GameMap map : mapManager.getMaps()) {
                    plugin.getLanguageManager().send(player, "exfil.list.maps.format", 
                        Placeholder.unparsed("name", map.getDisplayName()),
                        Placeholder.unparsed("id", map.getId()));
                }
                return true;
            }
            
            if (args[1].equalsIgnoreCase("extracts")) {
                plugin.getLanguageManager().send(player, "exfil.list.extracts.header");
                Map<String, ExtractionRegion> regions = regionManager.getAllExtractionRegions();
                
                // Group by world
                // Simple iteration for now, could be optimized
                List<String> worlds = new ArrayList<>();
                for (ExtractionRegion r : regions.values()) {
                    if (!worlds.contains(r.worldName)) worlds.add(r.worldName);
                }
                
                for (String world : worlds) {
                    plugin.getLanguageManager().send(player, "exfil.list.extracts.world_header", Placeholder.unparsed("world", world));
                    for (Map.Entry<String, ExtractionRegion> entry : regions.entrySet()) {
                        if (entry.getValue().worldName.equals(world)) {
                            plugin.getLanguageManager().send(player, "exfil.list.extracts.format", Placeholder.unparsed("name", entry.getKey()));
                        }
                    }
                }
                return true;
            }
            
            plugin.getLanguageManager().send(player, "exfil.command.usage_list");
            return true;
        }

        if (args[0].equalsIgnoreCase("delete")) {
            if (!player.hasPermission("exfil.admin")) {
                plugin.getLanguageManager().send(player, "exfil.command.no_permission");
                return true;
            }
            if (args.length < 3) {
                plugin.getLanguageManager().send(player, "exfil.command.usage_delete");
                return true;
            }
            
            if (args[1].equalsIgnoreCase("map")) {
                if (mapManager.deleteMap(args[2])) {
                    plugin.getLanguageManager().send(player, "exfil.delete.map_success");
                } else {
                    plugin.getLanguageManager().send(player, "exfil.delete.map_not_found");
                }
                return true;
            }
            
            if (args[1].equalsIgnoreCase("extract")) {
                if (regionManager.deleteExtractionRegion(args[2])) {
                    plugin.getLanguageManager().send(player, "exfil.delete.extract_success");
                } else {
                    plugin.getLanguageManager().send(player, "exfil.delete.extract_not_found");
                }
                return true;
            }
            
            plugin.getLanguageManager().send(player, "exfil.command.usage_delete");
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("stash");
            if (sender.hasPermission("exfil.admin")) {
                options.add("set");
                options.add("importmap");
                options.add("list");
                options.add("delete");
            }
            
            String input = args[0].toLowerCase();
            for (String option : options) {
                if (option.startsWith(input)) {
                    completions.add(option);
                }
            }
            return completions;
        }
        
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("set") && sender.hasPermission("exfil.admin")) {
                List<String> subCmds = new ArrayList<>();
                subCmds.add("extract");
                subCmds.add("spawn");
                
                String input = args[1].toLowerCase();
                for (String cmd : subCmds) {
                    if (cmd.startsWith(input)) {
                        completions.add(cmd);
                    }
                }
                return completions;
            }
            
            if (args[0].equalsIgnoreCase("list") && sender.hasPermission("exfil.admin")) {
                List<String> subCmds = new ArrayList<>();
                subCmds.add("maps");
                subCmds.add("extracts");
                
                String input = args[1].toLowerCase();
                for (String cmd : subCmds) {
                    if (cmd.startsWith(input)) {
                        completions.add(cmd);
                    }
                }
                return completions;
            }
            
            if (args[0].equalsIgnoreCase("delete") && sender.hasPermission("exfil.admin")) {
                List<String> subCmds = new ArrayList<>();
                subCmds.add("map");
                subCmds.add("extract");
                
                String input = args[1].toLowerCase();
                for (String cmd : subCmds) {
                    if (cmd.startsWith(input)) {
                        completions.add(cmd);
                    }
                }
                return completions;
            }
            
            if (args[0].equalsIgnoreCase("importmap") && sender.hasPermission("exfil.admin")) {
                // List files in import_maps folder
                File importFolder = new File(plugin.getDataFolder(), "import_maps");
                if (importFolder.exists() && importFolder.isDirectory()) {
                    File[] files = importFolder.listFiles();
                    if (files != null) {
                        String input = args[1].toLowerCase();
                        for (File file : files) {
                            if (file.getName().toLowerCase().startsWith(input)) {
                                completions.add(file.getName());
                            }
                        }
                    }
                }
                return completions;
            }
        }
        
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("set") && args[1].equalsIgnoreCase("extract") && sender.hasPermission("exfil.admin")) {
                completions.add("<name>");
                return completions;
            }
            if (args[0].equalsIgnoreCase("set") && args[1].equalsIgnoreCase("spawn") && sender.hasPermission("exfil.admin")) {
                completions.add("<radius>");
                return completions;
            }
            if (args[0].equalsIgnoreCase("importmap") && sender.hasPermission("exfil.admin")) {
                completions.add("<display_name>");
                return completions;
            }
            
            if (args[0].equalsIgnoreCase("delete") && sender.hasPermission("exfil.admin")) {
                if (args[1].equalsIgnoreCase("map")) {
                    for (GameMap map : mapManager.getMaps()) {
                        completions.add(map.getId());
                    }
                } else if (args[1].equalsIgnoreCase("extract")) {
                    for (String name : regionManager.getAllExtractionRegions().keySet()) {
                        completions.add(name);
                    }
                }
                return completions;
            }
        }
        
        return completions;
    }
}
