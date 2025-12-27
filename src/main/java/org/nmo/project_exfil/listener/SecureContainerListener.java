package org.nmo.project_exfil.listener;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.integration.itemsadder.ItemsAdderIntegration;
import org.nmo.project_exfil.manager.SecureContainerManager;
import org.nmo.project_exfil.util.DependencyHelper;

import java.util.Map;

public class SecureContainerListener implements Listener {

    private static final Map<String, Integer> TOKENS = Map.of(
        "ci:2x2", 4,
        "ci:2x3", 6,
        "ci:2x4", 8,
        "ci:3x3", 9,
        "ci:3x4", 12,
        "ci:3x5", 15
    );

    private final SecureContainerManager secure;

    public SecureContainerListener(ProjectEXFILPlugin plugin, SecureContainerManager secure) {
        this.secure = secure;
    }

    @EventHandler
    public void onUseToken(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.SPECTATOR) return;

        ItemStack item = e.getItem();
        if (item == null || item.getType().isAir()) return;

        if (!DependencyHelper.isItemsAdderEnabled() || !ItemsAdderIntegration.isReady()) return;

        CustomStack stack = CustomStack.byItemStack(item);
        if (stack == null) return;

        Integer size = TOKENS.get(stack.getNamespacedID());
        if (size == null) return;

        e.setCancelled(true);
        boolean activated = secure.activate(p, size);
        if (activated) {
            consumeOne(p, item);
            secure.open(p);
        }
    }

    @EventHandler
    public void onSecureClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof SecureContainerManager.SecureContainerHolder holder)) return;

        Inventory top = e.getView().getTopInventory();
        int allowedSlots = holder.slots();

        // Prevent interaction with padding slots
        if (e.getClickedInventory() == top && e.getSlot() >= allowedSlots) {
            e.setCancelled(true);
            return;
        }

        // Prevent shift-click putting items into padding slots
        if (e.isShiftClick() && e.getClickedInventory() != top) {
            ItemStack moving = e.getCurrentItem();
            if (moving != null && !moving.getType().isAir()) {
                if (!secure.isAllowedInSecureBox(moving)) {
                    e.setCancelled(true);
                }
            }
        }

        // Prevent placing forbidden items (cursor) into allowed slots
        if (e.getClickedInventory() == top && e.getSlot() < allowedSlots) {
            ItemStack cursor = e.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                if (!secure.isAllowedInSecureBox(cursor)) {
                    e.setCancelled(true);
                }
            }
        }

        // Prevent collecting forbidden items via number-key swap etc.
        ItemStack current = e.getCurrentItem();
        if (e.getClickedInventory() == top && e.getSlot() < allowedSlots) {
            if (current != null && !current.getType().isAir() && !secure.isAllowedInSecureBox(current)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onSecureDrag(InventoryDragEvent e) {
        if (!(e.getInventory().getHolder() instanceof SecureContainerManager.SecureContainerHolder holder)) return;
        int allowedSlots = holder.slots();

        for (int rawSlot : e.getRawSlots()) {
            if (rawSlot < e.getView().getTopInventory().getSize()) {
                // rawSlot indexes into top inventory first
                if (rawSlot >= allowedSlots) {
                    e.setCancelled(true);
                    return;
                }
            }
        }

        ItemStack cursor = e.getOldCursor();
        if (cursor != null && !cursor.getType().isAir() && !secure.isAllowedInSecureBox(cursor)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onSecureClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof SecureContainerManager.SecureContainerHolder holder)) return;
        secure.saveContents(holder.owner(), holder.slots(), e.getInventory());
    }

    private void consumeOne(Player player, ItemStack inHand) {
        // PlayerInteractEvent gives a copy; mutate actual slot to be safe
        int slot = player.getInventory().getHeldItemSlot();
        ItemStack real = player.getInventory().getItem(slot);
        if (real == null || real.getType() == Material.AIR) return;

        if (real.getAmount() <= 1) {
            player.getInventory().setItem(slot, null);
        } else {
            real.setAmount(real.getAmount() - 1);
        }
    }
}
