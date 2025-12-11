package org.nmo.project_exfil.region;

import org.bukkit.util.BoundingBox;

public class NPCRegion {
    private final String worldName;
    private final BoundingBox box;
    private final int count;

    public NPCRegion(String worldName, BoundingBox box, int count) {
        this.worldName = worldName;
        this.box = box;
        this.count = count;
    }

    public String getWorldName() {
        return worldName;
    }

    public BoundingBox getBox() {
        return box;
    }

    public int getCount() {
        return count;
    }
}
