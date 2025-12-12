package org.nmo.project_exfil.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.command.SubCommand;

import java.util.Collections;
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
        plugin.getLootManager().openEditor(player);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public String getPermission() {
        return "exfil.admin";
    }
}
