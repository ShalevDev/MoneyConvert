package com.shalev.moneyconvert;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class MoneyConvert extends JavaPlugin {


    @Override
    public void onEnable() {
        // Plugin startup logic
        saveConfig();
        this.saveDefaultConfig();

        refreshDefaultConfig();
        saveConfig();
        CustomConfig.setup();

        getServer().getPluginManager().registerEvents(new MyListener(),this);
        getServer().getPluginManager().registerEvents(new CustomInventory(),this);


    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }





    private boolean isNumeric(String str){
        try{
            Integer.parseInt(str);
            return true;
        }
        catch(NumberFormatException e){
            return false;
        }
    }

    public void setupNote(ItemStack item,int amount){

        NamespacedKey key = new NamespacedKey(this,"amount");
        ItemMeta meta = item.getItemMeta();

        meta.getPersistentDataContainer().set(key,PersistentDataType.INTEGER,amount);
        item.setItemMeta(meta);
    }

    public void setCustomUUID(ItemStack item,String uuid){
        NamespacedKey key = new NamespacedKey(this,"uuid");
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(key,PersistentDataType.STRING,uuid);
        item.setItemMeta(meta);
    }

    public String getUUID(ItemStack item){
        NamespacedKey key = new NamespacedKey(this,"uuid");
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if(container.has(key,PersistentDataType.STRING))
            return container.get(key,PersistentDataType.STRING);

        return "";
    }

    public int getAmount(ItemStack item){
        NamespacedKey key = new NamespacedKey(this,"amount");
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if(container.has(key,PersistentDataType.INTEGER))
            return container.get(key,PersistentDataType.INTEGER);

        return -1;
    }


    //Create a new note
    private void createNote(Player p,int amount,int balance){

        //Create a new note
        ItemStack item = new ItemStack(Material.PAPER);
        setupNote(item,amount);

        //Update the player's balance
        getConfig().set("balance."+p.getUniqueId(),balance-amount);
        saveConfig();

        setItemDesc(item,p,amount);

        //Give the note to the player
        p.getInventory().addItem(item);
        p.sendMessage(ChatColor.RED + "Successfully withdrew "+ChatColor.DARK_RED+amount+"$"+ChatColor.RED+" from your balance!");

    }


    private void createNote(Player p,int amount){
        //Get the player's balance
        int balance = getConfig().getInt("balance."+p.getUniqueId());

        createNote(p,amount,balance);
    }

    //Set a note's description
    private void setItemDesc(ItemStack item, Player signer,int amount){
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.GREEN+"MoneyNote");
        meta.setLore(Arrays.asList(ChatColor.GRAY+"(Right Click)",ChatColor.GOLD+"Amount: "+ChatColor.DARK_GREEN+amount+"$",ChatColor.GOLD+"Signer: "+ChatColor.WHITE+signer.getDisplayName()));
        item.setItemMeta(meta);
    }

    private void setItemDesc(ItemStack item, String signer,int amount){
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.GREEN+"MoneyNote");
        meta.setLore(Arrays.asList(ChatColor.GRAY+"(Right Click)",ChatColor.GOLD+"Amount: "+ChatColor.DARK_GREEN+amount+"$",ChatColor.GOLD+"Signer: "+ChatColor.WHITE+signer));
        item.setItemMeta(meta);
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(label.equalsIgnoreCase("withdraw")){
            if (args.length==1){
                List<String> lst = new ArrayList<>();
                if(sender.hasPermission("mco.all"))
                    lst.add("all");
                if(sender.hasPermission("mco.random"))
                    lst.add("random");
                if(sender.hasPermission("mco.give"))
                    lst.add("give");
                return lst;
            }
            if(args.length == 2 && args[0].equalsIgnoreCase("give"))
                return new ArrayList<>();

            return super.onTabComplete(sender, command, label, args);
        }
        if(label.equalsIgnoreCase("serverNote") && sender.hasPermission("mco.servernote")){
            if(args.length==1)
                return Collections.singletonList("create");
            if(args.length == 2)
                return Collections.singletonList("random");
            if(args.length == 3)
                if (isNumeric(args[1]))
                    return super.onTabComplete(sender, command, label, args);
                else
                    return new ArrayList<>();
            if(args.length == 4)
                return new ArrayList<>();


            return super.onTabComplete(sender, command, label, args);
        }
        if(label.equalsIgnoreCase("bal")){
            if(args.length == 1)
            {
                List<String> lst = new ArrayList<>();
                if(sender.hasPermission("mco.balset"))
                    lst.add("set");

                lst.add("profile");

                if(sender.hasPermission("mco.bal")) {
                    for (Player p : Bukkit.getOnlinePlayers())
                        lst.add(p.getDisplayName());
                }
                return lst;
            }
            if(args.length == 2)
                return new ArrayList<>();
        }



        return super.onTabComplete(sender, command, label, args);
    }

    public int[] getPlayerLimit(Player p){
        FileConfiguration config = CustomConfig.getConfig();

        Set<String> set =  config.getKeys(false);
        Iterator<String> iterator = set.iterator();
        iterator.next();

        boolean start = true;
        boolean noPerm = true;
        int min = 0,max=0;
        while(iterator.hasNext()){
            String key = iterator.next();

            if(p.hasPermission("mco."+key))
            {
                noPerm=false;
                int configMax = config.getInt(key + ".max-note-amount");
                int configMin = config.getInt(key+".min-note-amount");
                if( max<configMax) {
                    max = configMax;
                }
                if(start || min>configMin)
                    start=false;
                    min=configMin;
            }
        }

        if(noPerm){
            min = config.getInt("default.min-note-amount");
            max = config.getInt("default.max-note-amount");
        }

        return new int[] {min,max};

    }

    public boolean notePerms(){
        return CustomConfig.getConfig().getBoolean("use-perm-based-notes");
    }

    public boolean outOfLimit(Player p,int amount){

        if(notePerms())
        {
            int[] limits = getPlayerLimit(p);
            if(amount<limits[0] || amount>limits[1])
            {
                p.sendMessage(ChatColor.RED+"Withdraw amount outside of limits\n"+"Please use a value between "+ChatColor.WHITE+limits[0]+"$"+ChatColor.RED+" and "+ChatColor.WHITE+limits[1]+"$");
                return true;
            }
        }
        return false;
    }

    private boolean errorMsg(CommandSender sender, String[] args){

        if(!args[0].equalsIgnoreCase("set")){
            sender.sendMessage(ChatColor.RED+"Wrong syntax, for help type - "+ChatColor.GRAY+"/help MoneyConvert");
            return true;
        }

        if(notHasPerm(sender,"mco.balset"))
            return true;

        if (!isNumeric(args[1])) {
            sender.sendMessage(ChatColor.RED + "Invalid balance, please use the following syntax: " + ChatColor.GRAY + "/bal set <amount> <username-optional>");
            return true;
        }
        return false;
    }

    private boolean notPlayerInstance(CommandSender sender,String msg){
        if(!(sender instanceof Player))
        {
            sender.sendMessage(msg);
            return true;
        }
        return false;
    }

    private boolean notHasPerm(CommandSender sender,String perm){
        if(sender.hasPermission(perm))
            return false;
        sender.sendMessage(ChatColor.RED+"You do not have the permission to perform this command. Please contact the server administrators if you believe that this is a mistake.");
        return true;
    }

    public void refreshDefaultConfig(){
        for(Player p : Bukkit.getOnlinePlayers()) {
            boolean unset = false;
            if(!getConfig().isSet("password")){
                getConfig().set("password","");
                unset=true;
            }

            if (!getConfig().isSet("balance." + p.getUniqueId())) {
                getConfig().set("balance." + p.getUniqueId(), 0);
                unset = true;
            }
            if (!getConfig().isSet("bal-watch." + p.getUniqueId())) {
                getConfig().set("bal-watch." + p.getUniqueId(), true);
                unset = true;
            }

            if (unset)
                saveConfig();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if(label.equalsIgnoreCase("rlConfig")){

            if (notHasPerm(sender,"mco.rlconfig"))
                return true;

            refreshDefaultConfig();
            CustomConfig.reload();
            saveConfig();
            sender.sendMessage(ChatColor.GREEN + "Successfully reloaded config!");

            if(sender instanceof Player){
                Player p = (Player) sender;
                p.playSound(p.getLocation(),"entity.experience_orb.pickup",1,2);
            }

            return true;
        }

        if(label.equalsIgnoreCase("bal")){

            //Player checking for their balance
            // /bal
            if(args.length==0)
            {
                if(notHasPerm(sender,"mco.bal"))
                    return true;

                //In case this command is being used by a console
                if(notPlayerInstance(sender,ChatColor.RED+"Only players can have a balance"))
                    return true;

                Player p = (Player) sender;
                int balance = getConfig().getInt("balance."+p.getUniqueId());
                p.sendMessage(ChatColor.GREEN+"Your balance is: "+ChatColor.DARK_GREEN+balance+"$");
                return true;
            }
            // /bal <player>
            // /bal profile
            // Check for /bal set
            if(args.length==1){
                if(args[0].equalsIgnoreCase("set")){
                    if(notHasPerm(sender,"mco.balset"))
                        return true;

                    sender.sendMessage(ChatColor.RED+"Player not specified, please use the following syntax: " + ChatColor.GRAY + "/bal set <amount> <player>");
                    return true;
                }

                if(args[0].equalsIgnoreCase("profile")){
                    if(notPlayerInstance(sender,"Only players can set their profile balance"))
                        return true;

                    Player p = (Player) sender;
                    Inventory inv = Bukkit.createInventory(new CustomInventory(), 9,"Balance Profile");

                    ItemStack item = new ItemStack(Material.PAPER);
                    setCustomUUID(item,"$bal-watch");


                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName(ChatColor.AQUA+"Allow players to see my balance");
                    boolean setting = getConfig().getBoolean("bal-watch."+p.getUniqueId());

                    ChatColor color;
                    if(setting)
                        color = ChatColor.GREEN;
                    else
                        color = ChatColor.RED;

                    String strSetting = String.valueOf(setting);
                    strSetting = strSetting.substring(0,1).toUpperCase()+strSetting.substring(1);

                    meta.setLore(Collections.singletonList(color+strSetting));
                    item.setItemMeta(meta);

                    inv.setItem(4,item);

                    p.openInventory(inv);
                    return true;
                }

                if(notHasPerm(sender,"mco.bal"))
                    return true;

                Player p = Bukkit.getPlayer(args[0]);
                if (p==null) {
                    sender.sendMessage(ChatColor.RED + "The player does not exist or they haven't joined the server, please use the following syntax: " + ChatColor.GRAY + "/bal <player>");
                    return true;
                }

                if(!sender.hasPermission("mco.bypasswatch"))
                    if(!getConfig().getBoolean("bal-watch."+p.getUniqueId()))
                    {
                        sender.sendMessage(p.getDisplayName()+ChatColor.RED+"'s balance isn't visible");
                        return true;
                    }



                int balance = getConfig().getInt("balance."+p.getUniqueId());

                sender.sendMessage(ChatColor.WHITE+p.getDisplayName()+ChatColor.GREEN+"'s balance is: "+ChatColor.DARK_GREEN+balance+"$");
                return true;
            }
            // /bal set <amount>
            if(args.length == 2){
                if(errorMsg(sender,args))
                    return true;

                if(notPlayerInstance(sender,ChatColor.RED + "Player not specified, please use the following syntax: " + ChatColor.GRAY + "/bal set <amount> <username-optional>"))
                    return true;

                Player p = (Player) sender;


                int balance = Integer.parseInt(args[1]);
                getConfig().set("balance." + p.getUniqueId(), balance);
                saveConfig();
                sender.sendMessage(ChatColor.GREEN + "Your balance has been set to: " + ChatColor.DARK_GREEN + balance+"$");
                p.playSound(p.getLocation(),"entity.experience_orb.pickup",1,2);


                return true;
            }
            // /bal set <amount> <player>
            if(errorMsg(sender,args))
                return true;

            Player p = Bukkit.getPlayer(args[2]);
            if (p==null) {
                sender.sendMessage(ChatColor.RED + "The player does not exist or they haven't joined the server, please use the following syntax: " + ChatColor.GRAY + "/bal set <amount> <username-optional>");
                return true;
            }


            int balance = Integer.parseInt(args[1]);

            getConfig().set("balance."+p.getUniqueId(),balance);
            saveConfig();

            if(sender instanceof Player)
                ((Player) sender).playSound(((Player) sender).getLocation(),"entity.experience_orb.pickup",1,2);
            sender.sendMessage(ChatColor.WHITE+p.getDisplayName()+ChatColor.GREEN+"'s balance has been set to: "+ChatColor.DARK_GREEN+balance+"$");


            return true;
        }

        if(label.equalsIgnoreCase("withdraw")){

            if(args.length==0)
            {
                if(notHasPerm(sender,"mco.withdraw"))
                    return true;
                sender.sendMessage(ChatColor.RED+"Wrong syntax, please use the following syntax: "+ChatColor.GRAY+"/withdraw <amount>");
                return true;
            }
            // /withdraw <amount>
            // /withdraw all
            // /withdraw random
            if(args.length==1){

                if(notPlayerInstance(sender,ChatColor.RED+"Only a player can withdraw money"))
                    return true;

                if(isNumeric(args[0])){

                    if(notHasPerm(sender,"mco.withdraw"))
                        return true;


                    int amount = Integer.parseInt(args[0]);
                    Player p = (Player) sender;

                    int min = 1;

                    if(notePerms())
                        min = getPlayerLimit(p)[0];

                    if(amount<1)
                    {
                        sender.sendMessage(ChatColor.RED+"Amount withdrawn has to be "+ChatColor.DARK_GREEN+min+"$"+ChatColor.RED+" or above");
                        return true;
                    }



                   if(outOfLimit(p,amount))
                       return true;

                    int balance = getConfig().getInt("balance."+p.getUniqueId());

                    if(balance-amount>=0) {
                        createNote(p, amount);
                        p.playSound(p.getLocation(),"block.stone.break",1,2);
                    }
                    else
                        sender.sendMessage(ChatColor.RED+"You do not have enough money to make this transaction");

                    return true;
                }

                if(args[0].equalsIgnoreCase("all")){

                    if(notHasPerm(sender,"mco.all"))
                        return true;

                    Player p = (Player) sender;
                    int balance = getConfig().getInt("balance."+p.getUniqueId());

                    if(outOfLimit(p,balance))
                        return true;

                    if(balance > 0) {
                        createNote(p, balance, balance);
                        p.playSound(p.getLocation(),"block.stone.break",1,2);
                    }
                    else
                        sender.sendMessage(ChatColor.RED+"You do not have enough money to make this transaction");

                    return true;
                }
                if(args[0].equalsIgnoreCase("random")){

                    if(notHasPerm(sender,"mco.random"))
                        return true;

                    Player p = (Player) sender;
                    int balance = getConfig().getInt("balance."+p.getUniqueId());
                    if(balance>0) {

                        int max = balance;

                        int min = 1;
                        if(notePerms()) {
                            int[] limits = getPlayerLimit(p);

                            if(limits[1]<balance)
                                max = limits[1];

                            min = limits[0];

                            if(balance<min){
                                p.sendMessage(ChatColor.RED+"You do not have enough money to withdraw the minimum amount: "+ChatColor.WHITE+limits[0]+"$");
                                return true;
                            }
                        }



                        int amount = randomNum(min,max);

                        p.playSound(p.getLocation(),"block.stone.break",1,2);
                        createNote(p, amount, balance);
                    }
                    else
                        sender.sendMessage(ChatColor.RED+"You do not have enough money to make this transaction");
                    return true;
                }
                if(args[0].equalsIgnoreCase("give")){
                    sender.sendMessage(ChatColor.RED+"Wrong syntax, please use the following syntax: "+ChatColor.GRAY+"/withdraw give <amount> <player>");
                    return true;
                }
                sender.sendMessage(ChatColor.RED+"Wrong syntax, for help type - /help MoneyConvert");
                return true;
            }
            if(args.length==2){
                if(notHasPerm(sender,"mco.give"))
                    return true;
                sender.sendMessage(ChatColor.RED+"Wrong syntax, For help - type /help MoneyConvert");
                return true;
            }
            // /withdraw give <amount> <player>

            if(notHasPerm(sender,"mco.give"))
                return true;

            if(notPlayerInstance(sender,ChatColor.RED+"Only a player can withdraw money"))
                return true;
            if(!args[0].equalsIgnoreCase("give") || !isNumeric(args[1])){
                if(args[0].equalsIgnoreCase("random"))
                    sender.sendMessage(ChatColor.RED+"Wrong syntax, please use the following syntax: "+ChatColor.GRAY+"/withdraw random");
                else sender.sendMessage(ChatColor.RED+"Wrong syntax, please use the following syntax: "+ChatColor.GRAY+"/withdraw give <amount> <player>");

                return true;
            }

            Player p = Bukkit.getPlayer(args[2]);
            if(p == null)
            {
                sender.sendMessage(ChatColor.RED+"Player does not exist, Please use the following syntax: "+ChatColor.GRAY+"/withdraw give <amount> <player>");
                return true;
            }


            //Get the sender's balance
            Player senderPlayer = (Player) sender;
            int senderBal = getConfig().getInt("balance."+senderPlayer.getUniqueId());

            int amount = Integer.parseInt(args[1]);

            if(outOfLimit(senderPlayer,amount))
                return true;

            if(senderBal-amount>=0) {
                ItemStack item = new ItemStack(Material.PAPER);
                setupNote(item,amount);




                //Remove the money from the sender
                getConfig().set("balance." + senderPlayer.getUniqueId(), senderBal - amount);


                saveConfig();

                setItemDesc(item, senderPlayer, Integer.parseInt(args[1]));

                p.getInventory().addItem(item);

                p.playSound(p.getLocation(),"entity.experience_orb.pickup",1,2);
                senderPlayer.playSound(senderPlayer.getLocation(),"block.stone.break",1,2);

                senderPlayer.sendMessage(ChatColor.RED + "Successfully withdrew "+ChatColor.DARK_RED+amount+"$"+ChatColor.RED+" from your balance!");
                p.sendMessage(ChatColor.WHITE+ senderPlayer.getDisplayName() + ChatColor.GREEN+" has sent you a money note worth "+ChatColor.DARK_GREEN+amount+"$");
            }
            else
                sender.sendMessage(ChatColor.RED+"You do not have enough money to make this transaction");
            return true;

        }

        if(label.equalsIgnoreCase("serverNote")){

            if(notHasPerm(sender,"mco.servernote"))
                return true;
            if (args.length<2){
                sender.sendMessage(ChatColor.RED+"Wrong syntax. For help - type /help MoneyConvert");
                return true;
            }
            //servernote create <amount>
            if(args.length == 2){
                if(!args[0].equalsIgnoreCase("create")){
                    sender.sendMessage(ChatColor.RED+"Wrong syntax. For help - type /help MoneyConvert");
                    return true;
                }
                if(notPlayerInstance(sender,ChatColor.RED+"Please specify a player with the following syntax: "+ChatColor.GRAY+"/serverNote create <amount> <username-optional>"))
                    return true;

                if(isNumeric(args[1]))
                {
                    Player p = (Player) sender;
                    int amount = Integer.parseInt(args[1]);

                    if(amount<1)
                    {
                        sender.sendMessage(ChatColor.RED+"Amount has to be "+ChatColor.DARK_GREEN+"1$"+ChatColor.RED+" or above");
                        return true;
                    }

                    ItemStack item = createCustomNote(p,amount);

                    p.getInventory().addItem(item);
                    p.playSound(p.getLocation(),"entity.experience_orb.pickup",1,2);
                    p.sendMessage(ChatColor.GREEN+"A money note has been created worth "+ChatColor.DARK_GREEN+amount+"$");
                }
                else{
                    if(args[1].equalsIgnoreCase("random")) {
                        sender.sendMessage(ChatColor.RED + "Wrong syntax, please use the following syntax: " + ChatColor.GRAY + "/serverNote create random <min-amount> <max-amount> <username-optional>");

                    }
                    else
                        sender.sendMessage(ChatColor.RED+"Wrong syntax, please use the following syntax: "+ChatColor.GRAY+"/serverNote create <amount> <username-optional>");
                }
                return true;

            }
            //servernote create <amount> <player>
            if(args.length==3){
                if(!args[0].equalsIgnoreCase("create") || !isNumeric(args[1]) )
                {
                    sender.sendMessage(ChatColor.RED+"Wrong syntax. For help - type /help MoneyConvert");
                    return true;
                }

                if(args[1].equalsIgnoreCase("random")){
                    sender.sendMessage(ChatColor.RED+"Wrong syntax, please use the following syntax: "+ChatColor.GRAY+"/serverNote create random <min-amount> <max-amount> <username-optional>");
                    return true;
                }

                Player p = Bukkit.getPlayer(args[2]);
                if(p==null){
                    sender.sendMessage(ChatColor.RED+"Player specified haven't joined the server or they do not exist, please use the following syntax: "+ChatColor.GRAY+"/serverNote create <amount> <username-optional>");
                    return true;
                }

                int amount = Integer.parseInt(args[1]);

                if(amount<1)
                {
                    sender.sendMessage(ChatColor.RED+"Amount has to be "+ChatColor.DARK_GREEN+"1$"+ChatColor.RED+" or above");
                    return true;
                }

                ItemStack item;
                if(sender instanceof Player)
                {
                    Player senderPlayer = (Player) sender;
                    item = createCustomNote(senderPlayer,amount);
                    senderPlayer.playSound(senderPlayer.getLocation(),"entity.experience_orb.pickup",1,2);
                    p.sendMessage(ChatColor.WHITE+senderPlayer.getDisplayName()+ChatColor.GREEN +" has sent you a money note worth "+ChatColor.DARK_GREEN+amount+"$");

                }
                else{
                    item = createCustomNote("Console",amount);
                    p.sendMessage(ChatColor.WHITE+"Console"+ChatColor.GREEN +" has sent you a money note worth "+ChatColor.DARK_GREEN+amount+"$");

                }


                sender.sendMessage(ChatColor.GREEN+"A money note has been sent to "+ChatColor.WHITE+p.getDisplayName()+ChatColor.GREEN+" worth "+ChatColor.DARK_GREEN+amount+"$");

                p.getInventory().addItem(item);
                p.playSound(p.getLocation(),"entity.experience_orb.pickup",1,2);

                return true;
            }
            // /serverNote create random <min-amount> <max-amount>
            if(args.length==4){
                if(noteCheck(args,sender))
                    return true;

                if(notPlayerInstance(sender,ChatColor.RED+"Please specify a player with the following syntax: "+ChatColor.GRAY+"/serverNote create <amount> <username-optional>"))
                    return true;

                Player p = (Player) sender;

                int amount = randomNum(Integer.parseInt(args[2]),Integer.parseInt(args[3]));
                ItemStack item = createCustomNote(p,amount);

                p.getInventory().addItem(item);

                p.playSound(p.getLocation(),"entity.experience_orb.pickup",1,2);
                sender.sendMessage(ChatColor.GREEN+"A money note has been created worth "+ChatColor.DARK_GREEN+amount+"$");
                return true;
            }
            ///serverNote create random <min-amount> <max-amount> <player>
            if(noteCheck(args,sender))
                return true;


            Player p = Bukkit.getPlayer(args[4]);
            if(p==null){
                sender.sendMessage(ChatColor.RED + "The player does not exist or they haven't joined the server, please use the following syntax: " + ChatColor.GRAY + "/serverNote create random <min-amount> <max-amount> <username-optional>");
                return true;
            }

            int amount = randomNum(Integer.parseInt(args[2]),Integer.parseInt(args[3]));

            ItemStack item;
            if(sender instanceof Player){
                Player playerSender = (Player) sender;
                item = createCustomNote(playerSender,amount);
                playerSender.playSound(playerSender.getLocation(),"entity.experience_orb.pickup",1,2);
                p.sendMessage(ChatColor.GREEN+"A money note has been delivered to you by "+ChatColor.WHITE+playerSender.getDisplayName() +ChatColor.GREEN+" worth "+ChatColor.DARK_GREEN+amount+"$");


            }
            else{
                item = createCustomNote("Console",amount);
                p.sendMessage(ChatColor.GREEN+"A money note has been delivered to you by "+ChatColor.WHITE+"Console" +ChatColor.GREEN+" worth "+ChatColor.DARK_GREEN+amount+"$");

            }




            p.getInventory().addItem(item);

            sender.sendMessage(ChatColor.GREEN+"A money note has been created worth "+ChatColor.DARK_GREEN+amount+"$");

            p.playSound(p.getLocation(),"entity.experience_orb.pickup",1,2);


            return true;
        }

        return true;
    }



    private boolean noteCheck(String[] args, CommandSender sender){
        if(!args[0].equalsIgnoreCase("create") || !args[1].equalsIgnoreCase("random") || !isNumeric(args[2]) || !isNumeric(args[3])){
            sender.sendMessage(ChatColor.RED+"Wrong syntax. For help - type /help MoneyConvert");
            return true;
        }

        int min = Integer.parseInt(args[2]);
        int max = Integer.parseInt(args[3]);
        if(min<1 || max<1) {
            sender.sendMessage(ChatColor.RED+"Minimum and Maximum have to be 1$ and above");
            return true;
        }
        return false;
    }

    private int randomNum(int num1, int num2){
        Random rnd = new Random();
        int randomDifference = rnd.nextInt(Math.abs(num1-num2)+1);

        int min;
        if(num1<num2)
            min=num1;
        else
            min=num2;
        return min+randomDifference;
    }
    private ItemStack createCustomNote(Player p,int amount){

        ItemStack item = new ItemStack(Material.PAPER);
        setupNote(item,amount);

        setItemDesc(item,p,amount);

        return item;
    }

    private ItemStack createCustomNote(String p,int amount){

        ItemStack item = new ItemStack(Material.PAPER);
        setupNote(item,amount);

        setItemDesc(item,p,amount);

        return item;
    }


}
