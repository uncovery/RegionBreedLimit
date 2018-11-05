// Original package is com.github.ephemeralis.chunkbreedlimit
// https://github.com/Ephemeralis/ChunkBreedLimit

package us.azkedar.regionbreedlimit;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class RegionBreedLimit extends JavaPlugin {
	
	public int entitySpawnCap;
	public boolean debug;
        public int messageRadius;
        public List<Rule> rules;
        
	@Override
	public void onEnable()
	{
            this.saveDefaultConfig();
            loadConfig();
	}
        
        public void loadConfig() {
            this.debug = this.getConfig().getBoolean("debug");
            this.messageRadius = this.getConfig().getInt("message_radius",10);
            this.rules = new ArrayList<Rule>();
            ConfigurationSection ruleConfig = this.getConfig().getConfigurationSection("rules");
            for ( String rule_name : ruleConfig.getKeys(false) ) {
                this.getLogger().info("Loading rule: " + rule_name);
                try {
                    rules.add(new Rule(rule_name,this.getConfig(),this.getLogger(),this));
                } catch( Exception e ) {
                    this.getLogger().info("Error! Could not load configuration for: " + rule_name);
                }
            }		
            this.getServer().getPluginManager().registerEvents(new BreederListener(this),this);
        }
	
	@Override
	public void onDisable()
	{
		CreatureSpawnEvent.getHandlerList().unregister(this);
		getLogger().info("ChunkBreedLimit unloaded!");
	}
        
        public Hashtable<EntityType,Integer> getWorldData(Hashtable<String,Hashtable<EntityType,Integer>> individCapData,Location loc) {
            String worldName = loc.getWorld().getName();
            if (individCapData.containsKey(worldName)) {
                return individCapData.get(worldName);
            } else {
                return individCapData.get("default");
            }
        }
        
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
            if(cmd.getName().equalsIgnoreCase("regionbreedlimit") && args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("regionbreedlimit.reload") || sender instanceof ConsoleCommandSender) {
                    this.reloadConfig();
                    this.loadConfig();
                    sender.sendMessage("Reloaded configuration for RegionBreedLimit");
                }
                return true;
            }
            if(cmd.getName().equalsIgnoreCase("regionbreedlimit") && args.length > 0 && args[0].equalsIgnoreCase("debug")) {
                if (sender.hasPermission("regionbreedlimit.debug") || sender instanceof ConsoleCommandSender) {
                    this.debug = !this.debug;
                    sender.sendMessage("Toggled debugging for RegionBreedLimit, now " + this.debug);
                }
                return true;
            }
            if(cmd.getName().equalsIgnoreCase("headcount")){ 
                if (!(sender instanceof Player)) {
		    sender.sendMessage("This command doesn't make sense from the console.");
		} else {
                    Player player = (Player) sender;
                    boolean foundRule = false;
                    for ( Rule rule : rules ) {
                        if (!rule.headcount) continue; 
                        if (!rule.checkLocation(player.getLocation())) continue; 
                        
                        int count = rule.getCount(player.getLocation());


                        // Find the color coding for the amount found
                        int maxAllowed = rule.count;
                        ChatColor levelColor = ChatColor.GREEN;
                        if(count >= maxAllowed) {
                            levelColor = ChatColor.RED;
                        } else if (count >= (maxAllowed * 0.8)) {
                            levelColor = ChatColor.YELLOW;
                        }
                        foundRule = true;
                        
                        String rule_pretty_name = rule.rule_name;
                        rule_pretty_name = rule_pretty_name.replace("_"," ");
                        rule_pretty_name = rule_pretty_name.substring(0,1).toUpperCase() + rule_pretty_name.substring(1);                       
                        
                        player.sendMessage(
                                ChatColor.AQUA + "[" + rule_pretty_name + "] " 
                                + ChatColor.WHITE + join(rule.entities) + ChatColor.AQUA + " : " 
                                + levelColor + count + " / " + maxAllowed + ChatColor.WHITE
                        );
                    }
                                   
                    if(!foundRule) {
                        player.sendMessage(ChatColor.RED + "No spawning rules were found at your current location.");
                    }
                    return true;
                }           
            } 
            return false; 
        }
        

        
        public String join(List<String> myList) {
            String returnStr = "";
            for (String s : myList) {
                if (returnStr == "") {
                    returnStr = s;
                } else {
                    returnStr += ", " + s; 
                }
            }
            return returnStr;
        }
}
