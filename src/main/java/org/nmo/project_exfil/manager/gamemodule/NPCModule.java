package org.nmo.project_exfil.manager.gamemodule;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.nmo.project_exfil.ProjectEXFILPlugin;
import org.nmo.project_exfil.manager.GameInstance;
import org.nmo.project_exfil.manager.NPCType;
import org.nmo.project_exfil.region.NPCRegion;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;
import org.mcmonkey.sentinel.SentinelTrait;
import me.zombie_striker.qg.api.QualityArmory;

import java.util.ArrayList;
import java.util.List;

public class NPCModule implements GameModule {

    private final List<NPC> npcs = new ArrayList<>();

    @Override
    public void onStart(GameInstance game) {
        List<NPCRegion> regions = ProjectEXFILPlugin.getPlugin().getRegionManager().getNPCRegionsForWorld(game.getTemplateName());
        for (NPCRegion region : regions) {
            for (int i = 0; i < region.getCount(); i++) {
                Location loc = findRandomLocationInBox(game, region.getBox());
                if (loc != null) {
                    // 随机选择NPC类型，增加多样性
                    NPCType type = NPCType.randomWeighted();
                    spawnNPC(loc, type);
                }
            }
        }
    }

    @Override
    public void onEnd(GameInstance game) {
        for (NPC npc : npcs) {
            npc.destroy();
        }
        npcs.clear();
    }

    private Location findRandomLocationInBox(GameInstance game, org.bukkit.util.BoundingBox box) {
        for (int i = 0; i < 10; i++) {
            double x = box.getMinX() + Math.random() * (box.getMaxX() - box.getMinX());
            double z = box.getMinZ() + Math.random() * (box.getMaxZ() - box.getMinZ());
            int y = game.getBukkitWorld().getHighestBlockYAt((int)x, (int)z);
            
            if (y >= box.getMinY() && y <= box.getMaxY()) {
                return new Location(game.getBukkitWorld(), x, y + 1, z);
            }
        }
        return null;
    }

    /**
     * 生成NPC
     * @param loc 生成位置
     * @param type NPC类型
     */
    private void spawnNPC(Location loc, NPCType type) {
        NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, type.displayName);
        npc.spawn(loc);
        npcs.add(npc);

        // 配置Sentinel AI
        SentinelTrait sentinel = npc.getOrAddTrait(SentinelTrait.class);
        
        // 基础配置
        sentinel.addTarget("PLAYERS");
        sentinel.realistic = true;
        sentinel.setHealth(type.health);
        sentinel.respawnTime = -1; // 不重生
        
        // 范围和检测
        sentinel.range = type.range;
        sentinel.chaseRange = type.chaseRange;
        
        // 战斗行为
        sentinel.rangedChase = type.canChase;
        sentinel.closeChase = false; // 禁用近战追击，避免NPC冲脸
        sentinel.accuracy = type == NPCType.SNIPER ? 5.0 : 10.0; // 狙击手更准确
        
        // 根据类型设置特殊行为
        if (type == NPCType.GUARD) {
            // 守卫类型：更倾向于站岗，减少移动
            sentinel.rangedChase = false;
            sentinel.chaseRange = type.guardRange;
        } else if (type.canPatrol) {
            // 巡逻类型：允许追击但限制范围
            sentinel.rangedChase = type.canChase;
            sentinel.chaseRange = type.chaseRange;
        }
        
        // 性能优化：通过限制范围减少计算
        // Sentinel会自动优化，我们通过合理的范围设置来减少性能开销
        
        // 装备武器
        equipWeapon(npc, type);
    }
    
    /**
     * 装备武器
     */
    private void equipWeapon(NPC npc, NPCType type) {
        Equipment equip = npc.getOrAddTrait(Equipment.class);
        org.bukkit.inventory.ItemStack weapon = null;
        
        // 尝试从QualityArmory获取武器
        if (ProjectEXFILPlugin.getPlugin() != null) {
            try {
                for (String weaponId : type.weaponIds) {
                    weapon = QualityArmory.getCustomItemAsItemStack(weaponId);
                    if (weapon != null) break;
                }
            } catch (Exception e) {
                // QualityArmory不可用
            }
        }
        
        // 如果获取失败，使用备用武器
        if (weapon == null) {
            weapon = new org.bukkit.inventory.ItemStack(type.fallbackWeapon);
        }
        
        equip.set(Equipment.EquipmentSlot.HAND, weapon);
        
        // 根据类型添加护甲（可选）
        // 注意：Citizens的Equipment槽位可能因版本而异
        // 这里只设置主要武器，护甲可以通过其他方式添加
        // 如果需要护甲，可以考虑使用ItemsAdder的自定义护甲物品
    }
}
