package org.nmo.project_exfil.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.command.SubCommand;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImportMapCommand implements SubCommand {

    private final ProjectEXFILPlugin plugin;

    public ImportMapCommand(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLanguageManager().send(sender, "exfil.command.players_only");
            return;
        }
        Player player = (Player) sender;

        if (args.length < 3) {
            plugin.getLanguageManager().send(player, "exfil.command.usage_importmap");
            return;
        }
        String fileName = args[1];
        StringBuilder displayName = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            displayName.append(args[i]).append(" ");
        }
        
        plugin.getMapManager().importMap(player, fileName, displayName.toString().trim());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2) {
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
        } else if (args.length == 3) {
            completions.add("<display_name>");
        }
        return completions;
    }

    @Override
    public String getPermission() {
        return "exfil.admin";
    }
}
