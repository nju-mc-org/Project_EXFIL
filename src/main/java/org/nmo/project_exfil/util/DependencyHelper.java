package org.nmo.project_exfil.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class DependencyHelper {

    public static boolean isPlaceholderAPIEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public static boolean isDecentHologramsEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("DecentHolograms");
    }

    public static boolean isTABEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("TAB");
    }

    public static boolean isLuckPermsEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("LuckPerms");
    }

    public static boolean isXConomyEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("XConomy");
    }

    public static boolean isMultiverseCoreEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("Multiverse-Core");
    }

    // PlaceholderAPI Usage
    public static String parsePlaceholders(Player player, String text) {
        if (isPlaceholderAPIEnabled()) {
            return PlaceholderAPIHook.parse(player, text);
        }
        return text;
    }

    // DecentHolograms Usage
    public static void createExtractionHologram(Player player, int timeLeft) {
        if (isDecentHologramsEnabled()) {
            DecentHologramsHook.create(player, timeLeft);
        }
    }

    public static void removeExtractionHologram(Player player) {
        if (isDecentHologramsEnabled()) {
            DecentHologramsHook.remove(player);
        }
    }

    // TAB Usage
    public static void setExtractionHeader(Player player, boolean extracting) {
        if (isTABEnabled()) {
            TABHook.setHeader(player, extracting);
        }
    }

    // LuckPerms Usage
    public static String getPlayerGroup(Player player) {
        if (isLuckPermsEnabled()) {
            return LuckPermsHook.getGroup(player);
        }
        return "default";
    }

    public static String getPlayerPrefix(Player player) {
        if (isLuckPermsEnabled()) {
            return LuckPermsHook.getPrefix(player);
        }
        return "";
    }

    // XConomy Usage
    public static java.math.BigDecimal getBalance(Player player) {
        if (isXConomyEnabled()) {
            return XConomyHook.getBalance(player);
        }
        return java.math.BigDecimal.ZERO;
    }

    // Multiverse-Core Usage
    public static String getWorldAlias(org.bukkit.World world) {
        return world.getName();
    }

    // Inner classes to prevent ClassNotFoundException
    private static class PlaceholderAPIHook {
        static String parse(Player player, String text) {
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
        }
    }

    private static class DecentHologramsHook {
        static void create(Player player, int timeLeft) {
            String holoName = "extract_" + player.getUniqueId();
            Location loc = player.getLocation().add(0, 2.5, 0);
            try {
                if (eu.decentsoftware.holograms.api.DHAPI.getHologram(holoName) == null) {
                    List<String> lines = Arrays.asList("§aExtraction", "§e" + timeLeft + "s");
                    eu.decentsoftware.holograms.api.DHAPI.createHologram(holoName, loc, lines);
                } else {
                    eu.decentsoftware.holograms.api.DHAPI.setHologramLine(eu.decentsoftware.holograms.api.DHAPI.getHologram(holoName), 1, "§e" + timeLeft + "s");
                    eu.decentsoftware.holograms.api.DHAPI.moveHologram(eu.decentsoftware.holograms.api.DHAPI.getHologram(holoName), loc);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        static void remove(Player player) {
            String holoName = "extract_" + player.getUniqueId();
            try {
                if (eu.decentsoftware.holograms.api.DHAPI.getHologram(holoName) != null) {
                    eu.decentsoftware.holograms.api.DHAPI.removeHologram(holoName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class TABHook {
        static void setHeader(Player player, boolean extracting) {
            try {
                me.neznamy.tab.api.TabPlayer tabPlayer = me.neznamy.tab.api.TabAPI.getInstance().getPlayer(player.getUniqueId());
                if (tabPlayer != null && me.neznamy.tab.api.TabAPI.getInstance().getHeaderFooterManager() != null) {
                    if (extracting) {
                        me.neznamy.tab.api.TabAPI.getInstance().getHeaderFooterManager().setHeader(tabPlayer, "§c§lEXTRACTING...");
                    } else {
                        me.neznamy.tab.api.TabAPI.getInstance().getHeaderFooterManager().setHeader(tabPlayer, "");
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    private static class LuckPermsHook {
        static String getGroup(Player player) {
            try {
                net.luckperms.api.LuckPerms api = net.luckperms.api.LuckPermsProvider.get();
                net.luckperms.api.model.user.User user = api.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    return user.getPrimaryGroup();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "default";
        }

        static String getPrefix(Player player) {
            try {
                net.luckperms.api.LuckPerms api = net.luckperms.api.LuckPermsProvider.get();
                net.luckperms.api.model.user.User user = api.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    String prefix = user.getCachedData().getMetaData().getPrefix();
                    return prefix != null ? prefix : "";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        }
    }

    private static class XConomyHook {
        static java.math.BigDecimal getBalance(Player player) {
            try {
                me.yic.xconomy.api.XConomyAPI api = new me.yic.xconomy.api.XConomyAPI();
                return api.getPlayerData(player.getUniqueId()).getBalance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return java.math.BigDecimal.ZERO;
        }
    }
}
