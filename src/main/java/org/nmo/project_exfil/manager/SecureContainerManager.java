package org.nmo.project_exfil.manager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class SecureContainerManager {

    private final ProjectEXFILPlugin plugin;
    private final File dataFolder;
    private final java.util.Map<UUID, Object> fileLocks = new java.util.concurrent.ConcurrentHashMap<>();

    public SecureContainerManager(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "securedata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    private Object lockFor(UUID uuid) {
        return fileLocks.computeIfAbsent(uuid, k -> new Object());
    }

    private File fileFor(UUID uuid) {
        return new File(dataFolder, uuid + ".yml");
    }

    public int getSize(Player player) {
        org.bukkit.configuration.file.YamlConfiguration cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(fileFor(player.getUniqueId()));
        return cfg.getInt("secure.size", 0);
    }

    public boolean activate(Player player, int size) {
        if (size <= 0) return false;

        File f = fileFor(player.getUniqueId());
        synchronized (lockFor(player.getUniqueId())) {
            org.bukkit.configuration.file.YamlConfiguration cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
            int current = cfg.getInt("secure.size", 0);
            if (current >= size) {
                plugin.getLanguageManager().send(player, "exfil.secure.already_better", Placeholder.unparsed("size", String.valueOf(current)));
                return false;
            }
            cfg.set("secure.size", size);
            if (!cfg.contains("secure.contents")) {
                cfg.set("secure.contents", new ItemStack[size]);
            }
            try {
                cfg.save(f);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        plugin.getLanguageManager().send(player, "exfil.secure.activated", Placeholder.unparsed("size", String.valueOf(size)));
        return true;
    }

    public void open(Player player) {
        int size = getSize(player);
        if (size <= 0) {
            plugin.getLanguageManager().send(player, "exfil.secure.no_container");
            return;
        }

        int invSize = ((size + 8) / 9) * 9;
        Inventory inv = Bukkit.createInventory(new SecureContainerHolder(player.getUniqueId(), size), invSize,
            plugin.getLanguageManager().getMessage("exfil.secure.title"));

        // Fill padding slots
        for (int i = size; i < invSize; i++) {
            inv.setItem(i, createLockedSlotItem());
        }

        // Load contents
        ItemStack[] contents = loadContents(player.getUniqueId(), size);
        for (int i = 0; i < Math.min(size, contents.length); i++) {
            ItemStack it = contents[i];
            if (it != null && !it.getType().isAir()) {
                inv.setItem(i, it);
            }
        }

        player.openInventory(inv);
    }

    private ItemStack[] loadContents(UUID uuid, int size) {
        File f = fileFor(uuid);
        synchronized (lockFor(uuid)) {
            org.bukkit.configuration.file.YamlConfiguration cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
            List<?> list = cfg.getList("secure.contents");
            ItemStack[] result = new ItemStack[size];
            if (list == null) return result;
            for (int i = 0; i < Math.min(size, list.size()); i++) {
                Object o = list.get(i);
                if (o instanceof ItemStack) {
                    result[i] = (ItemStack) o;
                }
            }
            return result;
        }
    }

    public void saveContents(UUID uuid, int size, Inventory inv) {
        File f = fileFor(uuid);
        ItemStack[] snapshot = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            ItemStack it = inv.getItem(i);
            snapshot[i] = (it == null) ? null : it.clone();
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (lockFor(uuid)) {
                org.bukkit.configuration.file.YamlConfiguration cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
                cfg.set("secure.size", Math.max(cfg.getInt("secure.size", 0), size));
                cfg.set("secure.contents", snapshot);
                try {
                    cfg.save(f);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public boolean isAllowedInSecureBox(ItemStack item) {
        if (item == null || item.getType().isAir()) return true;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return true;

        List<Component> lore = meta.lore();
        if (lore == null) return true;

        for (Component line : lore) {
            String plain = PlainTextComponentSerializer.plainText().serialize(line);
            if (plain.contains("不可存储至安全箱")) {
                return false;
            }
        }
        return true;
    }

    private ItemStack createLockedSlotItem() {
        ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            it.setItemMeta(meta);
        }
        return it;
    }

    public static final class SecureContainerHolder implements InventoryHolder {
        private final UUID owner;
        private final int slots;

        public SecureContainerHolder(UUID owner, int slots) {
            this.owner = owner;
            this.slots = slots;
        }

        public UUID owner() {
            return owner;
        }

        public int slots() {
            return slots;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
