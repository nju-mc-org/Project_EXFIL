package org.nmo.project_exfil.manager;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.nmo.project_exfil.ProjectEXFILPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 战术动作管理器
 * 实现滑铲、侧身、快速换弹等战术动作
 */
public class TacticalActionManager implements Listener {
    
    private final ProjectEXFILPlugin plugin;
    
    // 滑铲相关
    private final Map<UUID, Long> slideCooldowns = new HashMap<>();
    private final Map<UUID, Long> slidingPlayers = new HashMap<>();
    private static final long SLIDE_COOLDOWN_MS = 3000; // 3秒冷却
    private static final long SLIDE_DURATION_MS = 800; // 滑铲持续时间800ms
    private static final double SLIDE_SPEED = 1.5; // 滑铲速度倍率
    
    // 侧身相关
    private final Map<UUID, LeanState> leaningPlayers = new HashMap<>();
    private static final double LEAN_ANGLE = 20.0; // 侧身角度
    
    // 快速换弹相关
    private final Map<UUID, Long> fastReloadCooldowns = new HashMap<>();
    private static final long FAST_RELOAD_COOLDOWN_MS = 2000; // 2秒冷却
    
    public enum LeanState {
        NONE,
        LEFT,
        RIGHT
    }
    
    public TacticalActionManager(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // 启动滑铲更新任务
        new BukkitRunnable() {
            @Override
            public void run() {
                updateSliding();
            }
        }.runTaskTimer(plugin, 0L, 1L); // 每tick更新
        
        // 启动侧身更新任务
        new BukkitRunnable() {
            @Override
            public void run() {
                updateLeaning();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    /**
     * 尝试执行滑铲
     * @param player 玩家
     * @return 是否成功执行滑铲
     */
    public boolean trySlide(Player player) {
        if (player.getGameMode() == GameMode.SPECTATOR) return false;
        if (plugin.getGameManager().getPlayerInstance(player) == null) return false;
        
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        
        // 检查冷却
        Long lastSlide = slideCooldowns.get(uuid);
        if (lastSlide != null && now - lastSlide < SLIDE_COOLDOWN_MS) {
            return false;
        }
        
        // 检查是否在移动（需要一定速度才能滑铲）
        Vector velocity = player.getVelocity();
        double speed = Math.sqrt(velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ());
        if (speed < 0.1) {
            return false; // 速度太慢，无法滑铲
        }
        
        // 检查是否在地面上
        Location loc = player.getLocation();
        Material blockBelow = loc.getWorld().getBlockAt(
            loc.getBlockX(),
            loc.getBlockY() - 1,
            loc.getBlockZ()
        ).getType();
        
        if (blockBelow == Material.AIR || !blockBelow.isSolid()) {
            return false; // 不在实心方块上
        }
        
        // 执行滑铲
        executeSlide(player);
        slideCooldowns.put(uuid, now);
        return true;
    }
    
    /**
     * 执行滑铲动作
     */
    private void executeSlide(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        
        // 设置滑铲状态
        slidingPlayers.put(uuid, now);
        
        // 获取玩家朝向
        Vector direction = player.getLocation().getDirection();
        direction.setY(0); // 水平方向
        direction.normalize();
        
        // 应用滑铲速度
        Vector slideVelocity = direction.multiply(SLIDE_SPEED);
        slideVelocity.setY(-0.2); // 稍微向下
        player.setVelocity(slideVelocity);
        
        // 添加效果
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 16, 2, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 16, 1, false, false, false)); // 降低垂直移动
        
        // 播放音效
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.5f);
        
        // 设置玩家为潜行状态（视觉上看起来像滑铲）
        player.setSneaking(true);
        
        // 延迟恢复
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && slidingPlayers.containsKey(uuid)) {
                    player.setSneaking(false);
                    slidingPlayers.remove(uuid);
                }
            }
        }.runTaskLater(plugin, SLIDE_DURATION_MS / 50); // 转换为ticks
    }
    
    /**
     * 更新滑铲状态
     */
    private void updateSliding() {
        long now = System.currentTimeMillis();
        slidingPlayers.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > SLIDE_DURATION_MS) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    player.setSneaking(false);
                }
                return true;
            }
            return false;
        });
    }
    
    /**
     * 切换侧身状态
     * @param player 玩家
     * @param direction 方向（LEFT或RIGHT）
     */
    public void toggleLean(Player player, LeanState direction) {
        if (player.getGameMode() == GameMode.SPECTATOR) return;
        if (plugin.getGameManager().getPlayerInstance(player) == null) return;
        
        UUID uuid = player.getUniqueId();
        LeanState current = leaningPlayers.getOrDefault(uuid, LeanState.NONE);
        
        if (current == direction) {
            // 取消侧身
            leaningPlayers.remove(uuid);
            resetLean(player);
        } else {
            // 设置侧身
            leaningPlayers.put(uuid, direction);
            applyLean(player, direction);
        }
    }
    
    /**
     * 应用侧身效果
     */
    private void applyLean(Player player, LeanState direction) {
        Location loc = player.getLocation();
        float yaw = loc.getYaw();
        
        if (direction == LeanState.LEFT) {
            yaw -= LEAN_ANGLE;
        } else if (direction == LeanState.RIGHT) {
            yaw += LEAN_ANGLE;
        }
        
        loc.setYaw(yaw);
        player.teleport(loc);
        
        // 播放音效
        player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_PLACE, 0.3f, 1.2f);
    }
    
    /**
     * 重置侧身
     */
    private void resetLean(Player player) {
        // 侧身已经通过teleport重置了
        player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_PLACE, 0.3f, 0.8f);
    }
    
    /**
     * 更新侧身状态（处理玩家移动时的侧身）
     */
    private void updateLeaning() {
        // 侧身状态在玩家移动时会自动保持
        // 这里可以添加额外的逻辑，比如自动取消侧身等
    }
    
    /**
     * 尝试快速换弹
     * @param player 玩家
     * @return 是否成功执行快速换弹
     */
    public boolean tryFastReload(Player player) {
        if (player.getGameMode() == GameMode.SPECTATOR) return false;
        if (plugin.getGameManager().getPlayerInstance(player) == null) return false;
        
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        
        // 检查冷却
        Long lastReload = fastReloadCooldowns.get(uuid);
        if (lastReload != null && now - lastReload < FAST_RELOAD_COOLDOWN_MS) {
            return false;
        }
        
        // 检查是否持有武器（简化版，实际应该检查QualityArmory武器）
        if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
            return false;
        }
        
        // 执行快速换弹
        executeFastReload(player);
        fastReloadCooldowns.put(uuid, now);
        return true;
    }
    
    /**
     * 执行快速换弹
     */
    private void executeFastReload(Player player) {
        // 播放快速换弹音效
        player.playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_END, 1.0f, 1.5f);
        
        // 添加视觉效果（可以改进）
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20, 0, false, false, false));
        
        // 发送消息
        plugin.getLanguageManager().send(player, "exfil.tactical.fast_reload");
    }
    
    /**
     * 获取玩家的侧身状态
     */
    public LeanState getLeanState(Player player) {
        return leaningPlayers.getOrDefault(player.getUniqueId(), LeanState.NONE);
    }
    
    /**
     * 检查玩家是否在滑铲
     */
    public boolean isSliding(Player player) {
        return slidingPlayers.containsKey(player.getUniqueId());
    }
    
    /**
     * 清理玩家数据
     */
    public void cleanup(Player player) {
        UUID uuid = player.getUniqueId();
        slideCooldowns.remove(uuid);
        slidingPlayers.remove(uuid);
        leaningPlayers.remove(uuid);
        fastReloadCooldowns.remove(uuid);
    }
    
    // 事件处理器
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // 如果玩家在滑铲，保持滑铲效果
        if (slidingPlayers.containsKey(uuid)) {
            // 可以在这里添加额外的滑铲逻辑
        }
        
        // 如果玩家在侧身，保持侧身角度
        LeanState leanState = leaningPlayers.get(uuid);
        if (leanState != null && leanState != LeanState.NONE) {
            // 侧身角度在移动时可能需要调整
            // 这里可以添加更复杂的逻辑
        }
    }
    
    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        
        // 如果玩家在滑铲中，取消潜行会结束滑铲
        if (!event.isSneaking() && isSliding(player)) {
            slidingPlayers.remove(player.getUniqueId());
            player.setSneaking(false);
        } else if (event.isSneaking() && !isSliding(player)) {
            // 尝试触发滑铲：在移动时按潜行键
            if (player.isSprinting() || player.getVelocity().lengthSquared() > 0.1) {
                // 延迟检查，避免与普通潜行冲突
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isSneaking() && !isSliding(player)) {
                            trySlide(player);
                        }
                    }
                }.runTaskLater(plugin, 2L);
            }
        }
    }
}

