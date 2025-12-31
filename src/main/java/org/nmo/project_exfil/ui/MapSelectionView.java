package org.nmo.project_exfil.ui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nmo.project_exfil.manager.GameManager;
import org.nmo.project_exfil.manager.MapManager;
import org.nmo.project_exfil.ui.framework.UIHelper;

import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.nmo.project_exfil.ProjectEXFILPlugin;

public class MapSelectionView {

    private final GameManager gameManager;
    private final ProjectEXFILPlugin plugin = ProjectEXFILPlugin.getPlugin();

    public MapSelectionView(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void open(Player player) {
        ChestGui gui = new ChestGui(3, "Select Operation Map");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        StaticPane pane = new StaticPane(0, 0, 9, 3);

        List<MapManager.GameMap> maps = plugin.getMapManager().getMaps();
        int x = 1;
        int y = 1;

        for (MapManager.GameMap map : maps) {
            ItemStack item = new ItemStack(Material.MAP);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(map.getDisplayName()).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                plugin.getLanguageManager().getMessage("exfil.map.click_deploy")
            ));
            item.setItemMeta(meta);

            pane.addItem(new GuiItem(item, event -> {
                UIHelper.playClickSound(player);
                plugin.getLanguageManager().send(player, "exfil.map.matchmaking", Placeholder.unparsed("map", map.getDisplayName()));
                gui.getInventory().close();
                gameManager.joinQueue(player, map.getTemplateName());
            }), x, y);
            
            x++;
            if (x > 7) {
                x = 1;
                y++;
            }
            if (y > 2) break;
        }

        // Back Button
        pane.addItem(UIHelper.createBackButton(), 0, 2);

        gui.addPane(pane);
        gui.show(player);
    }
}
