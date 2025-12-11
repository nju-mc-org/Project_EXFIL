package org.nmo.project_exfil.region;

public class CombatRegion {
    private final String worldName;
    private final double minX;
    private final double minZ;
    private final double maxX;
    private final double maxZ;

    public CombatRegion(String worldName, double minX, double minZ, double maxX, double maxZ) {
        this.worldName = worldName;
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
    }

    public String getWorldName() {
        return worldName;
    }

    public double getMinX() {
        return minX;
    }

    public double getMinZ() {
        return minZ;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMaxZ() {
        return maxZ;
    }

    public boolean contains(double x, double z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }
}
