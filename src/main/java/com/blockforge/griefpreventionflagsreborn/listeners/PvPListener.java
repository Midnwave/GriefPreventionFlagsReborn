package com.blockforge.griefpreventionflagsreborn.listeners;

import com.blockforge.griefpreventionflagsreborn.GriefPreventionFlagsPlugin;
import com.blockforge.griefpreventionflagsreborn.api.FlagManager;
import com.blockforge.griefpreventionflagsreborn.hooks.GriefPreventionHook;
import com.blockforge.griefpreventionflagsreborn.util.AdventureCompat;
import org.bukkit.Location;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces PvP-related flags that control player-versus-player combat
 * mechanics within flagged areas.
 * <p>
 * Handles 8 flags: no-pvp, no-player-damage, no-projectile-pvp,
 * no-end-crystal-pvp, forced-pvp, no-potion-pvp, bounty-hunting-allowed,
 * and pvp-deny-message.
 */
public final class PvPListener implements Listener {

    private final GriefPreventionFlagsPlugin plugin;
    private final FlagManager flagManager;
    private final GriefPreventionHook gpHook;
    private final Map<String, Long> messageCooldowns = new ConcurrentHashMap<>();

    /**
     * Creates a new PvPListener.
     *
     * @param plugin the owning plugin instance
     */
    public PvPListener(GriefPreventionFlagsPlugin plugin) {
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
     * Sends a custom PvP deny message if one is configured, otherwise sends
     * the default blocked-action message.
     *
     * @param player   the player to notify
     * @param location the location to check for the custom message
     */
    private void sendPvPDenyMessage(Player player, Location location) {
        String key = player.getUniqueId() + ":no-pvp";
        long now = System.currentTimeMillis();
        Long last = messageCooldowns.get(key);
        if (last != null && now - last <= 2000) return;
        messageCooldowns.put(key, now);

        String customMessage = flagManager.getFlagValue("pvp-deny-message", location);
        if (customMessage != null && !customMessage.isEmpty()) {
            AdventureCompat.sendMessage(player, customMessage);
        } else {
            plugin.getMessagesManager().sendMessage(player, "flag-blocked-action");
        }
    }

    /**
     * Resolves the actual attacking player from a damage event.
     * Handles direct player attacks and projectile attacks where the
     * shooter is a player.
     *
     * @param damager the direct damager entity
     * @return the attacking player, or null if the damager is not player-sourced
     */
    private Player getAttackingPlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------
    //  EntityDamageByEntityEvent - PvP flags
    // ---------------------------------------------------------------

    /**
     * Handles PvP damage events including direct combat, projectile attacks,
     * end crystal damage, and forced PvP zones.
     *
     * @param event the entity damage by entity event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Entity damager = event.getDamager();
        Player attacker = getAttackingPlayer(damager);

        // Not a PvP event if no attacking player is found
        if (attacker == null) {
            // Check for end crystal PvP - end crystal has no player source directly
            // but can be placed by a player. Check if the damager is an end crystal.
            if (damager instanceof EnderCrystal) {
                Location victimLocation = victim.getLocation();
                if (victimLocation == null || victimLocation.getWorld() == null) return;

                if (shouldBlock(victim, "no-end-crystal-pvp", victimLocation)) {
                    event.setCancelled(true);
                    sendBlockedMessage(victim, "no-end-crystal-pvp");
                }
            }
            return;
        }

        // Don't process self-damage
        if (attacker.equals(victim)) return;

        Location victimLocation = victim.getLocation();
        Location attackerLocation = attacker.getLocation();
        if (victimLocation == null || victimLocation.getWorld() == null) return;

        // Check forced-pvp first - if enabled, force PvP on regardless of other flags
        boolean forcedPvpAtVictim = flagManager.isFlagEnabled("forced-pvp", victimLocation);
        boolean forcedPvpAtAttacker = attackerLocation != null && attackerLocation.getWorld() != null
                && flagManager.isFlagEnabled("forced-pvp", attackerLocation);

        if (forcedPvpAtVictim || forcedPvpAtAttacker) {
            // Forced PvP overrides - ensure event is not cancelled
            event.setCancelled(false);
            return;
        }

        // Check no-pvp at victim's location
        if (shouldBlock(victim, "no-pvp", victimLocation)) {
            event.setCancelled(true);
            sendPvPDenyMessage(attacker, victimLocation);
            return;
        }

        // Also check no-pvp at attacker's location
        if (attackerLocation != null && attackerLocation.getWorld() != null) {
            if (shouldBlock(attacker, "no-pvp", attackerLocation)) {
                event.setCancelled(true);
                sendPvPDenyMessage(attacker, attackerLocation);
                return;
            }
        }

        // Check no-player-damage at victim's location
        if (shouldBlock(victim, "no-player-damage", victimLocation)) {
            event.setCancelled(true);
            sendBlockedMessage(attacker, "no-player-damage");
            return;
        }

        // Check no-projectile-pvp if the damage source is a projectile
        if (damager instanceof Projectile) {
            if (shouldBlock(victim, "no-projectile-pvp", victimLocation)) {
                event.setCancelled(true);
                sendBlockedMessage(attacker, "no-projectile-pvp");
                return;
            }
        }

        // Check no-end-crystal-pvp if damage is from an explosion caused by end crystal
        if (event.getCause() == EntityDamageByEntityEvent.DamageCause.ENTITY_EXPLOSION
                && damager instanceof EnderCrystal) {
            if (shouldBlock(victim, "no-end-crystal-pvp", victimLocation)) {
                event.setCancelled(true);
                sendBlockedMessage(attacker, "no-end-crystal-pvp");
            }
        }
    }

    // ---------------------------------------------------------------
    //  PotionSplashEvent - no-potion-pvp flag
    // ---------------------------------------------------------------

    /**
     * Prevents harmful splash potions from affecting other players when
     * the no-potion-pvp flag is enabled.
     *
     * @param event the potion splash event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        ProjectileSource source = event.getPotion().getShooter();
        if (!(source instanceof Player thrower)) return;

        Location throwerLocation = thrower.getLocation();
        if (throwerLocation == null || throwerLocation.getWorld() == null) return;

        // Check if no-potion-pvp is enabled at thrower's location
        boolean blockedAtThrower = flagManager.isFlagEnabled("no-potion-pvp", throwerLocation)
                && !thrower.hasPermission("gpfr.bypass.no-potion-pvp");

        // Remove affected players from splash effect
        for (org.bukkit.entity.LivingEntity affected : event.getAffectedEntities()) {
            if (!(affected instanceof Player affectedPlayer)) continue;
            if (affectedPlayer.equals(thrower)) continue;

            Location affectedLocation = affectedPlayer.getLocation();
            if (affectedLocation == null || affectedLocation.getWorld() == null) continue;

            boolean blockedAtVictim = flagManager.isFlagEnabled("no-potion-pvp", affectedLocation)
                    && !affectedPlayer.hasPermission("gpfr.bypass.no-potion-pvp");

            if (blockedAtThrower || blockedAtVictim) {
                event.setIntensity(affectedPlayer, 0);
                sendBlockedMessage(thrower, "no-potion-pvp");
            }
        }
    }

    // ---------------------------------------------------------------
    //  PlayerDeathEvent - bounty-hunting-allowed flag
    // ---------------------------------------------------------------

    /**
     * Tracks PvP kills for bounty-hunting-allowed flag integration.
     * The actual bounty logic is handled by HorizonUtilities; this listener
     * simply ensures the flag is respected.
     *
     * @param event the player death event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;

        Location location = victim.getLocation();
        if (location == null || location.getWorld() == null) return;

        // The bounty-hunting-allowed flag is informational for HorizonUtilities.
        // It is checked by HorizonUtilities via the GPFR API, so no direct
        // enforcement is needed here. This handler exists as documentation
        // of where the flag integration point is.
    }
}
