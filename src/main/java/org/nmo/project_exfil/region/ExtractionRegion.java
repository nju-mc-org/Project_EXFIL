package org.nmo.project_exfil.region;

import org.bukkit.util.BoundingBox;

public class ExtractionRegion {
    private final String worldName;
    private final BoundingBox box;

    public ExtractionRegion(String worldName, BoundingBox box) {
        this.worldName = worldName;
        this.box = box;
    }

    public String getWorldName() {
        return worldName;
    }

    public BoundingBox getBox() {
        return box;
    }
}
