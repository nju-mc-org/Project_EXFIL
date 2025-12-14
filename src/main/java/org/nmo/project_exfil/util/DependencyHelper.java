package org.nmo.project_exfil.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.manager.LanguageManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Arrays;
import java.util.List;

public class DependencyHelper {

    private static final java.util.Map<String, Boolean> enabledCache = new java.util.concurrent.ConcurrentHashMap<>();

    private static boolean isPluginEnabledCached(String pluginName) {
        return enabledCache.computeIfAbsent(pluginName, n -> Bukkit.getPluginManager().isPluginEnabled(n));
    }

    public static boolean isPlaceholderAPIEnabled() {
        return isPluginEnabledCached("PlaceholderAPI");
    }

    public static boolean isDecentHologramsEnabled() {
        return isPluginEnabledCached("DecentHolograms");
    }

    public static boolean isTABEnabled() {
        return isPluginEnabledCached("TAB");
    }

    public static boolean isLuckPermsEnabled() {
        return isPluginEnabledCached("LuckPerms");
    }

    public static boolean isXConomyEnabled() {
        return isPluginEnabledCached("XConomy");
    }

    public static boolean isItemsAdderEnabled() {
        return isPluginEnabledCached("ItemsAdder");
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

    public static void createStaticExtractionHologram(String name, Location loc) {
        if (isDecentHologramsEnabled()) {
            DecentHologramsHook.createStatic(name, loc);
        }
    }

    public static void removeStaticExtractionHologram(String name) {
        if (isDecentHologramsEnabled()) {
            DecentHologramsHook.removeStatic(name);
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

    // World Alias Usage
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
            LanguageManager lang = ProjectEXFILPlugin.getPlugin().getLanguageManager();
            String title = LegacyComponentSerializer.legacySection().serialize(lang.getMessage("exfil.hologram.extraction.extracting"));
            
            try {
                eu.decentsoftware.holograms.api.holograms.Hologram hologram = eu.decentsoftware.holograms.api.DHAPI.getHologram(holoName);
                if (hologram == null) {
                    List<String> lines = Arrays.asList(title, "§e" + timeLeft + "s");
                    hologram = eu.decentsoftware.holograms.api.DHAPI.createHologram(holoName, loc, lines);
                    hologram.setDefaultVisibleState(false);
                    hologram.setShowPlayer(player);
                } else {
                    eu.decentsoftware.holograms.api.DHAPI.setHologramLine(hologram, 1, "§e" + timeLeft + "s");
                    eu.decentsoftware.holograms.api.DHAPI.moveHologram(hologram, loc);
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

        static void createStatic(String name, Location loc) {
            // Sanitize name for hologram ID (alphanumeric, underscores, dashes only)
            String safeName = name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
            String holoName = "exfil_static_" + safeName;
            LanguageManager lang = ProjectEXFILPlugin.getPlugin().getLanguageManager();
            String title = LegacyComponentSerializer.legacySection().serialize(lang.getMessage("exfil.hologram.extraction.title"));
            
            try {
                eu.decentsoftware.holograms.api.holograms.Hologram hologram = eu.decentsoftware.holograms.api.DHAPI.getHologram(holoName);
                if (hologram != null) {
                    eu.decentsoftware.holograms.api.DHAPI.removeHologram(holoName);
                }
                List<String> lines = Arrays.asList(title, "§e" + name);
                eu.decentsoftware.holograms.api.DHAPI.createHologram(holoName, loc, lines);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        static void removeStatic(String name) {
            String safeName = name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
            String holoName = "exfil_static_" + safeName;
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
                        LanguageManager lang = ProjectEXFILPlugin.getPlugin().getLanguageManager();
                        String header = LegacyComponentSerializer.legacySection().serialize(lang.getMessage("exfil.tab.extracting"));
                        me.neznamy.tab.api.TabAPI.getInstance().getHeaderFooterManager().setHeader(tabPlayer, header);
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
