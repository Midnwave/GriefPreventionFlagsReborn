package com.blockforge.griefpreventionflagsreborn.listeners;

import com.blockforge.griefpreventionflagsreborn.GriefPreventionFlagsPlugin;
import com.blockforge.griefpreventionflagsreborn.api.FlagManager;
import com.blockforge.griefpreventionflagsreborn.hooks.GriefPreventionHook;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces flags related to Minecraft 1.21 content, including mace usage,
 * trial spawners, vault blocks, wind charges, breeze mobs, and copper
 * oxidation mechanics.
 * <p>
 * All event handlers are wrapped in try-catch blocks to gracefully handle
 * servers running older versions where 1.21 classes may not exist.
 * <p>
 * Handles 6 flags: no-mace-use, no-trial-spawner, no-vault-access,
 * no-wind-charge, no-breeze-spawning, and no-copper-oxidation.
 */
public final class ContentListener121 implements Listener {

    private final GriefPreventionFlagsPlugin plugin;
    private final FlagManager flagManager;
    private final GriefPreventionHook gpHook;
    private final Map<String, Long> messageCooldowns = new ConcurrentHashMap<>();

    // Pre-check if 1.21 materials/entities exist
    private final boolean hasMace;
    private final boolean hasTrialSpawner;
    private final boolean hasVault;
    private final boolean hasWindCharge;
    private final boolean hasBreeze;

    /**
     * Creates a new ContentListener121.
     *
     * @param plugin the owning plugin instance
     */
    public ContentListener121(GriefPreventionFlagsPlugin plugin) {
        this.plugin = plugin;
        this.flagManager = plugin.getFlagManager();
        this.gpHook = plugin.getGpHook();

        // Detect 1.21 content availability
        this.hasMace = materialExists("MACE");
        this.hasTrialSpawner = materialExists("TRIAL_SPAWNER");
        this.hasVault = materialExists("VAULT");
        this.hasWindCharge = materialExists("WIND_CHARGE");
        this.hasBreeze = entityTypeExists("BREEZE");
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

    private boolean isFlagActive(String flagId, Location location) {
        return flagManager.isFlagEnabled(flagId, location);
    }

    /**
     * Checks if a Material enum value exists by name.
     *
     * @param name the material name to check
     * @return true if the material exists in this server version
     */
    private static boolean materialExists(String name) {
        try {
            Material.valueOf(name);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Checks if an EntityType enum value exists by name.
     *
     * @param name the entity type name to check
     * @return true if the entity type exists in this server version
     */
    private static boolean entityTypeExists(String name) {
        try {
            EntityType.valueOf(name);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Checks if a material name indicates a copper block that is not
     * waxed and not already in its final oxidation state.
     *
     * @param material the material to check
     * @return true if the material is an oxidizable copper variant
     */
    private boolean isOxidizableCopper(Material material) {
        String name = material.name();
        // Must contain COPPER and not be waxed
        if (!name.contains("COPPER")) return false;
        if (name.startsWith("WAXED_")) return false;
        // Oxidized copper is already fully oxidized
        if (name.startsWith("OXIDIZED_")) return false;
        return true;
    }

    // ---------------------------------------------------------------
    //  EntityDamageByEntityEvent - no-mace-use flag
    // ---------------------------------------------------------------

    /**
     * Prevents players from dealing damage with the mace weapon when
     * the no-mace-use flag is enabled.
     *
     * @param event the entity damage by entity event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!hasMace) return;

        try {
            if (!(event.getDamager() instanceof Player player)) return;

            Location location = player.getLocation();
            if (location == null || location.getWorld() == null) return;

            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand.getType() == Material.MACE) {
                if (shouldBlock(player, "no-mace-use", location)) {
                    event.setCancelled(true);
                    sendBlockedMessage(player, "no-mace-use");
                }
            }
        } catch (NoClassDefFoundError | NoSuchFieldError ignored) {
            // Material.MACE does not exist on this server version
        }
    }

    // ---------------------------------------------------------------
    //  PlayerInteractEvent - no-trial-spawner, no-vault-access flags
    // ---------------------------------------------------------------

    /**
     * Prevents players from interacting with trial spawners and vault
     * blocks when the respective flags are enabled.
     *
     * @param event the player interact event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();
        Location location = block.getLocation();
        if (location.getWorld() == null) return;

        Material material = block.getType();

        // Check no-trial-spawner
        if (hasTrialSpawner) {
            try {
                if (material == Material.TRIAL_SPAWNER) {
                    if (shouldBlock(player, "no-trial-spawner", location)) {
                        event.setCancelled(true);
                        sendBlockedMessage(player, "no-trial-spawner");
                        return;
                    }
                }
            } catch (NoClassDefFoundError | NoSuchFieldError ignored) {
                // Material.TRIAL_SPAWNER does not exist
            }
        }

        // Check no-vault-access
        if (hasVault) {
            try {
                if (material == Material.VAULT) {
                    if (shouldBlock(player, "no-vault-access", location)) {
                        event.setCancelled(true);
                        sendBlockedMessage(player, "no-vault-access");
                    }
                }
            } catch (NoClassDefFoundError | NoSuchFieldError ignored) {
                // Material.VAULT does not exist
            }
        }
    }

    // ---------------------------------------------------------------
    //  ProjectileLaunchEvent - no-wind-charge flag
    // ---------------------------------------------------------------

    /**
     * Prevents players from launching wind charges when the no-wind-charge
     * flag is enabled.
     *
     * @param event the projectile launch event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!hasWindCharge) return;

        try {
            Entity projectile = event.getEntity();

            // Check if the projectile is a wind charge
            boolean isWindCharge = false;
            try {
                isWindCharge = projectile.getType() == EntityType.WIND_CHARGE;
            } catch (NoSuchFieldError ignored) {
                return;
            }

            if (!isWindCharge) return;

            ProjectileSource source = event.getEntity().getShooter();
            if (!(source instanceof Player player)) return;

            Location location = player.getLocation();
            if (location == null || location.getWorld() == null) return;

            if (shouldBlock(player, "no-wind-charge", location)) {
                event.setCancelled(true);
                sendBlockedMessage(player, "no-wind-charge");
            }
        } catch (NoClassDefFoundError | NoSuchFieldError ignored) {
            // Wind charge entity type does not exist
        }
    }

    // ---------------------------------------------------------------
    //  CreatureSpawnEvent - no-breeze-spawning flag
    // ---------------------------------------------------------------

    /**
     * Prevents breeze mobs from spawning when the no-breeze-spawning
     * flag is enabled.
     *
     * @param event the creature spawn event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!hasBreeze) return;

        try {
            if (event.getEntityType() != EntityType.BREEZE) return;

            Location location = event.getLocation();
            if (location == null || location.getWorld() == null) return;

            if (isFlagActive("no-breeze-spawning", location)) {
                event.setCancelled(true);
            }
        } catch (NoClassDefFoundError | NoSuchFieldError ignored) {
            // EntityType.BREEZE does not exist
        }
    }

    // ---------------------------------------------------------------
    //  BlockFormEvent - no-copper-oxidation flag
    // ---------------------------------------------------------------

    /**
     * Prevents copper blocks from oxidizing when the no-copper-oxidation
     * flag is enabled. This catches the block state change when copper
     * transitions between oxidation stages.
     *
     * @param event the block form event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();
        if (location.getWorld() == null) return;

        Material currentMaterial = block.getType();
        Material newMaterial = event.getNewState().getType();

        // Check if this is a copper oxidation event
        // Copper oxidation occurs when a non-oxidized copper block forms
        // into a more oxidized state
        if (isOxidizableCopper(currentMaterial) && isOxidizedVariant(currentMaterial, newMaterial)) {
            if (isFlagActive("no-copper-oxidation", location)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Checks if the new material is a more oxidized variant of the current
     * copper material. Copper oxidation follows the chain:
     * COPPER -> EXPOSED -> WEATHERED -> OXIDIZED
     *
     * @param current the current block material
     * @param next    the proposed new material
     * @return true if the new material represents an oxidation progression
     */
    private boolean isOxidizedVariant(Material current, Material next) {
        String currentName = current.name();
        String nextName = next.name();

        // Both must be copper
        if (!currentName.contains("COPPER") || !nextName.contains("COPPER")) return false;

        // The next state should have a higher oxidation level
        int currentLevel = getOxidationLevel(currentName);
        int nextLevel = getOxidationLevel(nextName);

        return nextLevel > currentLevel;
    }

    /**
     * Gets the oxidation level from a copper material name.
     *
     * @param name the material name
     * @return 0 for base, 1 for exposed, 2 for weathered, 3 for oxidized
     */
    private int getOxidationLevel(String name) {
        if (name.contains("OXIDIZED")) return 3;
        if (name.contains("WEATHERED")) return 2;
        if (name.contains("EXPOSED")) return 1;
        return 0;
    }
}
