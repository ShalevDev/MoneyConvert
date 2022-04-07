package com.shalev.moneyconvert;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;

import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class MyListener implements Listener {

    MoneyConvert main = MoneyConvert.getPlugin(MoneyConvert.class);


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        main.refreshDefaultConfig();
    }


    @EventHandler
    public void onPlayerUse(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if (p.getInventory().getItemInMainHand().getType() == Material.PAPER && event.getHand() == EquipmentSlot.HAND && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            ItemStack item = p.getInventory().getItemInMainHand();
            int amount = main.getAmount(item);
            if(amount!=-1) {
                int balance = main.getConfig().getInt("balance." + p.getUniqueId());
                main.getConfig().set("balance." + p.getUniqueId(), balance + amount);
                main.saveConfig();
                event.setCancelled(true);
                item.setAmount(item.getAmount()-1);


                p.sendMessage(ChatColor.GREEN + "Successfully deposited " + ChatColor.DARK_GREEN + amount + "$" + ChatColor.GREEN + " to your balance!");
                p.playSound(p.getLocation(),"entity.experience_orb.pickup",1,2);


            }
        }
    }
}
