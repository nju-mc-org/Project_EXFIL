package org.nmo.project_exfil.footsteps;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.nmo.project_exfil.ProjectEXFILPlugin;

/**
 * Main class for handling player footsteps
 */
public class Footstep implements Listener {
    private long footstepInterval = 4L;
    private long footstepIntervalSprint = 4L;
    private long footstepIntervalWalk = 8L;

    public void init() {
        ProjectEXFILPlugin plugin = ProjectEXFILPlugin.getPlugin();
        if (plugin == null) { return; }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        YamlConfiguration config = ProjectEXFILPlugin.getDefaultConfig();
        if (config == null) { return; }
        if (config.contains("footstep-interval")) {
            this.footstepInterval = config.getLong("footstep-interval");
        }
        if (config.contains("footstep-interval-sprint")) {
            this.footstepIntervalSprint = config.getLong("footstep-interval-sprint");
        }
        if (config.contains("footstep-interval-walk")) {
            this.footstepIntervalWalk = config.getLong("footstep-interval-walk");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        ProjectEXFILPlugin plugin = ProjectEXFILPlugin.getPlugin();
        if (plugin == null) { return; }
        (new PlayerFootstepRunnable(
                event.getPlayer(),
                footstepInterval,
                footstepIntervalSprint,
                footstepIntervalWalk
        )).runTaskTimer(plugin, 0, this.footstepInterval);
    }
}
