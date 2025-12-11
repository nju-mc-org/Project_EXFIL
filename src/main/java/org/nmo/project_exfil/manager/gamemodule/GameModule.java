package org.nmo.project_exfil.manager.gamemodule;

import org.bukkit.entity.Player;
import org.nmo.project_exfil.manager.GameInstance;

public interface GameModule {
    default void onStart(GameInstance game) {}
    default void onEnd(GameInstance game) {}
    default void onTick(GameInstance game) {}
    default void onPlayerJoin(GameInstance game, Player player) {}
    default void onPlayerQuit(GameInstance game, Player player) {}
}
