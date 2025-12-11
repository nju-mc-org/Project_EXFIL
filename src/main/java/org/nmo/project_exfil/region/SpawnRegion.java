package org.nmo.project_exfil.region;

public class SpawnRegion {
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final double radius;

    public SpawnRegion(String worldName, double x, double y, double z, double radius) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
    }

    public String getWorldName() {
        return worldName;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public double getRadius() {
        return radius;
    }
}
