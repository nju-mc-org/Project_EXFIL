package org.nmo.project_exfil.manager;

import org.bukkit.Material;

/**
 * NPC类型枚举 - 定义不同类型的NPC
 */
public enum NPCType {
    SCAVENGER("清道夫", 20, 15.0, 25.0, 8.0, 15.0, true, false, 
              new String[]{"ak47", "m4a1s"}, Material.IRON_SWORD),
    
    PATROL("巡逻兵", 25, 20.0, 30.0, 10.0, 20.0, true, true,
           new String[]{"ak47", "m4a1s"}, Material.IRON_SWORD),
    
    GUARD("守卫", 30, 25.0, 35.0, 5.0, 15.0, false, false,
          new String[]{"m4a1s", "ak47"}, Material.IRON_SWORD),
    
    SNIPER("狙击手", 15, 40.0, 50.0, 20.0, 30.0, true, false,
           new String[]{"awp", "sniper"}, Material.BOW),
    
    RUSHER("突击兵", 25, 10.0, 15.0, 5.0, 10.0, true, true,
           new String[]{"ak47", "m4a1s"}, Material.IRON_SWORD);

    public final String displayName;
    public final int health;
    public final double range;          // 检测范围
    public final double chaseRange;     // 追击范围
    public final double patrolRange;    // 巡逻范围
    public final double guardRange;     // 守卫范围
    public final boolean canPatrol;     // 是否巡逻
    public final boolean canChase;      // 是否追击
    public final String[] weaponIds;    // 武器ID列表
    public final Material fallbackWeapon; // 备用武器

    NPCType(String displayName, int health, double range, double chaseRange, 
            double patrolRange, double guardRange, boolean canPatrol, boolean canChase,
            String[] weaponIds, Material fallbackWeapon) {
        this.displayName = displayName;
        this.health = health;
        this.range = range;
        this.chaseRange = chaseRange;
        this.patrolRange = patrolRange;
        this.guardRange = guardRange;
        this.canPatrol = canPatrol;
        this.canChase = canChase;
        this.weaponIds = weaponIds;
        this.fallbackWeapon = fallbackWeapon;
    }
    
    /**
     * 随机获取一个NPC类型
     */
    public static NPCType random() {
        NPCType[] types = values();
        return types[(int) (Math.random() * types.length)];
    }
    
    /**
     * 根据权重随机获取（某些类型更常见）
     */
    public static NPCType randomWeighted() {
        double rand = Math.random();
        if (rand < 0.4) return SCAVENGER;      // 40% 清道夫
        if (rand < 0.7) return PATROL;        // 30% 巡逻兵
        if (rand < 0.85) return GUARD;       // 15% 守卫
        if (rand < 0.95) return SNIPER;      // 10% 狙击手
        return RUSHER;                        // 5% 突击兵
    }
}

