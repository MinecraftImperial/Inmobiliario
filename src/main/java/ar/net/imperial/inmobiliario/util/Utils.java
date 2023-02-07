package ar.net.imperial.inmobiliario.util;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dev.espi.protectionstones.PSRegion;
import org.bukkit.Bukkit;

import java.util.Set;
import java.util.UUID;

public class Utils {

    /**
     * @param uuid UUID of the player
     * @return Last login of the player in days
     */
    public static long lastLogin(UUID uuid) {
        return (System.currentTimeMillis() - Bukkit.getOfflinePlayer(uuid).getLastSeen()) / 86400000L;
    }

    public static void getConnectedRegions(PSRegion psRegion, Set<PSRegion> connectedRegions, RegionManager regionManager) {
        connectedRegions.add(psRegion);
        ProtectedRegion wgRegion = psRegion.getWGRegion();
        ApplicableRegionSet applicableRegionSet = regionManager.getApplicableRegions(wgRegion);
        for(ProtectedRegion protectedRegion : applicableRegionSet) {
            PSRegion connectedRegion = PSRegion.fromWGRegion(psRegion.getWorld(), protectedRegion);
            if(connectedRegion!=null && !connectedRegions.contains(connectedRegion)) {
                getConnectedRegions(connectedRegion, connectedRegions, regionManager);
            }
        }
    }



}
