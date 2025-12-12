package org.nmo.project_exfil.command.sub;

import org.bukkit.command.CommandSender;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.command.SubCommand;
import org.nmo.project_exfil.manager.MapManager.GameMap;
import org.nmo.project_exfil.region.ExtractionRegion;
import org.nmo.project_exfil.region.CombatRegion;
import org.nmo.project_exfil.region.LootRegion;
import org.nmo.project_exfil.region.NPCRegion;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ListCommand implements SubCommand {

    private final ProjectEXFILPlugin plugin;

    public ListCommand(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getLanguageManager().send(sender, "exfil.command.usage_list");
            return;
        }

        if (args[1].equalsIgnoreCase("maps")) {
            plugin.getLanguageManager().send(sender, "exfil.list.maps.header");
            for (GameMap map : plugin.getMapManager().getMaps()) {
                plugin.getLanguageManager().send(sender, "exfil.list.maps.format", 
                    Placeholder.unparsed("name", map.getDisplayName()),
                    Placeholder.unparsed("id", map.getId()));
            }
            return;
        }

        if (args[1].equalsIgnoreCase("extracts")) {
            plugin.getLanguageManager().send(sender, "exfil.list.extracts.header");
            Map<String, ExtractionRegion> regions = plugin.getRegionManager().getAllExtractionRegions();
            
            List<String> worlds = new ArrayList<>();
            for (ExtractionRegion r : regions.values()) {
                if (!worlds.contains(r.getWorldName())) worlds.add(r.getWorldName());
            }
            
            for (String world : worlds) {
                plugin.getLanguageManager().send(sender, "exfil.list.extracts.world_header", Placeholder.unparsed("world", world));
                for (Map.Entry<String, ExtractionRegion> entry : regions.entrySet()) {
                    if (entry.getValue().getWorldName().equals(world)) {
                        plugin.getLanguageManager().send(sender, "exfil.list.extracts.format", Placeholder.unparsed("name", entry.getKey()));
                    }
                }
            }
            return;
        }

        if (args[1].equalsIgnoreCase("combat")) {
            plugin.getLanguageManager().send(sender, "exfil.list.combat.header");
            Map<String, CombatRegion> regions = plugin.getRegionManager().getAllCombatRegions();
            
            for (Map.Entry<String, CombatRegion> entry : regions.entrySet()) {
                plugin.getLanguageManager().send(sender, "exfil.list.combat.format", 
                    Placeholder.unparsed("world", entry.getKey()));
            }
            return;
        }

        if (args[1].equalsIgnoreCase("npc")) {
            plugin.getLanguageManager().send(sender, "exfil.list.npc.header");
            Map<String, NPCRegion> regions = plugin.getRegionManager().getAllNPCRegions();
            
            List<String> worlds = new ArrayList<>();
            for (NPCRegion r : regions.values()) {
                if (!worlds.contains(r.getWorldName())) worlds.add(r.getWorldName());
            }
            
            for (String world : worlds) {
                plugin.getLanguageManager().send(sender, "exfil.list.npc.world_header", Placeholder.unparsed("world", world));
                for (Map.Entry<String, NPCRegion> entry : regions.entrySet()) {
                    if (entry.getValue().getWorldName().equals(world)) {
                        plugin.getLanguageManager().send(sender, "exfil.list.npc.format", 
                            Placeholder.unparsed("name", entry.getKey()),
                            Placeholder.unparsed("count", String.valueOf(entry.getValue().getCount())));
                    }
                }
            }
            return;
        }

        if (args[1].equalsIgnoreCase("loot")) {
            plugin.getLanguageManager().send(sender, "exfil.list.loot.header");
            Map<String, LootRegion> regions = plugin.getRegionManager().getAllLootRegions();
            
            List<String> worlds = new ArrayList<>();
            for (LootRegion r : regions.values()) {
                if (!worlds.contains(r.getWorldName())) worlds.add(r.getWorldName());
            }
            
            for (String world : worlds) {
                plugin.getLanguageManager().send(sender, "exfil.list.loot.world_header", Placeholder.unparsed("world", world));
                for (Map.Entry<String, LootRegion> entry : regions.entrySet()) {
                    if (entry.getValue().getWorldName().equals(world)) {
                        plugin.getLanguageManager().send(sender, "exfil.list.loot.format", 
                            Placeholder.unparsed("name", entry.getKey()),
                            Placeholder.unparsed("count", String.valueOf(entry.getValue().getCount())));
                    }
                }
            }
            return;
        }

        plugin.getLanguageManager().send(sender, "exfil.command.usage_list");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 2) {
            List<String> subCmds = new ArrayList<>();
            subCmds.add("maps");
            subCmds.add("extracts");
            subCmds.add("combat");
            subCmds.add("npc");
            subCmds.add("loot");
            
            String input = args[1].toLowerCase();
            for (String cmd : subCmds) {
                if (cmd.startsWith(input)) {
                    completions.add(cmd);
                }
            }
        }
        return completions;
    }

    @Override
    public String getPermission() {
        return "exfil.admin";
    }
}
