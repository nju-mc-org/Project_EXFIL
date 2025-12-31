package org.nmo.project_exfil.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.nmo.project_exfil.ProjectEXFILPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 小队交流管理器（大荒囚天指）
 * 提供小队内的快速交流和位置标记功能
 */
public class TeamCommunicationManager implements Listener {
    
    private final ProjectEXFILPlugin plugin;
    private final PartyManager partyManager;
    
    // 位置标记相关
    private final Map<UUID, Location> markedLocations = new HashMap<>();
    private static final long MARK_COOLDOWN_MS = 5000; // 5秒冷却
    private final Map<UUID, Long> markCooldowns = new HashMap<>();
    
    // 快捷消息
    public enum QuickMessage {
        ENEMY_SPOTTED("发现敌人", NamedTextColor.RED),
        NEED_BACKUP("需要支援", NamedTextColor.YELLOW),
        MOVING("正在移动", NamedTextColor.AQUA),
        HOLDING("坚守位置", NamedTextColor.GREEN),
        EXTRACTING("正在撤离", NamedTextColor.GOLD),
        LOOT_HERE("这里有战利品", NamedTextColor.LIGHT_PURPLE);
        
        private final String message;
        private final NamedTextColor color;
        
        QuickMessage(String message, NamedTextColor color) {
            this.message = message;
            this.color = color;
        }
        
        public String getMessage() {
            return message;
        }
        
        public NamedTextColor getColor() {
            return color;
        }
    }
    
    public TeamCommunicationManager(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
        this.partyManager = plugin.getPartyManager();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * 标记当前位置
     * @param player 玩家
     * @return 是否成功标记
     */
    public boolean markLocation(Player player) {
        if (!partyManager.isEnabled()) return false;
        
        com.alessiodp.parties.api.interfaces.PartyPlayer partyPlayer = partyManager.getPartyPlayer(player.getUniqueId());
        if (partyPlayer == null || !partyPlayer.isInParty()) {
            plugin.getLanguageManager().send(player, "exfil.team.not_in_party");
            return false;
        }
        
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        
        // 检查冷却
        Long lastMark = markCooldowns.get(uuid);
        if (lastMark != null && now - lastMark < MARK_COOLDOWN_MS) {
            long remaining = (MARK_COOLDOWN_MS - (now - lastMark)) / 1000;
            plugin.getLanguageManager().send(player, "exfil.team.mark_cooldown", 
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("time", String.valueOf(remaining)));
            return false;
        }
        
        // 标记位置
        Location loc = player.getLocation();
        markedLocations.put(uuid, loc);
        markCooldowns.put(uuid, now);
        
        // 通知小队成员
        com.alessiodp.parties.api.interfaces.Party party = partyManager.getParty(partyPlayer.getPartyId());
        if (party != null) {
            String locationStr = String.format("%.0f, %.0f, %.0f", loc.getX(), loc.getY(), loc.getZ());
            Component message = Component.text()
                .append(Component.text("[小队] ", NamedTextColor.AQUA))
                .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" 标记了位置: ", NamedTextColor.GRAY))
                .append(Component.text(locationStr, NamedTextColor.GREEN))
                .build();
            
            for (UUID memberId : party.getMembers()) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline()) {
                    member.sendMessage(message);
                    member.playSound(member.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
                }
            }
        }
        
        plugin.getLanguageManager().send(player, "exfil.team.location_marked");
        return true;
    }
    
    /**
     * 发送快捷消息
     * @param player 玩家
     * @param message 快捷消息类型
     * @return 是否成功发送
     */
    public boolean sendQuickMessage(Player player, QuickMessage message) {
        if (!partyManager.isEnabled()) return false;
        
        com.alessiodp.parties.api.interfaces.PartyPlayer partyPlayer = partyManager.getPartyPlayer(player.getUniqueId());
        if (partyPlayer == null || !partyPlayer.isInParty()) {
            plugin.getLanguageManager().send(player, "exfil.team.not_in_party");
            return false;
        }
        
        com.alessiodp.parties.api.interfaces.Party party = partyManager.getParty(partyPlayer.getPartyId());
        if (party == null) return false;
        
        // 构建消息
        Component chatMessage = Component.text()
            .append(Component.text("[小队] ", NamedTextColor.AQUA))
            .append(Component.text(player.getName(), NamedTextColor.YELLOW))
            .append(Component.text(": ", NamedTextColor.GRAY))
            .append(Component.text(message.getMessage(), message.getColor()))
            .build();
        
        // 发送给所有小队成员
        for (UUID memberId : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage(chatMessage);
                member.playSound(member.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 1.2f);
            }
        }
        
        return true;
    }
    
    /**
     * 获取玩家标记的位置
     */
    public Location getMarkedLocation(Player player) {
        return markedLocations.get(player.getUniqueId());
    }
    
    /**
     * 清理玩家数据
     */
    public void cleanup(Player player) {
        UUID uuid = player.getUniqueId();
        markedLocations.remove(uuid);
        markCooldowns.remove(uuid);
    }
    
    /**
     * 拦截小队聊天，添加特殊格式
     * 注意：AsyncPlayerChatEvent在较新版本中已deprecated，但为了兼容性仍使用
     */
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH)
    @SuppressWarnings("deprecation")
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!partyManager.isEnabled()) return;
        
        Player player = event.getPlayer();
        com.alessiodp.parties.api.interfaces.PartyPlayer partyPlayer = partyManager.getPartyPlayer(player.getUniqueId());
        
        // 如果玩家在小队中，且消息以特定前缀开头，则只发送给小队
        if (partyPlayer != null && partyPlayer.isInParty()) {
            String message = event.getMessage();
            
            // 检查是否是小队聊天（以@开头或/tc开头）
            if (message.startsWith("@") || message.startsWith("/tc")) {
                event.setCancelled(true);
                
                // 移除前缀
                String actualMessage = message;
                if (message.startsWith("@")) {
                    actualMessage = message.substring(1).trim();
                } else if (message.startsWith("/tc")) {
                    actualMessage = message.substring(3).trim();
                }
                
                // 发送给小队
                com.alessiodp.parties.api.interfaces.Party party = partyManager.getParty(partyPlayer.getPartyId());
                if (party != null) {
                    Component chatMessage = Component.text()
                        .append(Component.text("[小队] ", NamedTextColor.AQUA))
                        .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                        .append(Component.text(": ", NamedTextColor.GRAY))
                        .append(Component.text(actualMessage, NamedTextColor.WHITE))
                        .build();
                    
                    for (UUID memberId : party.getMembers()) {
                        Player member = Bukkit.getPlayer(memberId);
                        if (member != null && member.isOnline()) {
                            member.sendMessage(chatMessage);
                        }
                    }
                }
            }
        }
    }
}

