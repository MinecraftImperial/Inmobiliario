package ar.net.imperial.inmobiliario.controller;

import dev.espi.protectionstones.PSRegion;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PlaceholderManager extends PlaceholderExpansion {
    @Override
    public @NotNull String getIdentifier() {
        return "inmobiliario";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Chasis Torcido";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    public String onRequest(OfflinePlayer player, String params) {
        if (params.equalsIgnoreCase("placeowner")) {
            OfflinePlayer placeOwner = getPlaceOwnerFromStandingPlayer(player);
            if (placeOwner == null) return null;
            return placeOwner.getName();

        }

        if (params.equalsIgnoreCase("lasttime")) {
            OfflinePlayer placeOwner = getPlaceOwnerFromStandingPlayer(player);
            if (placeOwner == null) return String.valueOf(-1);
            return String.valueOf(lastTimeInDays(placeOwner));
        }

        return null; // Placeholder is unknown by the Expansion
    }

    private static long lastTimeInDays(OfflinePlayer player) {
        return (System.currentTimeMillis() - player.getLastSeen()) / 86400000L + 1;
    }

    /**
     * @param player the player
     * @return the owner of the place where the player is standing
     */
    @Nullable
    private static OfflinePlayer getPlaceOwnerFromStandingPlayer(OfflinePlayer player) {
        Location location = player.getLocation();
        if (location == null)
            return null;

        PSRegion region = PSRegion.fromLocation(location);
        if (region == null)
            return null;

        UUID uuid = region.getOwners().get(0);
        return Bukkit.getOfflinePlayer(uuid);
    }


}