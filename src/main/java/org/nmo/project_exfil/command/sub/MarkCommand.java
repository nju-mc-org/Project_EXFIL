package org.nmo.project_exfil.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.command.SubCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * 标记位置命令
 */
public class MarkCommand implements SubCommand {
    
    private final ProjectEXFILPlugin plugin;
    
    public MarkCommand(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLanguageManager().send(sender, "exfil.command.players_only");
            return;
        }
        
        Player player = (Player) sender;
        
        if (plugin.getTeamCommunicationManager() != null) {
            plugin.getTeamCommunicationManager().markLocation(player);
        }
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

