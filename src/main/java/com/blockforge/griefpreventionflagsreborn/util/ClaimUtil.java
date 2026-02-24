package com.blockforge.griefpreventionflagsreborn.util;

import com.blockforge.griefpreventionflagsreborn.api.FlagScope;
import com.blockforge.griefpreventionflagsreborn.hooks.GriefPreventionHook;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for claim-related operations, providing methods to derive
 * scope identifiers and human-readable scope descriptions.
 */
public final class ClaimUtil {

    private ClaimUtil() {
        // Utility class - no instantiation
    }

    /**
     * Determines the appropriate scope identifier for a given location and flag scope.
     * <p>
     * Returns:
     * <ul>
     *   <li>{@link FlagScope#SERVER} - always returns "server"</li>
     *   <li>{@link FlagScope#WORLD} - returns the world name</li>
     *   <li>{@link FlagScope#CLAIM} - returns the claim ID (via GriefPrevention)</li>
     *   <li>{@link FlagScope#SUBCLAIM} - returns the subclaim ID (via GriefPrevention)</li>
     * </ul>
     *
     * @param location the location to derive the scope ID from
     * @param scope    the flag scope
     * @param gpHook   the GriefPrevention hook for claim lookups
     * @return the scope identifier string, or null if the scope cannot be resolved
     *         (e.g., CLAIM scope but not in a claim)
     */
    @Nullable
    public static String getScopeId(@NotNull Location location, @NotNull FlagScope scope,
                                    @NotNull GriefPreventionHook gpHook) {
        return switch (scope) {
            case SERVER -> "server";
            case WORLD -> {
                if (location.getWorld() != null) {
                    yield location.getWorld().getName();
                }
                yield null;
            }
            case CLAIM -> gpHook.getClaimId(location);
            case SUBCLAIM -> gpHook.getSubclaimId(location);
        };
    }

    /**
     * Returns a human-readable description of a scope and its identifier.
     * <p>
     * Examples:
     * <ul>
     *   <li>SERVER, "server" -> "Server"</li>
     *   <li>WORLD, "world_nether" -> "World: world_nether"</li>
     *   <li>CLAIM, "42" -> "Claim #42"</li>
     *   <li>SUBCLAIM, "105" -> "Subclaim #105"</li>
     * </ul>
     *
     * @param scope   the flag scope
     * @param scopeId the scope identifier
     * @return a human-readable scope description
     */
    @NotNull
    public static String describeScope(@NotNull FlagScope scope, @NotNull String scopeId) {
        return switch (scope) {
            case SERVER -> "Server";
            case WORLD -> "World: " + scopeId;
            case CLAIM -> "Claim #" + scopeId;
            case SUBCLAIM -> "Subclaim #" + scopeId;
        };
    }
}
