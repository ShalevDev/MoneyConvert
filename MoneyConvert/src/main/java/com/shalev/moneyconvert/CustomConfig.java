package com.shalev.moneyconvert;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class CustomConfig {

    private static File file;
    private static FileConfiguration customFile;

    //Finds or generates the custom config file
    public static void setup(){
        file = new File(Bukkit.getServer().getPluginManager().getPlugin("MoneyConvert").getDataFolder(),"configPerms.yml");
        if(!file.exists()){
            try {
                file.createNewFile();
            }catch (IOException e) {
                e.printStackTrace();
            }
        }

        customFile = YamlConfiguration.loadConfiguration(file);

        if(!customFile.isSet("use-perm-based-notes"))
            customFile.set("use-perm-based-notes",false);
        if(!customFile.isSet("default")) {
            customFile.set("default.max-note-amount", 1000);
            customFile.set("default.min-note-amount", 50);
        }

        save();
    }

    public static FileConfiguration getConfig(){
        return customFile;
    }

    public static void save(){
        try{
            customFile.save(file);
        }catch(IOException e){
            System.out.println("Couldn't save custom config file name configPerms");
        }
    }

    public static void reload(){
        customFile = YamlConfiguration.loadConfiguration(file);
    }

}
