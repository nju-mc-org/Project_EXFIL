package org.nmo.project_exfil.listener;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.manager.GameInstance;
import org.nmo.project_exfil.manager.gamemodule.LootModule;

public class LootListener implements Listener {

    private final ProjectEXFILPlugin plugin;
    private static final String UUID_STRING = "720faeb5-cd75-4293-837e-601c1b32a11d";

    public LootListener(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.PLAYER_HEAD) return;
        
        Player player = event.getPlayer();
        GameInstance instance = plugin.getGameManager().getGameInstance(player);
        
        if (instance == null) return; // Not in a game
        
        if (isLootBox(block)) {
            event.setCancelled(true); // Prevent normal interaction (like placing blocks on it)
            
            LootModule lootModule = instance.getModule(LootModule.class);
            if (lootModule != null) {
                player.openInventory(lootModule.getInventory(block.getLocation()));
            }
        }
    }

    private boolean isLootBox(Block block) {
        if (block.getState() instanceof Skull) {
            Skull skull = (Skull) block.getState();
            PlayerProfile profile = skull.getPlayerProfile();
            if (profile != null && profile.getId() != null) {
                return profile.getId().toString().equals(UUID_STRING);
            }
        }
        return false;
    }
}
