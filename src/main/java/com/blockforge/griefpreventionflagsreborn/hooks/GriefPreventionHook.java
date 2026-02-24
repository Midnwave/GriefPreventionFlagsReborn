package com.blockforge.griefpreventionflagsreborn.hooks;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Integration hook for the GriefPrevention API.
 * <p>
 * Provides utility methods for querying claim ownership, trust levels,
 * and claim/subclaim identification at specific locations. Caches the
 * GriefPrevention plugin instance reference for efficient repeated lookups.
 */
public final class GriefPreventionHook {

    private final Logger logger;
    private GriefPrevention griefPrevention;

    /**
     * Creates a new GriefPreventionHook.
     *
     * @param logger the logger for diagnostic output
     */
    public GriefPreventionHook(@NotNull Logger logger) {
        this.logger = logger;
    }

    /**
     * Attempts to locate and cache the GriefPrevention plugin instance.
     *
     * @return true if GriefPrevention is loaded and available, false otherwise
     */
    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("GriefPrevention") == null) {
            logger.warning("GriefPrevention plugin not found! GPFR requires GriefPrevention to function.");
            return false;
        }

        griefPrevention = GriefPrevention.instance;
        if (griefPrevention == null) {
            logger.warning("GriefPrevention instance is null. The plugin may not be fully loaded.");
            return false;
        }

        logger.info("Successfully hooked into GriefPrevention.");
        return true;
    }

    /**
     * Checks whether a location is inside any GriefPrevention claim.
     *
     * @param location the location to check
     * @return true if the location is within a claim
     */
    public boolean isInClaim(@NotNull Location location) {
        Claim claim = getClaim(location);
        return claim != null;
    }

    /**
     * Checks whether a location is inside a subclaim (a child claim within a parent claim).
     *
     * @param location the location to check
     * @return true if the location is within a subclaim
     */
    public boolean isInSubclaim(@NotNull Location location) {
        Claim claim = getClaim(location);
        return claim != null && claim.parent != null;
    }

    /**
     * Returns the unique claim ID for the claim at a given location.
     * If the location is in a subclaim, this returns the parent claim's ID.
     *
     * @param location the location to look up
     * @return the claim ID as a string, or null if no claim exists at the location
     */
    @Nullable
    public String getClaimId(@NotNull Location location) {
        Claim claim = getClaim(location);
        if (claim == null) {
            return null;
        }
        // If in a subclaim, return the parent claim ID
        if (claim.parent != null) {
            return String.valueOf(claim.parent.getID());
        }
        return String.valueOf(claim.getID());
    }

    /**
     * Returns the unique subclaim ID for the subclaim at a given location.
     *
     * @param location the location to look up
     * @return the subclaim ID as a string, or null if the location is not in a subclaim
     */
    @Nullable
    public String getSubclaimId(@NotNull Location location) {
        Claim claim = getClaim(location);
        if (claim == null || claim.parent == null) {
            return null;
        }
        return String.valueOf(claim.getID());
    }

    /**
     * Checks whether the given player is the owner of the claim at a location.
     *
     * @param location   the location to check
     * @param playerUUID the UUID of the player
     * @return true if the player owns the claim at the given location
     */
    public boolean isClaimOwner(@NotNull Location location, @NotNull UUID playerUUID) {
        Claim claim = getClaim(location);
        if (claim == null) {
            return false;
        }
        // For subclaims, check the parent claim's owner
        Claim targetClaim = claim.parent != null ? claim.parent : claim;
        return playerUUID.equals(targetClaim.ownerID);
    }

    /**
     * Checks whether a player has a specific trust level in the claim at a location.
     * <p>
     * Supported trust types (case-insensitive):
     * <ul>
     *   <li>{@code BUILD} - Build trust (full block modification)</li>
     *   <li>{@code CONTAINER} - Container trust (access chests, etc.)</li>
     *   <li>{@code ACCESS} - Access trust (use buttons, levers, etc.)</li>
     *   <li>{@code PERMISSION} - Permission trust (grant trust to others)</li>
     * </ul>
     *
     * @param location   the location to check
     * @param playerUUID the UUID of the player
     * @param trustType  the trust type to check (BUILD, CONTAINER, ACCESS, PERMISSION)
     * @return true if the player has the specified trust level (or higher) in the claim
     */
    public boolean hasClaimTrust(@NotNull Location location, @NotNull UUID playerUUID, @NotNull String trustType) {
        Claim claim = getClaim(location);
        if (claim == null) {
            return false;
        }

        ClaimPermission permission = switch (trustType.toUpperCase()) {
            case "BUILD" -> ClaimPermission.Build;
            case "CONTAINER" -> ClaimPermission.Inventory;
            case "ACCESS" -> ClaimPermission.Access;
            case "PERMISSION" -> ClaimPermission.Manage;
            default -> null;
        };

        if (permission == null) {
            logger.warning("Unknown trust type: " + trustType);
            return false;
        }

        // allowAccess returns null if access is granted, a denial reason string otherwise
        // checkPermission returns null (or a Supplier) if access is granted, non-null = denied
        Object checkResult = claim.checkPermission(playerUUID, permission, null);
        return checkResult == null;
    }

    /**
     * Returns the UUID of the owner of the claim at a given location.
     *
     * @param location the location to look up
     * @return the owner's UUID, or null if no claim exists or the claim is admin-owned
     */
    @Nullable
    public UUID getClaimOwnerUUID(@NotNull Location location) {
        Claim claim = getClaim(location);
        if (claim == null) {
            return null;
        }
        // For subclaims, return the parent claim owner
        Claim targetClaim = claim.parent != null ? claim.parent : claim;
        return targetClaim.ownerID;
    }

    /**
     * Adds bonus claim blocks to a player's account.
     * Useful for economy/black market integration where claim blocks are a reward.
     *
     * @param playerUUID the UUID of the player
     * @param amount     the number of claim blocks to add (can be negative to remove)
     */
    public void addClaimBlocks(@NotNull UUID playerUUID, int amount) {
        if (griefPrevention == null) {
            logger.warning("Cannot add claim blocks: GriefPrevention is not hooked.");
            return;
        }
        griefPrevention.dataStore.getPlayerData(playerUUID).setBonusClaimBlocks(
            griefPrevention.dataStore.getPlayerData(playerUUID).getBonusClaimBlocks() + amount
        );
    }

    /**
     * Internal helper to retrieve the GriefPrevention claim at a location.
     *
     * @param location the location to look up
     * @return the {@link Claim} at the location, or null if none exists
     */
    @Nullable
    private Claim getClaim(@NotNull Location location) {
        if (griefPrevention == null) {
            return null;
        }
        return griefPrevention.dataStore.getClaimAt(location, true, null);
    }
}
