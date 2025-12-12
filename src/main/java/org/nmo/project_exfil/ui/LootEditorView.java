package org.nmo.project_exfil.ui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.manager.LootManager;
import org.nmo.project_exfil.manager.LootManager.LootItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LootEditorView implements Listener {

    private final ProjectEXFILPlugin plugin;
    private final LootManager lootManager;
    private final Map<UUID, LootItem> editingChance = new HashMap<>();

    public LootEditorView(ProjectEXFILPlugin plugin, LootManager lootManager) {
        this.plugin = plugin;
        this.lootManager = lootManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        ChestGui gui = new ChestGui(6, PlainTextComponentSerializer.plainText().serialize(plugin.getLanguageManager().getMessage("exfil.loot.editor.title")));
        
        gui.setOnGlobalClick(event -> {
            event.setCancelled(true);
            if (event.getClickedInventory() == event.getView().getBottomInventory()) {
                // Handle adding items from bottom inventory
                if (event.isShiftClick()) {
                    ItemStack item = event.getCurrentItem();
                    if (item != null && !item.getType().isAir()) {
                        LootItem newItem = new LootItem(item.clone(), 0.1);
                        lootManager.getLootTable().add(newItem);
                        lootManager.saveLootTable();
                        open(player); // Refresh
                    }
                } else {
                    // Allow picking up items from bottom inventory?
                    // If we cancel global click, they can't move items in their own inventory.
                    // We should probably allow bottom inventory interaction.
                    event.setCancelled(false);
                }
            }
        });

        PaginatedPane pages = new PaginatedPane(0, 0, 9, 5);
        List<LootItem> lootTable = lootManager.getLootTable();
        List<GuiItem> items = new ArrayList<>();

        for (LootItem lootItem : lootTable) {
            ItemStack displayItem = lootItem.getItem().clone();
            ItemMeta meta = displayItem.getItemMeta();
            List<Component> lore = meta.lore();
            if (lore == null) lore = new ArrayList<>();
            lore.add(plugin.getLanguageManager().getMessage("exfil.loot.editor.chance", Placeholder.unparsed("chance", String.valueOf(lootItem.getChance() * 100))));
            lore.add(plugin.getLanguageManager().getMessage("exfil.loot.editor.edit_chance"));
            lore.add(plugin.getLanguageManager().getMessage("exfil.loot.editor.remove"));
            meta.lore(lore);
            displayItem.setItemMeta(meta);

            items.add(new GuiItem(displayItem, event -> {
                if (event.isLeftClick()) {
                    // Edit chance
                    editingChance.put(player.getUniqueId(), lootItem);
                    gui.getInventory().close();
                    plugin.getLanguageManager().send(player, "exfil.loot.editor.enter_chance");
                } else if (event.isRightClick()) {
                    // Remove
                    lootTable.remove(lootItem);
                    lootManager.saveLootTable();
                    open(player); // Refresh
                }
            }));
        }
        
        pages.populateWithGuiItems(items);
        gui.addPane(pages);

        // Navigation Pane
        StaticPane navPane = new StaticPane(0, 5, 9, 1);
        
        ItemStack prevItem = new ItemStack(Material.ARROW);
        ItemMeta prevMeta = prevItem.getItemMeta();
        prevMeta.displayName(plugin.getLanguageManager().getMessage("exfil.player_select.prev_page"));
        prevItem.setItemMeta(prevMeta);
        
        navPane.addItem(new GuiItem(prevItem, event -> {
            if (pages.getPage() > 0) {
                pages.setPage(pages.getPage() - 1);
                gui.update();
            }
        }), 0, 0);
        
        ItemStack nextItem = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = nextItem.getItemMeta();
        nextMeta.displayName(plugin.getLanguageManager().getMessage("exfil.player_select.next_page"));
        nextItem.setItemMeta(nextMeta);
        
        navPane.addItem(new GuiItem(nextItem, event -> {
            if (pages.getPage() < pages.getPages() - 1) {
                pages.setPage(pages.getPage() + 1);
                gui.update();
            }
        }), 8, 0);
        
        // Add Item Info (Center)
        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.displayName(Component.text("Drag & Drop items from inventory to add", NamedTextColor.GRAY));
        infoItem.setItemMeta(infoMeta);
        navPane.addItem(new GuiItem(infoItem), 4, 0);

        gui.addPane(navPane);
        gui.show(player);
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (editingChance.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            LootItem item = editingChance.remove(player.getUniqueId());
            
            String message = PlainTextComponentSerializer.plainText().serialize(event.message());
            
            try {
                double chance = Double.parseDouble(message);
                if (chance < 0 || chance > 1) {
                    plugin.getLanguageManager().send(player, "exfil.loot.editor.invalid_chance");
                } else {
                    item.setChance(chance);
                    lootManager.saveLootTable();
                    plugin.getLanguageManager().send(player, "exfil.loot.editor.chance_updated", Placeholder.unparsed("chance", String.valueOf(chance * 100)));
                }
            } catch (NumberFormatException e) {
                plugin.getLanguageManager().send(player, "exfil.command.invalid_number");
            }
            
            // Re-open inventory on main thread
            Bukkit.getScheduler().runTask(plugin, () -> open(player));
        }
    }
}
