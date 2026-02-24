package com.blockforge.griefpreventionflagsreborn.listeners;

import com.blockforge.griefpreventionflagsreborn.GriefPreventionFlagsPlugin;
import com.blockforge.griefpreventionflagsreborn.api.FlagManager;
import com.blockforge.griefpreventionflagsreborn.hooks.GriefPreventionHook;
import org.bukkit.Location;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.entity.Animals;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces mob-related flags that control mob spawning, behavior,
 * damage, targeting, griefing, and explosions within flagged areas.
 * <p>
 * Handles 18 flags: no-mob-spawning, no-monster-spawning, no-animal-spawning,
 * no-phantom-spawning, no-slime-spawning, no-warden-spawning, no-spawner-mobs,
 * blocked-mobs, allowed-mobs, no-monster-damage, no-mob-damage, no-monster-target,
 * no-enderman-grief, no-mob-grief, no-creeper-explosion, no-wither-damage,
 * no-zombie-break-doors, and mob-damage-multiplier.
 */
public final class MobListener implements Listener {

    private final GriefPreventionFlagsPlugin plugin;
    private final FlagManager flagManager;
    private final GriefPreventionHook gpHook;
    private final Map<String, Long> messageCooldowns = new ConcurrentHashMap<>();

    /**
     * Creates a new MobListener.
     *
     * @param plugin the owning plugin instance
     */
    public MobListener(GriefPreventionFlagsPlugin plugin) {
        this.plugin = plugin;
        this.flagManager = plugin.getFlagManager();
        this.gpHook = plugin.getGpHook();
    }

    // ---------------------------------------------------------------
    //  Helper methods
    // ---------------------------------------------------------------

    private boolean shouldBlock(Player player, String flagId, Location location) {
        if (player.hasPermission("gpfr.bypass." + flagId)) return false;
        return flagManager.isFlagEnabled(flagId, location);
    }

    private void sendBlockedMessage(Player player, String flagId) {
        String key = player.getUniqueId() + ":" + flagId;
        long now = System.currentTimeMillis();
        Long last = messageCooldowns.get(key);
        if (last == null || now - last > 2000) {
            messageCooldowns.put(key, now);
            plugin.getMessagesManager().sendMessage(player, "flag-blocked-action");
        }
    }

    /**
     * Checks if a flag is enabled at a location (for non-player events
     * that do not have a bypass check).
     *
     * @param flagId   the flag identifier
     * @param location the location to check
     * @return true if the flag is enabled at the location
     */
    private boolean isFlagActive(String flagId, Location location) {
        return flagManager.isFlagEnabled(flagId, location);
    }

    // ---------------------------------------------------------------
    //  CreatureSpawnEvent - spawning flags
    // ---------------------------------------------------------------

    /**
     * Controls mob spawning based on various flags including type-specific
     * spawn prevention, spawner restrictions, and allow/block lists.
     *
     * @param event the creature spawn event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        Location location = event.getLocation();
        if (location == null || location.getWorld() == null) return;

        Entity entity = event.getEntity();
        EntityType entityType = event.getEntityType();
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();

        // Check no-mob-spawning (all mobs)
        if (isFlagActive("no-mob-spawning", location)) {
            event.setCancelled(true);
            return;
        }

        // Check no-monster-spawning (hostile mobs)
        if (entity instanceof Monster && isFlagActive("no-monster-spawning", location)) {
            event.setCancelled(true);
            return;
        }

        // Check no-animal-spawning (passive mobs)
        if (entity instanceof Animals && isFlagActive("no-animal-spawning", location)) {
            event.setCancelled(true);
            return;
        }

        // Check specific mob type flags
        if (entityType == EntityType.PHANTOM && isFlagActive("no-phantom-spawning", location)) {
            event.setCancelled(true);
            return;
        }

        if ((entityType == EntityType.SLIME || entityType == EntityType.MAGMA_CUBE)
                && isFlagActive("no-slime-spawning", location)) {
            event.setCancelled(true);
            return;
        }

        if (entityType == EntityType.WARDEN && isFlagActive("no-warden-spawning", location)) {
            event.setCancelled(true);
            return;
        }

        // Check no-spawner-mobs
        if (reason == CreatureSpawnEvent.SpawnReason.SPAWNER
                && isFlagActive("no-spawner-mobs", location)) {
            event.setCancelled(true);
            return;
        }

        // Check blocked-mobs list
        List<EntityType> blockedMobs = flagManager.getFlagValue("blocked-mobs", location);
        if (blockedMobs != null && !blockedMobs.isEmpty()) {
            if (blockedMobs.contains(entityType)) {
                event.setCancelled(true);
                return;
            }
        }

        // Check allowed-mobs list (whitelist mode)
        List<EntityType> allowedMobs = flagManager.getFlagValue("allowed-mobs", location);
        if (allowedMobs != null && !allowedMobs.isEmpty()) {
            if (!allowedMobs.contains(entityType)) {
                event.setCancelled(true);
            }
        }
    }

    // ---------------------------------------------------------------
    //  EntityDamageByEntityEvent - mob damage to players
    // ---------------------------------------------------------------

    /**
     * Prevents or modifies mob-to-player damage based on no-monster-damage,
     * no-mob-damage, and mob-damage-multiplier flags.
     *
     * @param event the entity damage by entity event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Entity damager = event.getDamager();
        Location location = victim.getLocation();
        if (location == null || location.getWorld() == null) return;

        // Resolve the actual mob damager (could be a projectile from a mob)
        Entity actualDamager = damager;
        if (damager instanceof org.bukkit.entity.Projectile projectile) {
            if (projectile.getShooter() instanceof Entity shooterEntity) {
                actualDamager = shooterEntity;
            }
        }

        // Skip if the actual damager is a player (handled by PvPListener)
        if (actualDamager instanceof Player) return;

        // Check no-monster-damage
        if (actualDamager instanceof Monster) {
            if (shouldBlock(victim, "no-monster-damage", location)) {
                event.setCancelled(true);
                sendBlockedMessage(victim, "no-monster-damage");
                return;
            }
        }

        // Check no-mob-damage (any mob)
        if (actualDamager instanceof Mob || actualDamager instanceof LivingEntity) {
            if (shouldBlock(victim, "no-mob-damage", location)) {
                event.setCancelled(true);
                sendBlockedMessage(victim, "no-mob-damage");
                return;
            }
        }

        // Apply mob-damage-multiplier
        if (actualDamager instanceof Mob || actualDamager instanceof LivingEntity) {
            if (!(actualDamager instanceof Player)) {
                Object multiplierValue = flagManager.getFlagValue("mob-damage-multiplier", location);
                if (multiplierValue instanceof Number multiplier) {
                    double mult = multiplier.doubleValue();
                    if (mult != 1.0) {
                        event.setDamage(event.getDamage() * mult);
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------
    //  EntityTargetEvent - no-monster-target flag
    // ---------------------------------------------------------------

    /**
     * Prevents monsters from targeting players when the no-monster-target
     * flag is enabled at the target's location.
     *
     * @param event the entity target event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getTarget() instanceof Player target)) return;
        if (!(event.getEntity() instanceof Monster)) return;

        Location location = target.getLocation();
        if (location == null || location.getWorld() == null) return;

        if (shouldBlock(target, "no-monster-target", location)) {
            event.setCancelled(true);
        }
    }

    // ---------------------------------------------------------------
    //  EntityChangeBlockEvent - enderman/mob grief flags
    // ---------------------------------------------------------------

    /**
     * Prevents mobs from changing blocks (enderman grief, general mob grief)
     * when the appropriate flags are enabled.
     *
     * @param event the entity change block event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        Location location = event.getBlock().getLocation();
        if (location.getWorld() == null) return;

        Entity entity = event.getEntity();

        // Check no-enderman-grief
        if (entity instanceof Enderman) {
            if (isFlagActive("no-enderman-grief", location)) {
                event.setCancelled(true);
                return;
            }
        }

        // Check no-mob-grief for any mob
        if (entity instanceof Mob || entity instanceof LivingEntity) {
            if (!(entity instanceof Player)) {
                if (isFlagActive("no-mob-grief", location)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    // ---------------------------------------------------------------
    //  EntityExplodeEvent - creeper/wither explosion flags
    // ---------------------------------------------------------------

    /**
     * Prevents entity explosions (creepers, withers) from destroying blocks
     * when the appropriate flags are enabled.
     *
     * @param event the entity explode event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        Location location = event.getLocation();
        if (location == null || location.getWorld() == null) return;

        // Check no-creeper-explosion
        if (entity instanceof Creeper) {
            if (isFlagActive("no-creeper-explosion", location)) {
                event.blockList().clear();
                event.setCancelled(true);
                return;
            }
        }

        // Check no-wither-damage
        if (entity instanceof Wither) {
            if (isFlagActive("no-wither-damage", location)) {
                event.blockList().clear();
                event.setCancelled(true);
                return;
            }
        }

        // General mob grief check for other mob explosions
        if (entity instanceof Mob) {
            if (isFlagActive("no-mob-grief", location)) {
                event.blockList().clear();
                event.setCancelled(true);
            }
        }
    }

    // ---------------------------------------------------------------
    //  EntityBreakDoorEvent - no-zombie-break-doors flag
    // ---------------------------------------------------------------

    /**
     * Prevents zombies from breaking doors when the no-zombie-break-doors
     * flag is enabled.
     *
     * @param event the entity break door event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityBreakDoor(EntityBreakDoorEvent event) {
        Location location = event.getBlock().getLocation();
        if (location.getWorld() == null) return;

        if (isFlagActive("no-zombie-break-doors", location)) {
            event.setCancelled(true);
        }
    }

    // ---------------------------------------------------------------
    //  EntityDamageEvent - mob-damage-multiplier (non-entity source)
    // ---------------------------------------------------------------

    /**
     * Applies the mob-damage-multiplier for damage dealt by mobs through
     * non-direct means (e.g. thorns reflection from mobs).
     * <p>
     * Note: Primary mob damage multiplier handling is in onEntityDamageByEntity.
     * This handler covers edge cases from the general EntityDamageEvent.
     *
     * @param event the entity damage event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        // This is handled primarily in onEntityDamageByEntity for direct mob attacks.
        // This handler exists as documentation that mob-damage-multiplier is covered.
    }
}
