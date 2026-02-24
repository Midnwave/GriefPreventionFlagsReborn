package com.blockforge.griefpreventionflagsreborn.hooks;

import com.blockforge.griefpreventionflagsreborn.api.FlagScope;
import com.blockforge.griefpreventionflagsreborn.storage.FlagStorageManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * PlaceholderAPI expansion for GriefPreventionFlagsReborn.
 * <p>
 * Registers the following placeholders under the {@code gpfr} identifier:
 * <ul>
 *   <li>{@code %gpfr_flag_<flagname>%} - Returns the value of the specified flag at the player's current location</li>
 *   <li>{@code %gpfr_flags_count%} - Returns the total number of flags set in the player's current claim</li>
 *   <li>{@code %gpfr_in_claim%} - Returns "true" or "false" depending on whether the player is in a claim</li>
 * </ul>
 */
public final class PlaceholderAPIHook extends PlaceholderExpansion {

    private final String pluginVersion;
    private final GriefPreventionHook gpHook;
    private final FlagStorageManager flagStorage;

    /**
     * Creates a new PlaceholderAPIHook.
     *
     * @param pluginVersion the plugin version string for PlaceholderAPI metadata
     * @param gpHook        the GriefPrevention hook for claim lookups
     * @param flagStorage   the flag storage manager for value lookups
     */
    public PlaceholderAPIHook(@NotNull String pluginVersion,
                              @NotNull GriefPreventionHook gpHook,
                              @NotNull FlagStorageManager flagStorage) {
        this.pluginVersion = pluginVersion;
        this.gpHook = gpHook;
        this.flagStorage = flagStorage;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "gpfr";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "BlockForge";
    }

    @Override
    @NotNull
    public String getVersion() {
        return pluginVersion;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    @Nullable
    public String onPlaceholderRequest(@Nullable Player player, @NotNull String params) {
        if (player == null) {
            return null;
        }

        // %gpfr_in_claim%
        if (params.equalsIgnoreCase("in_claim")) {
            return String.valueOf(gpHook.isInClaim(player.getLocation()));
        }

        // %gpfr_flags_count%
        if (params.equalsIgnoreCase("flags_count")) {
            return getFlagsCount(player.getLocation());
        }

        // %gpfr_flag_<flagname>%
        if (params.toLowerCase().startsWith("flag_")) {
            String flagName = params.substring(5); // Remove "flag_" prefix
            if (flagName.isEmpty()) {
                return null;
            }
            return getFlagValue(flagName, player.getLocation());
        }

        return null;
    }

    @Override
    @Nullable
    public String onRequest(@Nullable OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) {
            return null;
        }
        Player player = offlinePlayer.getPlayer();
        if (player == null) {
            return null;
        }
        return onPlaceholderRequest(player, params);
    }

    /**
     * Looks up a flag value at the given location, checking subclaim, claim,
     * world, and server scopes in descending priority order.
     *
     * @param flagId   the flag identifier
     * @param location the location to check
     * @return the resolved flag value, or an empty string if not set
     */
    @NotNull
    private String getFlagValue(@NotNull String flagId, @NotNull Location location) {
        // Check subclaim first (highest priority)
        if (gpHook.isInSubclaim(location)) {
            String subclaimId = gpHook.getSubclaimId(location);
            if (subclaimId != null) {
                String value = flagStorage.getValue(flagId, FlagScope.SUBCLAIM, subclaimId);
                if (value != null) {
                    return value;
                }
            }
        }

        // Check claim
        if (gpHook.isInClaim(location)) {
            String claimId = gpHook.getClaimId(location);
            if (claimId != null) {
                String value = flagStorage.getValue(flagId, FlagScope.CLAIM, claimId);
                if (value != null) {
                    return value;
                }
            }
        }

        // Check world
        String worldName = location.getWorld() != null ? location.getWorld().getName() : "world";
        String worldValue = flagStorage.getValue(flagId, FlagScope.WORLD, worldName);
        if (worldValue != null) {
            return worldValue;
        }

        // Check server
        String serverValue = flagStorage.getValue(flagId, FlagScope.SERVER, "server");
        if (serverValue != null) {
            return serverValue;
        }

        return "";
    }

    /**
     * Counts the number of flags set in the claim at the player's current location.
     *
     * @param location the player's current location
     * @return the count as a string, or "0" if not in a claim
     */
    @NotNull
    private String getFlagsCount(@NotNull Location location) {
        if (!gpHook.isInClaim(location)) {
            return "0";
        }

        String claimId = gpHook.getClaimId(location);
        if (claimId == null) {
            return "0";
        }

        Map<String, String> flags = flagStorage.getAllForScope(FlagScope.CLAIM, claimId);
        return String.valueOf(flags.size());
    }
}
