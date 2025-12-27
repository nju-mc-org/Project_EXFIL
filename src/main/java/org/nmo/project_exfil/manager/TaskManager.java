package org.nmo.project_exfil.manager;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.nmo.project_exfil.ProjectEXFILPlugin;

import java.util.*;

/**
 * 任务管理器 - 处理局内和局外任务
 */
public class TaskManager {
    
    private final ProjectEXFILPlugin plugin;
    // 玩家任务列表 (玩家UUID -> 任务列表)
    private final Map<UUID, List<PlayerTask>> playerTasks = new HashMap<>();
    // 所有可用任务
    private final List<TaskTemplate> taskTemplates = new ArrayList<>();
    
    public TaskManager(ProjectEXFILPlugin plugin) {
        this.plugin = plugin;
        initializeDefaultTasks();
    }
    
    /**
     * 初始化默认任务
     */
    private void initializeDefaultTasks() {
        // 局外任务示例
        taskTemplates.add(new TaskTemplate("kill_scavs", "击杀清道夫", 
            TaskType.RAID, TaskTarget.KILL_NPC, 5, 1000.0, 
            Arrays.asList(new ItemStack(org.bukkit.Material.IRON_INGOT, 10))));
        
        taskTemplates.add(new TaskTemplate("extract_items", "收集物品", 
            TaskType.RAID, TaskTarget.COLLECT_ITEMS, 10, 500.0, 
            Arrays.asList(new ItemStack(org.bukkit.Material.GOLD_INGOT, 5))));
        
        taskTemplates.add(new TaskTemplate("survive_raid", "完成一次撤离", 
            TaskType.RAID, TaskTarget.EXTRACT, 1, 2000.0, 
            Arrays.asList(new ItemStack(org.bukkit.Material.DIAMOND, 1))));
        
        // 局内任务示例
        taskTemplates.add(new TaskTemplate("kill_players", "击杀玩家", 
            TaskType.LOBBY, TaskTarget.KILL_PLAYER, 3, 1500.0, 
            Arrays.asList(new ItemStack(org.bukkit.Material.EMERALD, 3))));
    }
    
    /**
     * 给玩家分配任务
     * @param player 玩家
     * @param taskId 任务ID
     * @return 是否成功
     */
    public boolean assignTask(Player player, String taskId) {
        TaskTemplate template = getTaskTemplate(taskId);
        if (template == null) return false;
        
        PlayerTask task = new PlayerTask(template, player.getUniqueId());
        playerTasks.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(task);
        
        plugin.getLanguageManager().send(player, "exfil.task.assigned", 
            net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("name", template.name));
        
        return true;
    }
    
    /**
     * 完成任务
     * @param player 玩家
     * @param taskId 任务ID
     */
    public void completeTask(Player player, String taskId) {
        List<PlayerTask> tasks = playerTasks.get(player.getUniqueId());
        if (tasks == null) return;
        
        PlayerTask task = tasks.stream()
            .filter(t -> t.template.id.equals(taskId) && !t.completed)
            .findFirst()
            .orElse(null);
        
        if (task == null) return;
        
        if (task.progress >= task.template.targetAmount) {
            task.completed = true;
            giveRewards(player, task.template);
            plugin.getLanguageManager().send(player, "exfil.task.completed", 
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("name", task.template.name));
        }
    }
    
    /**
     * 更新任务进度
     * @param player 玩家
     * @param target 任务目标类型
     * @param amount 增加的数量
     */
    public void updateProgress(Player player, TaskTarget target, int amount) {
        List<PlayerTask> tasks = playerTasks.get(player.getUniqueId());
        if (tasks == null) return;
        
        for (PlayerTask task : tasks) {
            if (!task.completed && task.template.target == target) {
                task.progress += amount;
                if (task.progress >= task.template.targetAmount) {
                    completeTask(player, task.template.id);
                }
            }
        }
    }
    
    /**
     * 给予任务奖励
     */
    private void giveRewards(Player player, TaskTemplate template) {
        // 给予货币
        if (template.moneyReward > 0) {
            org.nmo.project_exfil.util.DependencyHelper.depositMoney(player, template.moneyReward);
        }
        
        // 给予物品
        for (ItemStack item : template.itemRewards) {
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(item);
            } else {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }
    }
    
    /**
     * 获取玩家的任务列表
     */
    public List<PlayerTask> getPlayerTasks(Player player) {
        return playerTasks.getOrDefault(player.getUniqueId(), new ArrayList<>());
    }
    
    /**
     * 获取所有可用任务模板
     */
    public List<TaskTemplate> getAvailableTasks() {
        return new ArrayList<>(taskTemplates);
    }
    
    /**
     * 获取任务模板
     */
    public TaskTemplate getTaskTemplate(String id) {
        return taskTemplates.stream()
            .filter(t -> t.id.equals(id))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 任务模板
     */
    public static class TaskTemplate {
        public final String id;
        public final String name;
        public final TaskType type;
        public final TaskTarget target;
        public final int targetAmount;
        public final double moneyReward;
        public final List<ItemStack> itemRewards;
        
        public TaskTemplate(String id, String name, TaskType type, TaskTarget target, 
                           int targetAmount, double moneyReward, List<ItemStack> itemRewards) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.target = target;
            this.targetAmount = targetAmount;
            this.moneyReward = moneyReward;
            this.itemRewards = itemRewards;
        }
    }
    
    /**
     * 玩家任务实例
     */
    public static class PlayerTask {
        public final TaskTemplate template;
        public final UUID playerId;
        public int progress;
        public boolean completed;
        public long assignedTime;
        
        public PlayerTask(TaskTemplate template, UUID playerId) {
            this.template = template;
            this.playerId = playerId;
            this.progress = 0;
            this.completed = false;
            this.assignedTime = System.currentTimeMillis();
        }
    }
    
    /**
     * 任务类型
     */
    public enum TaskType {
        RAID,    // 局内任务
        LOBBY   // 局外任务
    }
    
    /**
     * 任务目标
     */
    public enum TaskTarget {
        KILL_PLAYER,    // 击杀玩家
        KILL_NPC,       // 击杀NPC
        COLLECT_ITEMS,  // 收集物品
        EXTRACT,        // 撤离
        REACH_LOCATION  // 到达地点
    }
}

