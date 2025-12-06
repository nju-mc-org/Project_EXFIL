package org.nmo.project_exfil.Footsteps;

import org.bukkit.Input;
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
                // TODO: Play sound
            }
        } else {
            this.walkTickCounter -= this.tickInterval;
            if (this.walkTickCounter <= 0) {
                this.walkTickCounter += this.footstepIntervalWalk;
                // TODO: Play sound
            }
        }
    }
}
