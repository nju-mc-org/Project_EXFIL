package org.nmo.project_exfil.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.command.SubCommand;
import org.nmo.project_exfil.ui.StashView;

import java.util.ArrayList;
import java.util.List;

public class StashCommand implements SubCommand {

    private final ProjectEXFILPlugin plugin;
    private final StashView stashView;

    public StashCommand(ProjectEXFILPlugin plugin, StashView stashView) {
        this.plugin = plugin;
        this.stashView = stashView;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLanguageManager().send(sender, "exfil.command.players_only");
            return;
        }
        stashView.open((Player) sender);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return null; // No permission required
    }
}
