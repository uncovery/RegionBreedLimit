// Original package is com.github.ephemeralis.chunkbreedlimit
// https://github.com/Ephemeralis/ChunkBreedLimit
package us.azkedar.regionbreedlimit;


import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.vehicle.VehicleCreateEvent;

public class BreederListener implements Listener {

    private RegionBreedLimit basePlugin;
    public BreederListener(RegionBreedLimit plugin) {
        basePlugin = plugin;
    }
    
    @EventHandler
    public void onCreatureSpawnEvent(CreatureSpawnEvent event) {
        event.setCancelled(checkCancel(event.getEntity(),event.getLocation(),event.getSpawnReason()));
    }
    
    @EventHandler
    public void onVehicleCreateEvent(VehicleCreateEvent event) {
       if(checkCancel(event.getVehicle(),event.getVehicle().getLocation(),null)) {
           event.getVehicle().remove();
       }
    }
    
    public boolean checkCancel(Entity entity,Location loc,SpawnReason reason) {
        //basePlugin.getLogger().info("Spawn event detected");
        for(Rule rule : basePlugin.rules) {

            if (!rule.checkLocation(loc)) continue; 
            if (reason != null && !rule.checkReason(reason)) continue;
            if (!rule.checkEntityType(entity)) continue;
            
            int count = rule.getCount(loc);
            
            if( count >= rule.count ) {
                if(basePlugin.debug) {
                    basePlugin.getLogger().info("Preventing spawn due to rule: " + rule.rule_name + " (" + count + " found / " + rule.count + " allowed)");
                }
                if (rule.message != null && !rule.message.isEmpty()) {
                    int radius = basePlugin.messageRadius;
                    basePlugin.getLogger().info(entity.getLocation().toString());
                    for(Entity nearby : entity.getNearbyEntities(radius, radius, radius)) {
                        if((nearby instanceof Player)) {
                            ((Player) nearby).sendMessage(ChatColor.RED + rule.message);
                        }
                    }
                };
                return true;
            }
        }
        return false;
    }
}
