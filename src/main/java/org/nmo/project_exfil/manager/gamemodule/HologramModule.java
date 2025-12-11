package org.nmo.project_exfil.manager.gamemodule;

import org.bukkit.Location;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.manager.GameInstance;
import org.nmo.project_exfil.region.ExtractionRegion;
import org.nmo.project_exfil.util.DependencyHelper;

import java.util.Map;

public class HologramModule implements GameModule {

    @Override
    public void onStart(GameInstance game) {
        Map<String, ExtractionRegion> regions = ProjectEXFILPlugin.getPlugin().getRegionManager().getExtractionRegions(game.getTemplateName());
        for (Map.Entry<String, ExtractionRegion> entry : regions.entrySet()) {
            String name = entry.getKey();
            ExtractionRegion region = entry.getValue();
            
            String holoName = game.getInstanceId() + "_" + name;
            Location loc = new Location(game.getBukkitWorld(), region.getBox().getCenterX(), region.getBox().getCenterY(), region.getBox().getCenterZ());
            
            DependencyHelper.createStaticExtractionHologram(holoName, loc);
        }
    }

    @Override
    public void onEnd(GameInstance game) {
        Map<String, ExtractionRegion> regions = ProjectEXFILPlugin.getPlugin().getRegionManager().getExtractionRegions(game.getTemplateName());
        for (String name : regions.keySet()) {
            String holoName = game.getInstanceId() + "_" + name;
            DependencyHelper.removeStaticExtractionHologram(holoName);
        }
    }
}
