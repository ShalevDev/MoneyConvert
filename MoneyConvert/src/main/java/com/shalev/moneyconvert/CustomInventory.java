package com.shalev.moneyconvert;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.Objects;

public class CustomInventory implements InventoryHolder, Listener {

    MoneyConvert main = MoneyConvert.getPlugin(MoneyConvert.class);


    @Override
    public Inventory getInventory() {
        return null;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){

        if(event.getClickedInventory()==null)
            return;

        InventoryHolder invHolder = event.getClickedInventory().getHolder();
        ItemStack item = event.getCurrentItem();
        if( invHolder instanceof CustomInventory && item!=null && Objects.equals(main.getUUID(item),"$bal-watch") && event.getWhoClicked() instanceof Player) {
            Player p = (Player) event.getWhoClicked();
            event.setCancelled(true);

            boolean setting = main.getConfig().getBoolean("bal-watch."+p.getUniqueId());
            main.getConfig().set("bal-watch."+p.getUniqueId(),!setting);
            main.saveConfig();

            setting = !setting;

            ChatColor color;
            if(setting)
                color = ChatColor.GREEN;
            else
                color = ChatColor.RED;

            String strSetting = String.valueOf(setting);
            strSetting = strSetting.substring(0,1).toUpperCase()+strSetting.substring(1);

            ItemMeta meta = item.getItemMeta();
            meta.setLore(Collections.singletonList(color+strSetting));
            item.setItemMeta(meta);
        }

    }
}
