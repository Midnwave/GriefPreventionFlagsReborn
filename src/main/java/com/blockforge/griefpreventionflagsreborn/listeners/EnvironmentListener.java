package com.blockforge.griefpreventionflagsreborn.listeners;

import com.blockforge.griefpreventionflagsreborn.GriefPreventionFlagsPlugin;
import com.blockforge.griefpreventionflagsreborn.api.FlagManager;
import com.blockforge.griefpreventionflagsreborn.hooks.GriefPreventionHook;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.StructureGrowEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces environment-related flags that control natural world processes
 * such as fire spread, growth, weather, liquid flow, and block changes.
 * <p>
 * Handles 22 flags: no-fire-spread, no-vine-growth, no-mushroom-spread,
 * no-sculk-spread, no-grass-spread, no-liquid-flow, no-water-flow,
 * no-lava-flow, no-leaf-decay, no-crop-growth, no-kelp-growth,
 * no-dripstone-growth, no-tree-growth, no-snow-form, no-ice-form,
 * no-snow-melt, no-ice-melt, no-coral-death, no-explosions,
 * no-block-explosion, no-weather-change, and force-weather.
 */
public final class EnvironmentListener implements Listener {

    private final GriefPreventionFlagsPlugin plugin;
    private final FlagManager flagManager;
    private final GriefPreventionHook gpHook;
    private final Map<String, Long> messageCooldowns = new ConcurrentHashMap<>();

    /**
     * Creates a new EnvironmentListener.
     *
     * @param plugin the owning plugin instance
     */
    public EnvironmentListener(GriefPreventionFlagsPlugin plugin) {
        this.plugin = plugin;
        this.flagManager = plugin.getFlagManager();
        this.gpHook = plugin.getGpHook();
    }

    // ---------------------------------------------------------------
    //  Helper methods
    // ---------------------------------------------------------------

    /**
     * Checks if a flag is enabled at a location. Environment events
     * typically have no player involved, so no bypass check is needed.
     *
     * @param flagId   the flag identifier
     * @param location the location to check
     * @return true if the flag is enabled at the location
     */
    private boolean isFlagActive(String flagId, Location location) {
        return flagManager.isFlagEnabled(flagId, location);
    }

    /**
     * Checks if a material name contains a given substring (case-insensitive).
     *
     * @param material  the material to check
     * @param substring the substring to search for
     * @return true if the material name contains the substring
     */
    private boolean materialContains(Material material, String substring) {
        return material.name().contains(substring);
    }

    // ---------------------------------------------------------------
    //  BlockSpreadEvent - spread flags
    // ---------------------------------------------------------------

    /**
     * Controls block spreading (fire, vines, mushrooms, sculk, grass)
     * based on the appropriate flags.
     *
     * @param event the block spread event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        Block source = event.getSource();
        Location location = event.getBlock().getLocation();
        if (location.getWorld() == null) return;

        Material sourceMaterial = source.getType();

        // Fire spread
        if (sourceMaterial == Material.FIRE || sourceMaterial == Material.SOUL_FIRE) {
            if (isFlagActive("no-fire-spread", location)) {
                event.setCancelled(true);
                return;
            }
        }

        // Vine growth
        if (sourceMaterial == Material.VINE || sourceMaterial == Material.CAVE_VINES
                || sourceMaterial == Material.CAVE_VINES_PLANT
                || sourceMaterial == Material.TWISTING_VINES
                || sourceMaterial == Material.TWISTING_VINES_PLANT
                || sourceMaterial == Material.WEEPING_VINES
                || sourceMaterial == Material.WEEPING_VINES_PLANT) {
            if (isFlagActive("no-vine-growth", location)) {
                event.setCancelled(true);
                return;
            }
        }

        // Mushroom spread
        if (sourceMaterial == Material.RED_MUSHROOM || sourceMaterial == Material.BROWN_MUSHROOM) {
            if (isFlagActive("no-mushroom-spread", location)) {
                event.setCancelled(true);
                return;
            }
        }

        // Sculk spread
        if (materialContains(sourceMaterial, "SCULK")) {
            if (isFlagActive("no-sculk-spread", location)) {
                event.setCancelled(true);
                return;
            }
        }

        // Grass spread
        if (sourceMaterial == Material.GRASS_BLOCK) {
            if (isFlagActive("no-grass-spread", location)) {
                event.setCancelled(true);
            }
        }
    }

    // ---------------------------------------------------------------
    //  BlockBurnEvent - no-fire-spread flag
    // ---------------------------------------------------------------

    /**
     * Prevents blocks from being destroyed by fire when the no-fire-spread
     * flag is enabled.
     *
     * @param event the block burn event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        Location location = event.getBlock().getLocation();
        if (location.getWorld() == null) return;

        if (isFlagActive("no-fire-spread", location)) {
            event.setCancelled(true);
        }
    }

    // ---------------------------------------------------------------
    //  BlockFromToEvent - liquid flow flags
    // ---------------------------------------------------------------

    /**
     * Controls liquid flow (water and lava) based on no-water-flow,
     * no-lava-flow, and no-liquid-flow flags.
     *
     * @param event the block from-to event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        Block source = event.getBlock();
        Location location = event.getToBlock().getLocation();
        if (location.getWorld() == null) return;

        Material sourceMaterial = source.getType();

        // Check no-liquid-flow (covers both water and lava)
        if (sourceMaterial == Material.WATER || sourceMaterial == Material.LAVA) {
            if (isFlagActive("no-liquid-flow", location)) {
                event.setCancelled(true);
                return;
            }
        }

        // Check no-water-flow
        if (sourceMaterial == Material.WATER) {
            if (isFlagActive("no-water-flow", location)) {
                event.setCancelled(true);
                return;
            }
        }

        // Check no-lava-flow
        if (sourceMaterial == Material.LAVA) {
            if (isFlagActive("no-lava-flow", location)) {
                event.setCancelled(true);
            }
        }
    }

    // ---------------------------------------------------------------
    //  LeavesDecayEvent - no-leaf-decay flag
    // ---------------------------------------------------------------

    /**
     * Prevents natural leaf decay when the no-leaf-decay flag is enabled.
     *
     * @param event the leaves decay event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent event) {
        Location location = event.getBlock().getLocation();
        if (location.getWorld() == null) return;

        if (isFlagActive("no-leaf-decay", location)) {
            event.setCancelled(true);
        }
    }

    // ---------------------------------------------------------------
    //  BlockGrowEvent - crop/kelp/dripstone growth flags
    // ---------------------------------------------------------------

    /**
     * Controls block growth for crops, kelp, and dripstone based on
     * the appropriate flags.
     *
     * @param event the block grow event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();
        if (location.getWorld() == null) return;

        Material material = event.getNewState().getType();

        // Check no-crop-growth for crop-type blocks
        if (isCrop(material)) {
            if (isFlagActive("no-crop-growth", location)) {
                event.setCancelled(true);
                return;
            }
        }

        // Check no-kelp-growth
        if (material == Material.KELP || material == Material.KELP_PLANT) {
            if (isFlagActive("no-kelp-growth", location)) {
                event.setCancelled(true);
                return;
            }
        }

        // Check no-dripstone-growth
        if (material == Material.POINTED_DRIPSTONE) {
            if (isFlagActive("no-dripstone-growth", location)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Determines if a material is a crop or crop-like block.
     *
     * @param material the material to check
     * @return true if the material is a crop
     */
    private boolean isCrop(Material material) {
        return material == Material.WHEAT
                || material == Material.CARROTS
                || material == Material.POTATOES
                || material == Material.BEETROOTS
                || material == Material.MELON_STEM
                || material == Material.PUMPKIN_STEM
                || material == Material.ATTACHED_MELON_STEM
                || material == Material.ATTACHED_PUMPKIN_STEM
                || material == Material.MELON
                || material == Material.PUMPKIN
                || material == Material.SWEET_BERRY_BUSH
                || material == Material.COCOA
                || material == Material.NETHER_WART
                || material == Material.SUGAR_CANE
                || material == Material.CACTUS
                || material == Material.BAMBOO
                || material == Material.CHORUS_PLANT
                || material == Material.CHORUS_FLOWER
                || material == Material.TORCHFLOWER_CROP
                || material == Material.PITCHER_CROP;
    }

    // ---------------------------------------------------------------
    //  StructureGrowEvent - no-tree-growth flag
    // ---------------------------------------------------------------

    /**
     * Prevents tree growth from saplings when the no-tree-growth flag
     * is enabled.
     *
     * @param event the structure grow event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        Location location = event.getLocation();
        if (location == null || location.getWorld() == null) return;

        if (isFlagActive("no-tree-growth", location)) {
            event.setCancelled(true);
        }
    }

    // ---------------------------------------------------------------
    //  BlockFormEvent - snow/ice formation flags
    // ---------------------------------------------------------------

    /**
     * Controls block formation (snow, ice) based on the appropriate flags.
     *
     * @param event the block form event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        Location location = event.getBlock().getLocation();
        if (location.getWorld() == null) return;

        Material newMaterial = event.getNewState().getType();

        // Check no-snow-form
        if (newMaterial == Material.SNOW || newMaterial == Material.SNOW_BLOCK) {
            if (isFlagActive("no-snow-form", location)) {
                event.setCancelled(true);
                return;
            }
        }

        // Check no-ice-form
        if (newMaterial == Material.ICE || newMaterial == Material.FROSTED_ICE) {
            if (isFlagActive("no-ice-form", location)) {
                event.setCancelled(true);
            }
        }
    }

    // ---------------------------------------------------------------
    //  BlockFadeEvent - snow/ice melt and coral death flags
    // ---------------------------------------------------------------

    /**
     * Controls block fading (snow melting, ice melting, coral death)
     * based on the appropriate flags.
     *
     * @param event the block fade event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();
        if (location.getWorld() == null) return;

        Material currentMaterial = block.getType();

        // Check no-snow-melt
        if (currentMaterial == Material.SNOW || currentMaterial == Material.SNOW_BLOCK) {
            if (isFlagActive("no-snow-melt", location)) {
                event.setCancelled(true);
                return;
            }
        }

        // Check no-ice-melt
        if (currentMaterial == Material.ICE || currentMaterial == Material.FROSTED_ICE) {
            if (isFlagActive("no-ice-melt", location)) {
                event.setCancelled(true);
                return;
            }
        }

        // Check no-coral-death
        if (isCoral(currentMaterial)) {
            if (isFlagActive("no-coral-death", location)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Determines if a material is a living coral block, coral, or coral fan.
     *
     * @param material the material to check
     * @return true if the material is a living coral variant
     */
    private boolean isCoral(Material material) {
        String name = material.name();
        // Living coral contains the word CORAL but not DEAD_
        return name.contains("CORAL") && !name.startsWith("DEAD_");
    }

    // ---------------------------------------------------------------
    //  BlockExplodeEvent - explosion flags
    // ---------------------------------------------------------------

    /**
     * Prevents block explosions (TNT, beds, respawn anchors) from
     * destroying blocks when the appropriate flags are enabled.
     *
     * @param event the block explode event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        Location location = event.getBlock().getLocation();
        if (location.getWorld() == null) return;

        // Check no-explosions (all explosions)
        if (isFlagActive("no-explosions", location)) {
            event.blockList().clear();
            event.setCancelled(true);
            return;
        }

        // Check no-block-explosion (block-based explosions only)
        if (isFlagActive("no-block-explosion", location)) {
            event.blockList().clear();
            event.setCancelled(true);
        }
    }

    // ---------------------------------------------------------------
    //  WeatherChangeEvent - weather flags
    // ---------------------------------------------------------------

    /**
     * Controls weather changes based on no-weather-change and force-weather
     * flags. Note that weather events are world-scoped, so the flag is
     * checked at the world scope.
     *
     * @param event the weather change event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWeatherChange(WeatherChangeEvent event) {
        // Weather events need a location for flag checking - use world spawn
        Location worldSpawn = event.getWorld().getSpawnLocation();
        if (worldSpawn.getWorld() == null) return;

        // Check force-weather flag
        String forcedWeather = flagManager.getFlagValue("force-weather", worldSpawn);
        if (forcedWeather != null && !forcedWeather.isEmpty()) {
            switch (forcedWeather.toUpperCase()) {
                case "CLEAR" -> {
                    // Cancel if trying to change to rain
                    if (event.toWeatherState()) {
                        event.setCancelled(true);
                    }
                }
                case "RAIN", "THUNDER" -> {
                    // Cancel if trying to change to clear
                    if (!event.toWeatherState()) {
                        event.setCancelled(true);
                    }
                }
            }
            return;
        }

        // Check no-weather-change - cancel if changing to rain
        if (event.toWeatherState() && isFlagActive("no-weather-change", worldSpawn)) {
            event.setCancelled(true);
        }
    }

    // ---------------------------------------------------------------
    //  ThunderChangeEvent - weather flags
    // ---------------------------------------------------------------

    /**
     * Controls thunder changes based on force-weather and no-weather-change
     * flags. Similar logic to weather change handling.
     *
     * @param event the thunder change event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onThunderChange(ThunderChangeEvent event) {
        Location worldSpawn = event.getWorld().getSpawnLocation();
        if (worldSpawn.getWorld() == null) return;

        // Check force-weather flag
        String forcedWeather = flagManager.getFlagValue("force-weather", worldSpawn);
        if (forcedWeather != null && !forcedWeather.isEmpty()) {
            switch (forcedWeather.toUpperCase()) {
                case "CLEAR", "RAIN" -> {
                    // Cancel if trying to enable thunder
                    if (event.toThunderState()) {
                        event.setCancelled(true);
                    }
                }
                case "THUNDER" -> {
                    // Cancel if trying to disable thunder
                    if (!event.toThunderState()) {
                        event.setCancelled(true);
                    }
                }
            }
            return;
        }

        // Check no-weather-change
        if (event.toThunderState() && isFlagActive("no-weather-change", worldSpawn)) {
            event.setCancelled(true);
        }
    }
}
