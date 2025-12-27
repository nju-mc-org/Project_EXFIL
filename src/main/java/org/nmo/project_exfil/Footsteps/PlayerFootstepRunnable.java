package org.nmo.project_exfil.footsteps;

import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

/**
 * The runnable instance for a single player
 */
public class PlayerFootstepRunnable extends BukkitRunnable {
    private final Player player;
    private final long tickInterval;
    private final long footstepIntervalSprint;
    private final long footstepIntervalWalk;
    private long sprintTickCounter = 0;
    private long walkTickCounter = 0;

    PlayerFootstepRunnable(@NotNull Player player, long tickInterval, long footstepIntervalSprint, long footstepIntervalWalk) {
        this.player = player;
        this.tickInterval = tickInterval;
        this.footstepIntervalSprint = footstepIntervalSprint;
        this.footstepIntervalWalk = footstepIntervalWalk;
        this.sprintTickCounter = footstepIntervalSprint;
        this.walkTickCounter = footstepIntervalWalk;
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            cancel();
            return;
        }
        Input playerInput = player.getCurrentInput();
        if (!(playerInput.isForward() | playerInput.isBackward() | playerInput.isLeft() | playerInput.isRight())) {
            return;
        }
        if (player.isSprinting()) {
            this.sprintTickCounter -= this.tickInterval;
            if (this.sprintTickCounter <= 0) {
                this.sprintTickCounter += this.footstepIntervalSprint;
                playFootstepSound(player, true);
            }
        } else {
            this.walkTickCounter -= this.tickInterval;
            if (this.walkTickCounter <= 0) {
                this.walkTickCounter += this.footstepIntervalWalk;
                playFootstepSound(player, false);
            }
        }
    }

    /**
     * 播放脚步声
     * @param player 玩家
     * @param isSprinting 是否在跑步
     */
    private void playFootstepSound(Player player, boolean isSprinting) {
        if (!player.isOnline()) return;
        
        Location loc = player.getLocation();
        Block blockUnder = loc.getWorld().getBlockAt(
            loc.getBlockX(),
            loc.getBlockY() - 1,
            loc.getBlockZ()
        );
        
        Material material = blockUnder.getType();
        Sound sound = getFootstepSound(material);
        float volume = isSprinting ? 0.4f : 0.3f;
        float pitch = isSprinting ? 1.1f : 1.0f;
        
        // 播放给附近的玩家（包括自己）
        double range = 16.0; // 16格范围内可以听到
        for (Player nearby : player.getWorld().getPlayers()) {
            if (nearby.getLocation().distanceSquared(loc) <= range * range) {
                nearby.playSound(loc, sound, volume, pitch);
            }
        }
    }

    /**
     * 根据方块材质获取对应的脚步声
     * @param material 方块材质
     * @return 对应的声音
     */
    private Sound getFootstepSound(Material material) {
        // 根据方块类型返回不同的脚步声
        String materialName = material.name();
        
        if (material == Material.GRASS_BLOCK || materialName.contains("GRASS") || 
            materialName.contains("FERN") || materialName.contains("MOSS")) {
            return Sound.BLOCK_GRASS_STEP;
        } else if (material == Material.SAND || material == Material.RED_SAND ||
                   material == Material.SOUL_SAND) {
            return Sound.BLOCK_SAND_STEP;
        } else if (material == Material.GRAVEL) {
            return Sound.BLOCK_GRAVEL_STEP;
        } else if (material == Material.SNOW || material == Material.SNOW_BLOCK ||
                   material == Material.POWDER_SNOW) {
            return Sound.BLOCK_SNOW_STEP;
        } else if (materialName.contains("WOOL") || materialName.contains("CARPET")) {
            return Sound.BLOCK_WOOL_STEP;
        } else if (material == Material.WATER || materialName.contains("WATER") || 
                   material == Material.SEAGRASS) {
            return Sound.BLOCK_WATER_AMBIENT;
        } else if (material == Material.LAVA || materialName.contains("LAVA")) {
            return Sound.BLOCK_LAVA_POP;
        } else if (materialName.contains("GLASS")) {
            return Sound.BLOCK_GLASS_STEP;
        } else if (material == Material.STONE || materialName.contains("STONE") ||
                   materialName.contains("ORE")) {
            return Sound.BLOCK_STONE_STEP;
        } else if (materialName.contains("WOOD") || materialName.contains("LOG") || 
                   materialName.contains("PLANKS")) {
            return Sound.BLOCK_WOOD_STEP;
        } else if (materialName.contains("IRON") || materialName.contains("GOLD") ||
                   materialName.contains("COPPER") || materialName.contains("NETHERITE")) {
            return Sound.BLOCK_METAL_STEP;
        } else {
            // 默认脚步声
            return Sound.BLOCK_GRASS_STEP;
        }
    }
}
