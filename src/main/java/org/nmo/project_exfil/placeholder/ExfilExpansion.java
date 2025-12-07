package org.nmo.project_exfil.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.jetbrains.annotations.NotNull;

public class ExfilExpansion extends PlaceholderExpansion {

    private final ProjectEXFILPlugin plugin;

    public ExfilExpansion(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "exfil";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("version")) {
            return plugin.getDescription().getVersion();
        }
        
        if (params.equalsIgnoreCase("author")) {
            return getAuthor();
        }

        return null;
    }
}
