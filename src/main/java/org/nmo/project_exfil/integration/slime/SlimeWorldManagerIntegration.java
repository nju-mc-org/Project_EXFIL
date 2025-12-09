package org.nmo.project_exfil.integration.slime;

import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import com.infernalsuite.asp.api.world.properties.SlimeProperties;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.infernalsuite.asp.loaders.file.FileLoader;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.nmo.project_exfil.ProjectEXFILPlugin;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public class SlimeWorldManagerIntegration {

    private final ProjectEXFILPlugin plugin;
    private final AdvancedSlimePaperAPI asp;
    private SlimeLoader loader;

    public SlimeWorldManagerIntegration(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
        this.asp = AdvancedSlimePaperAPI.instance();
        
        // Initialize FileLoader
        File worldsDir = new File(plugin.getDataFolder(), "slime_worlds");
        if (!worldsDir.exists()) {
            worldsDir.mkdirs();
        }
        this.loader = new FileLoader(worldsDir);
    }

    public CompletableFuture<SlimeWorldInstance> loadWorld(String worldName, boolean readOnly) {
        CompletableFuture<SlimeWorldInstance> future = new CompletableFuture<>();

        // Read world asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                SlimePropertyMap properties = new SlimePropertyMap();
                properties.setValue(SlimeProperties.DIFFICULTY, "normal");
                properties.setValue(SlimeProperties.PVP, true);
                
                SlimeWorld world = asp.readWorld(loader, worldName, readOnly, properties);
                
                // Load world synchronously
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        SlimeWorldInstance instance = asp.loadWorld(world, true);
                        future.complete(instance);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                });
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }
    
    public CompletableFuture<SlimeWorldInstance> createInstance(String templateName) {
        CompletableFuture<SlimeWorldInstance> future = new CompletableFuture<>();
        // Sanitize template name for instance name to ensure valid ResourceLocation
        String sanitizedTemplate = templateName.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
        String instanceName = sanitizedTemplate + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                SlimePropertyMap properties = new SlimePropertyMap();
                properties.setValue(SlimeProperties.DIFFICULTY, "normal");
                properties.setValue(SlimeProperties.PVP, true);
                
                // Read the template world
                SlimeWorld template = asp.readWorld(loader, templateName, true, properties);
                
                // Clone it with a new name
                SlimeWorld instanceWorld = template.clone(instanceName);
                
                // Load the cloned world synchronously
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        SlimeWorldInstance instance = asp.loadWorld(instanceWorld, true);
                        future.complete(instance);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                });
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }

    public CompletableFuture<Void> importVanillaWorld(File worldDir, String worldName) {
        return CompletableFuture.runAsync(() -> {
            try {
                SlimeWorld world = asp.readVanillaWorld(worldDir, worldName, loader);
                asp.saveWorld(world);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void unloadWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            Bukkit.unloadWorld(world, false);
        }
    }
    
    public SlimeLoader getLoader() {
        return loader;
    }
}
