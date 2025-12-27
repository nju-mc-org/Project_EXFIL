package org.nmo.project_exfil.ui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nmo.project_exfil.manager.StashManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

public class StashView {

    private final StashManager stashManager;
    private final org.nmo.project_exfil.ProjectEXFILPlugin plugin;
    private static final int PAGE_SIZE = 45;
    private final Set<UUID> sortingPlayers = new HashSet<>();

    public StashView(StashManager stashManager) {
        this.stashManager = stashManager;
        this.plugin = org.nmo.project_exfil.ProjectEXFILPlugin.getPlugin();
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        // Check if player is in combat
        if (plugin.getGameManager().getPlayerInstance(player) != null) {
            plugin.getLanguageManager().send(player, "exfil.error.combat_stash");
            return;
        }

        Component title = plugin.getLanguageManager().getMessage("exfil.stash.title_page", Placeholder.unparsed("page", String.valueOf(page + 1)));
        ChestGui gui = new ChestGui(6, PlainTextComponentSerializer.plainText().serialize(title));
        
        // Handle global clicks for auto-add logic
        gui.setOnGlobalClick(event -> handleGlobalClick(event, gui, page));
        
        // Handle close to save
        gui.setOnClose(event -> handleClose(event, page));

        // Main Stash Pane
        StaticPane stashPane = new StaticPane(0, 0, 9, 5);
        
        ItemStack[] stashItems = stashManager.loadStash(player, page);
        if (stashItems != null) {
            for (int i = 0; i < Math.min(stashItems.length, PAGE_SIZE); i++) {
                if (stashItems[i] != null && !stashItems[i].getType().isAir()) {
                    stashPane.addItem(new GuiItem(stashItems[i]), i % 9, i / 9);
                }
            }
        }
        
        // Navigation Pane (Bottom Row)
        StaticPane navPane = new StaticPane(0, 5, 9, 1);
        
        // Filler
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.text(" "));
        filler.setItemMeta(fillerMeta);
        
        GuiItem fillerItem = new GuiItem(filler, event -> event.setCancelled(true));
        for (int i = 0; i < 9; i++) {
            navPane.addItem(fillerItem, i, 0);
        }

        // Previous Page
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta meta = prev.getItemMeta();
            meta.displayName(plugin.getLanguageManager().getMessage("exfil.stash.prev_page"));
            prev.setItemMeta(meta);
            navPane.addItem(new GuiItem(prev, event -> {
                open(player, page - 1);
            }), 0, 0);
        }

        // Sort Button
        ItemStack sort = new ItemStack(Material.HOPPER);
        ItemMeta sortMeta = sort.getItemMeta();
        sortMeta.displayName(plugin.getLanguageManager().getMessage("exfil.stash.sort"));
        sort.setItemMeta(sortMeta);
        navPane.addItem(new GuiItem(sort, event -> {
            // Mark player as sorting to prevent handleClose from overwriting
            sortingPlayers.add(player.getUniqueId());
            
            // Manually save current page state to disk so sortStash sees it
            saveCurrentPage(player, gui.getInventory(), page);
            
            // Perform global sort
            stashManager.sortStash(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            
            // Re-open (this triggers close of current inv)
            open(player, 0); 
        }), 2, 0);

        // Back Button
        ItemStack back = new ItemStack(Material.OAK_DOOR);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(plugin.getLanguageManager().getMessage("exfil.team.back"));
        back.setItemMeta(backMeta);
        navPane.addItem(new GuiItem(back, event -> {
            if (plugin.getMainMenuView() != null) {
                plugin.getMainMenuView().open(player);
            } else {
                player.closeInventory();
            }
        }), 6, 0);

        // Info Icon
        ItemStack info = new ItemStack(Material.CHEST);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(plugin.getLanguageManager().getMessage("exfil.stash.page_info", Placeholder.unparsed("page", String.valueOf(page + 1))));
        info.setItemMeta(infoMeta);
        navPane.addItem(new GuiItem(info, event -> event.setCancelled(true)), 4, 0);

        // Next Page
        ItemStack next = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = next.getItemMeta();
        nextMeta.displayName(plugin.getLanguageManager().getMessage("exfil.stash.next_page"));
        next.setItemMeta(nextMeta);
        navPane.addItem(new GuiItem(next, event -> {
            open(player, page + 1);
        }), 8, 0);

        gui.addPane(stashPane);
        gui.addPane(navPane);
        
        gui.show(player);
    }

    private void handleGlobalClick(InventoryClickEvent event, ChestGui gui, int page) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = gui.getInventory();
        
        if (clickedInventory == null) return;

        // Prevent manual placement in Stash (Top Inventory)
        if (clickedInventory == topInventory) {
            // Allow picking up items (if cursor is air), but prevent placing (if cursor is not air)
            if (event.getCursor() != null && !event.getCursor().getType().isAir()) {
                // If clicking inside stash with item on cursor -> Cancel
                event.setCancelled(true);
            }
            
            // Custom interaction logic
            // Only apply to stash slots (0-44), ignore navigation slots (45-53)
            if (event.getSlot() < 45) {
                if (event.getCurrentItem() != null && !event.getCurrentItem().getType().isAir()) {
                    event.setCancelled(true);
                    ItemStack clickedItem = event.getCurrentItem();
                    
                    if (event.isLeftClick()) {
                        // Left click: Take all to cursor (if cursor empty) or swap?
                        // User request: "Left click stash item -> Take this slot item"
                        // "Right click -> Take half, put to inventory directly"
                        // Update: "Unified back to backpack" -> Left click also moves to inventory
                        
                        if (event.getCursor() == null || event.getCursor().getType().isAir()) {
                            // Move ALL to player inventory
                            ItemStack toGive = clickedItem.clone();
                            
                            HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(toGive);
                            if (leftovers.isEmpty()) {
                                event.setCurrentItem(null);
                                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 1f);
                            } else {
                                // Inventory full or partial
                                int remaining = leftovers.get(0).getAmount();
                                clickedItem.setAmount(remaining);
                                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1f);
                            }
                        } else {
                            // Cursor not empty. If same item, stack?
                            if (event.getCursor().isSimilar(clickedItem)) {
                                int space = event.getCursor().getMaxStackSize() - event.getCursor().getAmount();
                                if (space > 0) {
                                    int toAdd = Math.min(space, clickedItem.getAmount());
                                    event.getCursor().setAmount(event.getCursor().getAmount() + toAdd);
                                    clickedItem.setAmount(clickedItem.getAmount() - toAdd);
                                    if (clickedItem.getAmount() <= 0) {
                                        event.setCurrentItem(null);
                                    }
                                    player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 1f);
                                }
                            }
                        }
                    } else if (event.isRightClick()) {
                        // Right click: Take half directly to player inventory
                        int amount = (int) Math.ceil(clickedItem.getAmount() / 2.0);
                        ItemStack toGive = clickedItem.clone();
                        toGive.setAmount(amount);
                        
                        HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(toGive);
                        if (leftovers.isEmpty()) {
                            clickedItem.setAmount(clickedItem.getAmount() - amount);
                            if (clickedItem.getAmount() <= 0) {
                                event.setCurrentItem(null);
                            }
                            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 1f);
                        } else {
                            // Inventory full or partial
                            int given = amount - leftovers.get(0).getAmount();
                            if (given > 0) {
                                clickedItem.setAmount(clickedItem.getAmount() - given);
                                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 1f);
                            } else {
                                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1f);
                            }
                        }
                    }
                } else if (event.getCursor() != null && !event.getCursor().getType().isAir()) {
                     // Clicking empty slot with cursor item
                     // Allow placing? Previous logic blocked placing.
                     // User request: "Left click inventory item -> Put to this slot" (This is for bottom inv)
                     // For top inv empty slot, standard behavior is place.
                     // But we blocked it above.
                     // Let's allow placing if it's a valid stash slot (0-44)
                     // if (event.getSlot() < 45) { // Already checked
                         event.setCancelled(false); // Allow default place
                     // }
                }
            }
        }
        
        // Auto-Add from Player Inventory (Bottom Inventory)
        if (clickedInventory == event.getView().getBottomInventory()) {
            if (event.getCurrentItem() != null && !event.getCurrentItem().getType().isAir()) {
                event.setCancelled(true);
                
                ItemStack item = event.getCurrentItem();
                
                if (event.isLeftClick()) {
                    // Left click: Put all to stash (auto-add)
                    ItemStack toAdd = item.clone();
                    HashMap<Integer, ItemStack> leftovers = addItemToStash(topInventory, toAdd);
                    
                    if (leftovers.isEmpty()) {
                        event.setCurrentItem(null);
                        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 1f);
                    } else {
                        item.setAmount(leftovers.get(0).getAmount());
                        event.setCurrentItem(item);
                        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1f);
                    }
                } else if (event.isRightClick()) {
                    // Right click: Put half to stash
                    int amount = (int) Math.ceil(item.getAmount() / 2.0);
                    ItemStack toAdd = item.clone();
                    toAdd.setAmount(amount);
                    
                    HashMap<Integer, ItemStack> leftovers = addItemToStash(topInventory, toAdd);
                    
                    // Calculate how many were actually added
                    int added = amount;
                    if (!leftovers.isEmpty()) {
                        added -= leftovers.get(0).getAmount();
                    }
                    
                    if (added > 0) {
                        item.setAmount(item.getAmount() - added);
                        if (item.getAmount() <= 0) {
                            event.setCurrentItem(null);
                        }
                        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 1f);
                    } else {
                        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1f);
                    }
                }
            }
        }
    }
    
    private HashMap<Integer, ItemStack> addItemToStash(Inventory inv, ItemStack item) {
        // Only try to add to slots 0-44
        HashMap<Integer, ItemStack> leftovers = new HashMap<>();
        int amount = item.getAmount();
        
        // First pass: merge with existing stacks
        for (int i = 0; i < 45; i++) {
            ItemStack slotItem = inv.getItem(i);
            if (slotItem != null && slotItem.isSimilar(item)) {
                int space = slotItem.getMaxStackSize() - slotItem.getAmount();
                if (space > 0) {
                    int toAdd = Math.min(space, amount);
                    slotItem.setAmount(slotItem.getAmount() + toAdd);
                    amount -= toAdd;
                    if (amount <= 0) break;
                }
            }
        }
        
        // Second pass: fill empty slots
        if (amount > 0) {
            for (int i = 0; i < 45; i++) {
                ItemStack slotItem = inv.getItem(i);
                if (slotItem == null || slotItem.getType().isAir()) {
                    ItemStack newItem = item.clone();
                    newItem.setAmount(amount);
                    inv.setItem(i, newItem);
                    amount = 0;
                    break;
                }
            }
        }
        
        if (amount > 0) {
            ItemStack remaining = item.clone();
            remaining.setAmount(amount);
            leftovers.put(0, remaining);
        }
        
        return leftovers;
    }

    private void handleClose(InventoryCloseEvent event, int page) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            
            // If player is sorting, skip saving (we already saved manually, and we don't want to overwrite sorted data)
            if (sortingPlayers.contains(player.getUniqueId())) {
                sortingPlayers.remove(player.getUniqueId());
                return;
            }
            
            saveCurrentPage(player, event.getInventory(), page);
        }
    }

    private void saveCurrentPage(Player player, Inventory inventory, int page) {
        ItemStack[] contents = inventory.getContents();
        ItemStack[] itemsToSave = new ItemStack[PAGE_SIZE];
        
        boolean isEmpty = true;
        for (int i = 0; i < PAGE_SIZE; i++) {
            itemsToSave[i] = cleanItem(contents[i]);
            if (itemsToSave[i] != null && !itemsToSave[i].getType().isAir()) {
                isEmpty = false;
            }
        }
        
        if (isEmpty && page > 0) {
            stashManager.removeStashPage(player, page);
        } else {
            stashManager.saveStash(player, itemsToSave, page);
            if (!sortingPlayers.contains(player.getUniqueId())) {
                plugin.getLanguageManager().send(player, "exfil.stash_saved");
            }
        }
    }

    private ItemStack cleanItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return item;
        if (!item.hasItemMeta()) return item;
        
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, "if-uuid");
        if (meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE_ARRAY)) {
            meta.getPersistentDataContainer().remove(key);
            item.setItemMeta(meta);
        }
        return item;
    }
}
