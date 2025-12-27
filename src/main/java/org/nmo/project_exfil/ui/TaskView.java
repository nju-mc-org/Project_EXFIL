package org.nmo.project_exfil.ui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nmo.project_exfil.manager.TaskManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务界面
 */
public class TaskView {
    
    private final TaskManager taskManager;
    
    public TaskView(TaskManager taskManager) {
        this.taskManager = taskManager;
    }
    
    public void open(Player player) {
        ChestGui gui = new ChestGui(6, "任务系统");
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        
        StaticPane pane = new StaticPane(0, 0, 9, 6);
        
        // 显示玩家当前任务
        List<TaskManager.PlayerTask> tasks = taskManager.getPlayerTasks(player);
        
        int slot = 0;
        for (TaskManager.PlayerTask task : tasks) {
            if (slot >= 45) break; // 最多显示45个任务
            
            ItemStack taskItem = createTaskItem(task);
            pane.addItem(new GuiItem(taskItem), slot % 9, slot / 9);
            slot++;
        }
        
        // 如果没有任务，显示提示
        if (tasks.isEmpty()) {
            ItemStack noTaskItem = new ItemStack(Material.BARRIER);
            ItemMeta meta = noTaskItem.getItemMeta();
            meta.displayName(Component.text("暂无任务", NamedTextColor.RED));
            meta.lore(List.of(Component.text("完成任务以获得奖励！", NamedTextColor.GRAY)));
            noTaskItem.setItemMeta(meta);
            pane.addItem(new GuiItem(noTaskItem), 4, 2);
        }
        
        gui.addPane(pane);
        gui.show(player);
    }
    
    private ItemStack createTaskItem(TaskManager.PlayerTask task) {
        Material material = task.completed ? Material.GREEN_CONCRETE : Material.YELLOW_CONCRETE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        String status = task.completed ? "已完成" : "进行中";
        NamedTextColor color = task.completed ? NamedTextColor.GREEN : NamedTextColor.YELLOW;
        
        meta.displayName(Component.text(task.template.name + " - " + status, color));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("进度: " + task.progress + "/" + task.template.targetAmount, NamedTextColor.GRAY));
        lore.add(Component.text("奖励: $" + String.format("%.2f", task.template.moneyReward), NamedTextColor.GOLD));
        if (!task.template.itemRewards.isEmpty()) {
            lore.add(Component.text("物品奖励: " + task.template.itemRewards.size() + " 件", NamedTextColor.AQUA));
        }
        lore.add(Component.text("类型: " + (task.template.type == TaskManager.TaskType.RAID ? "局内任务" : "局外任务"), NamedTextColor.DARK_GRAY));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
}

