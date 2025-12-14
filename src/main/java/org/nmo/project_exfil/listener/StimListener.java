package org.nmo.project_exfil.listener;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.integration.itemsadder.ItemsAdderIntegration;
import org.nmo.project_exfil.util.DependencyHelper;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StimListener implements Listener {

    private final ProjectEXFILPlugin plugin;

    // simple global cooldown per stim family
    private static final long COOLDOWN_MS = 30_000;
    private final Map<UUID, Long> lastUse = new ConcurrentHashMap<>();

    private static final Map<String, String> DISPLAY = Map.of(
        "ci:zsq1", "SJ6",
        "ci:zsq2", "Propital",
        "ci:zsq3", "Zagustin",
        "ci:zsq4", "eTG-change",
        "ci:zsq5", "M.U.L.E.",
        "ci:zsq6", "Obdolbos 2"
    );

    public StimListener(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onUseStim(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.SPECTATOR) return;

        ItemStack item = e.getItem();
        if (item == null || item.getType().isAir()) return;

        if (!DependencyHelper.isItemsAdderEnabled() || !ItemsAdderIntegration.isReady()) return;

        CustomStack stack = CustomStack.byItemStack(item);
        if (stack == null) return;

        String id = stack.getNamespacedID();
        if (!DISPLAY.containsKey(id)) return;

        e.setCancelled(true);

        long now = System.currentTimeMillis();
        long last = lastUse.getOrDefault(p.getUniqueId(), 0L);
        if (now - last < COOLDOWN_MS) {
            long left = (COOLDOWN_MS - (now - last) + 999) / 1000;
            plugin.getLanguageManager().send(p, "exfil.stim.cooldown", Placeholder.unparsed("time", String.valueOf(left)));
            return;
        }

        applyEffects(p, id);
        consumeOne(p);
        lastUse.put(p.getUniqueId(), now);

        plugin.getLanguageManager().send(p, "exfil.stim.used", Placeholder.unparsed("name", DISPLAY.get(id)));
    }

    private void applyEffects(Player p, String id) {
        // durations are in ticks
        switch (id) {
            case "ci:zsq1" -> {
                // SJ6: movement burst
                add(p, PotionEffectType.SPEED, 20 * 120, 1);
                add(p, PotionEffectType.JUMP_BOOST, 20 * 120, 0);
                add(p, PotionEffectType.HUNGER, 20 * 30, 0); // side effect
            }
            case "ci:zsq2" -> {
                // Propital: sustained regen
                add(p, PotionEffectType.REGENERATION, 20 * 60, 0);
                add(p, PotionEffectType.ABSORPTION, 20 * 60, 0);
                add(p, PotionEffectType.NAUSEA, 20 * 8, 0); // side effect
            }
            case "ci:zsq3" -> {
                // Zagustin: stop bleeding (no bleeding system yet) -> resistance + short regen
                add(p, PotionEffectType.RESISTANCE, 20 * 60, 0);
                add(p, PotionEffectType.REGENERATION, 20 * 15, 1);
                add(p, PotionEffectType.WEAKNESS, 20 * 10, 0); // side effect
            }
            case "ci:zsq4" -> {
                // eTG-change: strong regen, heavy side effect
                add(p, PotionEffectType.REGENERATION, 20 * 20, 2);
                add(p, PotionEffectType.ABSORPTION, 20 * 90, 1);
                add(p, PotionEffectType.WITHER, 20 * 6, 0); // side effect
            }
            case "ci:zsq5" -> {
                // M.U.L.E.: simulate "carry" with strength + resistance, slight hunger
                add(p, PotionEffectType.STRENGTH, 20 * 180, 0);
                add(p, PotionEffectType.RESISTANCE, 20 * 180, 0);
                add(p, PotionEffectType.HUNGER, 20 * 45, 1); // side effect
            }
            case "ci:zsq6" -> {
                // Obdolbos 2: roll random cocktail
                double r = Math.random();
                if (r < 0.33) {
                    add(p, PotionEffectType.SPEED, 20 * 90, 1);
                    add(p, PotionEffectType.STRENGTH, 20 * 90, 0);
                    add(p, PotionEffectType.NAUSEA, 20 * 10, 0);
                } else if (r < 0.66) {
                    add(p, PotionEffectType.REGENERATION, 20 * 45, 1);
                    add(p, PotionEffectType.RESISTANCE, 20 * 45, 0);
                    add(p, PotionEffectType.BLINDNESS, 20 * 4, 0);
                } else {
                    add(p, PotionEffectType.ABSORPTION, 20 * 90, 1);
                    add(p, PotionEffectType.HASTE, 20 * 90, 0);
                    add(p, PotionEffectType.POISON, 20 * 6, 0);
                }
            }
        }
    }

    private void add(Player p, PotionEffectType type, int durationTicks, int amplifier) {
        if (type == null) return;
        p.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, true, true, true));
    }

    private void consumeOne(Player player) {
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
