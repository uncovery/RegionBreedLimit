package us.azkedar.regionbreedlimit;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.plugin.Plugin;

/**
 *
 * @author bellaire
 */
public class Rule {
    public String rule_name;
    public boolean region;
    public boolean headcount;
    public boolean world;
    public List<String> worlds;
    public List<String> entities;
    public List<String> reasons;
    public int count;
    public String message;
    private Logger log;
    private RegionBreedLimit basePlugin;

    public Rule(String rule_name, FileConfiguration ruleconf, Logger log, RegionBreedLimit plugin) {
        this.log = log;
        this.basePlugin = plugin;
        this.rule_name  = rule_name;
        this.region     = ruleconf.getBoolean(   "rules." + rule_name + ".region");
        this.headcount  = ruleconf.getBoolean(   "rules." + rule_name + ".headcount");
        this.world      = ruleconf.getBoolean(   "rules." + rule_name + ".world");
        this.worlds     = ruleconf.getStringList("rules." + rule_name + ".worlds");
        this.entities   = ruleconf.getStringList("rules." + rule_name + ".entities");
        this.reasons    = ruleconf.getStringList("rules." + rule_name + ".reasons");
        this.count      = ruleconf.getInt(       "rules." + rule_name + ".count");
        this.message    = ruleconf.getString(    "rules." + rule_name + ".message");
    }
    
    public boolean configCheck(List<String> list, String name, String label) {
        if(!list.isEmpty() && !containsIgnoreCase(list, name)) {
            if(basePlugin.debug) {
                //log(label + " check: " + name + " not in " + basePlugin.join(list));
            }
            return false;
        }
        return true;
    }
    
    public boolean checkLocation(Location loc) {
        String worldName = loc.getWorld().getName();
        if (!configCheck(worlds,worldName,"World")) {
            return false;
        }
       
        // Check if this is a region rule where there are no regions
        
        // Get a Worlguard instance. WG 7.0 version
        WorldGuard worldGuard = WorldGuard.getInstance();
        // get a region container
        RegionContainer container = worldGuard.getPlatform().getRegionContainer();
        // create a location query
        RegionQuery query = container.createQuery();
        // adpat the bukkit location to WG location and query the region
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(loc));

        // If no region is defined, but we asked for a region, return null
        if(set.size() == 0 && region) {
            //log("Region check: No regions here");
            return false;
        }
        return true;
    }
    
    public boolean checkReason(SpawnReason reason) {
        String reasonName = reason.name();
        return configCheck(reasons,reasonName,"Spawn reason");
    }
    
    public boolean checkEntityType(Entity ent) {
        String entityTypeName = ent.getType().getName();
        return configCheck(entities,entityTypeName,"Entity type");
    }
    
    public int getCount(Location loc) {
        if (basePlugin.debug) {
            //log("Counting...");
        }
        
        Entity[] allEntities = getEntities(loc);
        if (allEntities == null) {
            return 0;
        }
        
        int count = 0;
        for(Entity thisAnimal : allEntities) {
            if(checkEntityType(thisAnimal)) {
                count++;
                if (basePlugin.debug) {
                    log("Found " + thisAnimal.getType() + " at " + terseLocation(thisAnimal.getLocation()));
                }
            }
        }
        return count;
    }
    
    public Entity[] getEntities(Location loc) {
            ArrayList<Entity> allEntities = new ArrayList<Entity>();
        
        if (world) {
            List<Entity> worldEntities = loc.getWorld().getEntities();
            return worldEntities.toArray(new Entity[worldEntities.size()]);
        }
        
        if(!region) {
            return loc.getChunk().getEntities();
        }

        // Get a Worlguard instance. WG 7.0 version
        WorldGuard worldGuard = WorldGuard.getInstance();
        // get a region container
        RegionContainer container = worldGuard.getPlatform().getRegionContainer();
        // create a location query
        RegionQuery query = container.createQuery();
        // adpat the bukkit location to WG location and query the region
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(loc));

        // If no region is defined, but we asked for a region, return null
        if(set.size() == 0) {
            return null;
        }

        for (ProtectedRegion region : set) {
            int min_x = region.getMinimumPoint().getBlockX();
            int min_z = region.getMinimumPoint().getBlockZ();
            int max_x = region.getMaximumPoint().getBlockX();
            int max_z = region.getMaximumPoint().getBlockZ();
            for(int x = min_x; x < (max_x+16); x += 16) {
                for(int z = min_z; z < (max_z+16); z+= 16) {                       
                    //getLogger().info("Finding chunk at X"+new Integer(x).toString()+" and Z"+new Integer(z).toString());
                    allEntities.addAll(Arrays.asList(loc.getWorld().getChunkAt(new Location(loc.getWorld(),x,64,z)).getEntities()));
                }
            }
        }

        return allEntities.toArray(new Entity[allEntities.size()]);
    }

    private WorldGuardPlugin getWorldGuard() {
        Plugin plugin = basePlugin.getServer().getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            return null; // Maybe you want throw an exception instead
        }

        return (WorldGuardPlugin) plugin;
    }    
    
    public boolean containsIgnoreCase(List<String> haystack, String needle) {
        for (String item : haystack) {
            if (item.equalsIgnoreCase(needle)) {
                return true;
            }
        }
        return false;
    }
    
    private String terseLocation(Location loc) {
        return "[X " + loc.getBlockX() + ", Y " + loc.getBlockY() + ", Z " + loc.getBlockZ() + "]";
    }
    
    private void log(String message) {
        log.info("[" + rule_name + "] " + message);
    }
}
