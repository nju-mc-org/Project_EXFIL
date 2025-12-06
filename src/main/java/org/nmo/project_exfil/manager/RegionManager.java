package org.nmo.project_exfil.manager;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.entity.Player;
import org.nmo.project_exfil.ProjectEXFILPlugin;

public class RegionManager {

    private final ProjectEXFILPlugin plugin;

    public RegionManager(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
    }

    public Region getPlayerSelection(Player player) {
        SessionManager manager = WorldEdit.getInstance().getSessionManager();
        try {
            return manager.get(BukkitAdapter.adapt(player)).getSelection(BukkitAdapter.adapt(player.getWorld()));
        } catch (IncompleteRegionException e) {
            player.sendMessage("§cYou have not selected a complete region! Use the wand.");
            return null;
        } catch (Exception e) {
             return null;
        }
    }

    public void saveExtractionPoint(Player admin, String name) {
        Region selection = getPlayerSelection(admin);
        if (selection == null) return;

        // Create WorldGuard region
        ProtectedRegion region = new ProtectedCuboidRegion(name, selection.getMinimumPoint(), selection.getMaximumPoint());
        
        // Add to WorldGuard
        WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(admin.getWorld())).addRegion(region);
        
        admin.sendMessage("§aSaved extraction point (WG Region): " + name);
    }
}
