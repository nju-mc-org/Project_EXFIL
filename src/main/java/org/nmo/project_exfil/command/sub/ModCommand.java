package org.nmo.project_exfil.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.command.SubCommand;
import org.nmo.project_exfil.ui.WeaponModificationView;

import java.util.ArrayList;
import java.util.List;

/**
 * 武器改装命令
 */
public class ModCommand implements SubCommand {
    
    private final ProjectEXFILPlugin plugin;
    
    public ModCommand(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLanguageManager().send(sender, "exfil.command.players_only");
            return;
        }
        
        Player player = (Player) sender;
        
        if (plugin.getWeaponModificationManager() == null) {
            plugin.getLanguageManager().send(player, "exfil.weapon.modification_not_available");
            return;
        }
        
        WeaponModificationView modView = new WeaponModificationView(plugin, plugin.getWeaponModificationManager());
        modView.open(player);
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

