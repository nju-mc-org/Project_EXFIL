package org.nmo.project_exfil.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.command.SubCommand;

import java.util.List;

public class LootCommand implements SubCommand {

    private final ProjectEXFILPlugin plugin;

    public LootCommand(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLanguageManager().send(sender, "exfil.command.players_only");
            return;
        }
        Player player = (Player) sender;
        
        // 如果有参数，处理预设管理
        if (args.length > 1) {
            if (args[1].equalsIgnoreCase("preset") || args[1].equalsIgnoreCase("presets")) {
                // 打开预设管理界面
                org.nmo.project_exfil.ui.LootPresetView presetView = 
                    new org.nmo.project_exfil.ui.LootPresetView(plugin.getLootPresetManager());
                presetView.open(player);
                return;
            }
        }
        
        // 默认打开战利品编辑器
        plugin.getLootManager().openEditor(player);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new java.util.ArrayList<>();
        if (args.length == 2) {
            if ("preset".startsWith(args[1].toLowerCase()) || "presets".startsWith(args[1].toLowerCase())) {
                completions.add("preset");
                completions.add("presets");
            }
        }
        return completions;
    }

    @Override
    public String getPermission() {
        return "exfil.admin";
    }
}
