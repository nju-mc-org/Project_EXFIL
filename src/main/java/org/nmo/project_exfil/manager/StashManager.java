package org.nmo.project_exfil.manager;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.nmo.project_exfil.ProjectEXFILPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StashManager {

    private final ProjectEXFILPlugin plugin;
    private final File userDataFolder;

    public StashManager(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
        this.userDataFolder = new File(plugin.getDataFolder(), "userdata");
        if (!userDataFolder.exists()) {
            userDataFolder.mkdirs();
        }
    }

    public void saveStash(Player player, ItemStack[] contents) {
        File userFile = new File(userDataFolder, player.getUniqueId() + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(userFile);
        
        config.set("stash", contents);
        
        try {
            config.save(userFile);
        } catch (IOException e) {
            e.printStackTrace();
            player.sendMessage("§cError saving stash data!");
        }
    }

    public ItemStack[] loadStash(Player player) {
        File userFile = new File(userDataFolder, player.getUniqueId() + ".yml");
        if (!userFile.exists()) {
            return new ItemStack[54]; // Double chest size
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(userFile);
        List<ItemStack> list = (List<ItemStack>) config.getList("stash");
        
        if (list == null) {
            return new ItemStack[54];
        }
        
        return list.toArray(new ItemStack[0]);
    }
}
