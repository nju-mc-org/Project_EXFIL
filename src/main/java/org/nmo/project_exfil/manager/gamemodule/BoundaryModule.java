package org.nmo.project_exfil.manager.gamemodule;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.manager.GameInstance;
import org.nmo.project_exfil.region.CombatRegion;
import org.nmo.project_exfil.manager.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import java.time.Duration;
import java.util.UUID;

public class BoundaryModule implements GameModule {

    @Override
    public void onTick(GameInstance game) {
        CombatRegion region = ProjectEXFILPlugin.getPlugin().getRegionManager().getCombatRegion(game.getTemplateName());
        if (region == null) return;

        LanguageManager lang = ProjectEXFILPlugin.getPlugin().getLanguageManager();

        for (UUID uuid : game.getPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.getWorld().equals(game.getBukkitWorld())) {
                if (!region.contains(p.getLocation().getX(), p.getLocation().getZ())) {
                    // Outside
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, false, false));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, false, false));
                    
                    Title title = Title.title(
                        lang.getMessage("exfil.boundary.warning.title"),
                        lang.getMessage("exfil.boundary.warning.subtitle"),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(1500), Duration.ofMillis(500))
                    );
                    p.showTitle(title);
                }
            }
        }
    }
}
