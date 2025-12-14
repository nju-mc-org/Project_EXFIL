package org.nmo.project_exfil.manager;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.nmo.project_exfil.ProjectEXFILPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

public class StashManager {

    private static final int PAGE_SIZE = 45;

    private final ProjectEXFILPlugin plugin;
    private final File userDataFolder;
    private final java.util.Map<UUID, Object> fileLocks = new java.util.concurrent.ConcurrentHashMap<>();

    public StashManager(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
        this.userDataFolder = new File(plugin.getDataFolder(), "userdata");
        if (!userDataFolder.exists()) {
            userDataFolder.mkdirs();
        }
    }

    private Object lockFor(UUID uuid) {
        return fileLocks.computeIfAbsent(uuid, k -> new Object());
    }

    public void saveStash(Player player, ItemStack[] contents, int page) {
        File userFile = new File(userDataFolder, player.getUniqueId() + ".yml");

        ItemStack[] snapshot = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            snapshot[i] = (it == null) ? null : it.clone();
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (lockFor(player.getUniqueId())) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(userFile);
                config.set("stash.page_" + page, snapshot);
                try {
                    config.save(userFile);
                } catch (IOException e) {
                    e.printStackTrace();
                    Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage("§cError saving stash data!"));
                }
            }
        });
    }

    public void removeStashPage(Player player, int page) {
        File userFile = new File(userDataFolder, player.getUniqueId() + ".yml");
        if (!userFile.exists()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (lockFor(player.getUniqueId())) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(userFile);
                config.set("stash.page_" + page, null);
                try {
                    config.save(userFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void sortStash(Player player) {
        File userFile = new File(userDataFolder, player.getUniqueId() + ".yml");
        if (!userFile.exists()) return;

        synchronized (lockFor(player.getUniqueId())) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(userFile);
            if (!config.isConfigurationSection("stash")) return;

        // 1. Collect all items from all pages
        List<ItemStack> allItems = new ArrayList<>();
        for (String key : config.getConfigurationSection("stash").getKeys(false)) {
            if (key.startsWith("page_")) {
                List<?> list = config.getList("stash." + key);
                if (list != null) {
                    for (Object obj : list) {
                        if (obj instanceof ItemStack) {
                            allItems.add(cleanItem((ItemStack) obj));
                        }
                    }
                }
            }
        }

        // 2. Merge stacks
        List<ItemStack> mergedItems = new ArrayList<>();
        for (ItemStack item : allItems) {
            if (item == null || item.getType().isAir()) continue;
            
            boolean merged = false;
            for (ItemStack existing : mergedItems) {
                if (existing.isSimilar(item)) {
                    int maxStack = existing.getMaxStackSize();
                    int space = maxStack - existing.getAmount();
                    if (space > 0) {
                        int toAdd = Math.min(space, item.getAmount());
                        existing.setAmount(existing.getAmount() + toAdd);
                        item.setAmount(item.getAmount() - toAdd);
                        if (item.getAmount() <= 0) {
                            merged = true;
                            break;
                        }
                    }
                }
            }
            if (!merged && item.getAmount() > 0) {
                mergedItems.add(item);
            }
        }

        // 3. Sort items
        mergedItems.sort(new Comparator<ItemStack>() {
            @Override
            public int compare(ItemStack i1, ItemStack i2) {
                if (i1 == null && i2 == null) return 0;
                if (i1 == null) return 1;
                if (i2 == null) return -1;

                // Compare Type
                int typeCompare = i1.getType().compareTo(i2.getType());
                if (typeCompare != 0) return typeCompare;

                // Compare Display Name
                String n1 = i1.hasItemMeta() && i1.getItemMeta().hasDisplayName() ? i1.getItemMeta().getDisplayName() : "";
                String n2 = i2.hasItemMeta() && i2.getItemMeta().hasDisplayName() ? i2.getItemMeta().getDisplayName() : "";
                int nameCompare = n1.compareTo(n2);
                if (nameCompare != 0) return nameCompare;

                // Compare Amount (Descending)
                return Integer.compare(i2.getAmount(), i1.getAmount());
            }
        });

        // 4. Save back to pages
        config.set("stash", null); // Clear old stash
        
        int pageSize = 45;
        int totalItems = mergedItems.size();
        int pageCount = (int) Math.ceil((double) totalItems / pageSize);
        if (pageCount == 0) pageCount = 1; // At least one page

        for (int i = 0; i < pageCount; i++) {
            int start = i * pageSize;
            int end = Math.min(start + pageSize, totalItems);
            List<ItemStack> pageItems = new ArrayList<>(mergedItems.subList(start, end));
            
            // Fill the rest of the page with nulls/air if needed to maintain array size? 
            // Actually YamlConfiguration handles lists fine. But loadStash expects array.
            // Let's just save the list.
            
            config.set("stash.page_" + i, pageItems);
        }

        try {
            config.save(userFile);
        } catch (IOException e) {
            e.printStackTrace();
            player.sendMessage("§cError saving sorted stash!");
        }
        }
    }

    private ItemStack cleanItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return item;
        if (!item.hasItemMeta()) return item;
        
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, "if-uuid");
        if (meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE_ARRAY)) {
            meta.getPersistentDataContainer().remove(key);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void depositItemsAsync(Player player, List<ItemStack> items) {
        if (items == null || items.isEmpty()) return;
        File userFile = new File(userDataFolder, player.getUniqueId() + ".yml");

        List<ItemStack> snapshot = new ArrayList<>();
        for (ItemStack it : items) {
            if (it != null && !it.getType().isAir()) snapshot.add(cleanItem(it.clone()));
        }
        if (snapshot.isEmpty()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (lockFor(player.getUniqueId())) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(userFile);
                if (!config.isConfigurationSection("stash")) {
                    config.createSection("stash");
                }

                // Load existing pages
                int maxPage = -1;
                if (config.isConfigurationSection("stash")) {
                    for (String key : config.getConfigurationSection("stash").getKeys(false)) {
                        if (key.startsWith("page_")) {
                            try {
                                maxPage = Math.max(maxPage, Integer.parseInt(key.substring(5)));
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
                if (maxPage < 0) maxPage = 0;

                List<ItemStack[]> pages = new ArrayList<>();
                for (int page = 0; page <= maxPage; page++) {
                    pages.add(loadPageArray(config, page));
                }

                for (ItemStack item : snapshot) {
                    addToPages(pages, item);
                }

                for (int page = 0; page < pages.size(); page++) {
                    config.set("stash.page_" + page, pages.get(page));
                }

                try {
                    config.save(userFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private ItemStack[] loadPageArray(YamlConfiguration config, int page) {
        ItemStack[] arr = new ItemStack[PAGE_SIZE];
        List<?> list = config.getList("stash.page_" + page);
        if (list == null) return arr;
        for (int i = 0; i < Math.min(PAGE_SIZE, list.size()); i++) {
            Object obj = list.get(i);
            if (obj instanceof ItemStack) {
                arr[i] = (ItemStack) obj;
            }
        }
        return arr;
    }

    private void addToPages(List<ItemStack[]> pages, ItemStack item) {
        if (item == null || item.getType().isAir()) return;

        int amount = item.getAmount();

        // merge first
        for (ItemStack[] page : pages) {
            for (int i = 0; i < PAGE_SIZE && amount > 0; i++) {
                ItemStack slot = page[i];
                if (slot != null && slot.isSimilar(item)) {
                    int space = slot.getMaxStackSize() - slot.getAmount();
                    if (space <= 0) continue;
                    int toAdd = Math.min(space, amount);
                    slot.setAmount(slot.getAmount() + toAdd);
                    amount -= toAdd;
                }
            }
            if (amount <= 0) return;
        }

        // empty slots
        while (amount > 0) {
            boolean placed = false;
            for (ItemStack[] page : pages) {
                for (int i = 0; i < PAGE_SIZE; i++) {
                    if (page[i] == null || page[i].getType().isAir()) {
                        ItemStack toPut = item.clone();
                        int stack = Math.min(toPut.getMaxStackSize(), amount);
                        toPut.setAmount(stack);
                        page[i] = toPut;
                        amount -= stack;
                        placed = true;
                        break;
                    }
                }
                if (placed) break;
            }
            if (!placed) {
                pages.add(new ItemStack[PAGE_SIZE]);
            }
        }
    }

    public ItemStack[] loadStash(Player player, int page) {
        File userFile = new File(userDataFolder, player.getUniqueId() + ".yml");
        if (!userFile.exists()) {
            return new ItemStack[PAGE_SIZE];
        }

        synchronized (lockFor(player.getUniqueId())) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(userFile);
        
        // Migration check: if "stash" exists as a list but "stash.page_0" doesn't
        if (page == 0 && config.contains("stash") && !config.isConfigurationSection("stash")) {
             List<ItemStack> oldList = (List<ItemStack>) config.getList("stash");
             if (oldList != null) {
                 // Migrate to page 0
                 ItemStack[] oldItems = oldList.toArray(new ItemStack[0]);
                 // Resize to 45 if needed, or just take first 45
                 ItemStack[] newItems = new ItemStack[45];
                 for(int i=0; i<Math.min(oldItems.length, 45); i++) {
                     newItems[i] = oldItems[i];
                 }
                 config.set("stash", null); // Remove old
                 config.set("stash.page_0", newItems);
                 try { config.save(userFile); } catch (IOException e) {}
                 return newItems;
             }
        }

        List<ItemStack> list = (List<ItemStack>) config.getList("stash.page_" + page);
        ItemStack[] result = new ItemStack[PAGE_SIZE];
        if (list == null) {
            return result;
        }
        for (int i = 0; i < Math.min(PAGE_SIZE, list.size()); i++) {
            ItemStack it = list.get(i);
            result[i] = it;
        }
        return result;
        }
    }
}
