package com.blockforge.griefpreventionflagsreborn.flags.categories;

import com.blockforge.griefpreventionflagsreborn.api.FlagCategory;
import com.blockforge.griefpreventionflagsreborn.api.FlagDefinition;
import com.blockforge.griefpreventionflagsreborn.api.FlagScope;
import com.blockforge.griefpreventionflagsreborn.api.FlagType;
import com.blockforge.griefpreventionflagsreborn.flags.BooleanFlag;
import com.blockforge.griefpreventionflagsreborn.api.FlagRegistry;
import com.blockforge.griefpreventionflagsreborn.flags.StringFlag;
import org.bukkit.Material;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Registers all environment-related flags that control natural world
 * processes such as fire spread, growth, weather, and block changes.
 */
public final class EnvironmentFlags {

    private EnvironmentFlags() {
        // Utility class
    }

    /**
     * Registers all environment flags into the given registry.
     *
     * @param registry the flag registry to register flags into
     */
    public static void registerAll(FlagRegistry registry) {

        // 41. no-fire-spread
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-fire-spread")
                .displayName("No Fire Spread")
                .description("Prevents fire from spreading to other blocks in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.ENVIRONMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.CAMPFIRE)
                .adminOnly(false)
                .build()));

        // 42. no-vine-growth
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-vine-growth")
                .displayName("No Vine Growth")
                .description("Prevents vine growth and spreading in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.ENVIRONMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.VINE)
                .adminOnly(false)
                .build()));

        // 43. no-mushroom-spread
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-mushroom-spread")
                .displayName("No Mushroom Spread")
                .description("Prevents mushroom spreading in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.ENVIRONMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.RED_MUSHROOM)
                .adminOnly(false)
                .build()));

        // 44. no-sculk-spread
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-sculk-spread")
                .displayName("No Sculk Spread")
                .description("Prevents sculk from spreading in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.ENVIRONMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.SCULK)
                .adminOnly(false)
                .build()));

        // 45. no-liquid-flow
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-liquid-flow")
                .displayName("No Liquid Flow")
                .description("Prevents all liquid (water and lava) from flowing in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.ENVIRONMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.BUCKET)
                .adminOnly(false)
                .build()));

        // 46. no-water-flow
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-water-flow")
                .displayName("No Water Flow")
                .description("Prevents water from flowing in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.ENVIRONMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.WATER_BUCKET)
                .adminOnly(false)
                .build()));

        // 47. no-lava-flow
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-lava-flow")
                .displayName("No Lava Flow")
                .description("Prevents lava from flowing in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.ENVIRONMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.LAVA_BUCKET)
                .adminOnly(false)
                .build()));

        // 48. no-leaf-decay
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-leaf-decay")
                .displayName("No Leaf Decay")
                .description("Prevents leaves from decaying naturally in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.ENVIRONMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.OAK_LEAVES)
                .adminOnly(false)
                .build()));

        // 49. no-crop-growth
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-crop-growth")
                .displayName("No Crop Growth")
                .description("Prevents crops from growing in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.ENVIRONMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.WHEAT_SEEDS)
                .adminOnly(false)
                .build()));

        // 50. no-tree-growth
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-tree-growth")
                .displayName("No Tree Growth")
                .description("Prevents saplings from growing into trees in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.ENVIRONMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.OAK_SAPLING)
                .adminOnly(false)
                .build()));

        // 51. no-snow-form
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-snow-form")
                .displayName("No Snow Form")
                .description("Prevents snow from forming on blocks in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.ENVIRONMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.SNOW_BLOCK)
                .adminOnly(false)
                .build()));

        // 52. no-ice-form
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-ice-form")
                .displayName("No Ice Form")
                .description("Prevents ice from forming on water in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.ENVIRONMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.ICE)
                .adminOnly(false)
                .build()));

        // 53. no-snow-melt
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-snow-melt")
                .displayName("No Snow Melt")
                .description("Prevents snow from melting in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.ENVIRONMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.SNOW)
                .adminOnly(false)
                .build()));

        // 54. no-ice-melt
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-ice-melt")
                .displayName("No Ice Melt")
                .description("Prevents ice from melting in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.ENVIRONMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.PACKED_ICE)
                .adminOnly(false)
                .build()));

        // 55. no-coral-death
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-coral-death")
                .displayName("No Coral Death")
                .description("Prevents coral from dying when out of water in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.ENVIRONMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.BRAIN_CORAL)
                .adminOnly(false)
                .build()));

        // 56. no-explosions
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-explosions")
                .displayName("No Explosions")
                .description("Prevents all explosions from destroying blocks in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.ENVIRONMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.TNT)
                .adminOnly(false)
                .build()));

        // 57. no-block-explosion
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-block-explosion")
                .displayName("No Block Explosion")
                .description("Prevents block-based explosions (TNT, beds, respawn anchors) in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.ENVIRONMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.TNT_MINECART)
                .adminOnly(false)
                .build()));

        // 58. force-weather
        Set<String> weatherValues = new LinkedHashSet<>();
        weatherValues.add("CLEAR");
        weatherValues.add("RAIN");
        weatherValues.add("THUNDER");
        registry.registerFlag(new StringFlag(FlagDefinition.builder()
                .id("force-weather")
                .displayName("Force Weather")
                .description("Forces a specific weather state in this area (CLEAR, RAIN, or THUNDER)")
                .type(FlagType.STRING)
                .category(FlagCategory.ENVIRONMENT)
                .defaultValue("")
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .allowedEnumValues(weatherValues)
                .guiIcon(Material.SUNFLOWER)
                .adminOnly(false)
                .build()));

        // 59. no-weather-change
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-weather-change")
                .displayName("No Weather Change")
                .description("Prevents weather from changing in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.ENVIRONMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.CLOCK)
                .adminOnly(false)
                .build()));

        // 60. no-grass-spread
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-grass-spread")
                .displayName("No Grass Spread")
                .description("Prevents grass from spreading to dirt blocks in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.ENVIRONMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.GRASS_BLOCK)
                .adminOnly(false)
                .build()));

        // 61. no-kelp-growth
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-kelp-growth")
                .displayName("No Kelp Growth")
                .description("Prevents kelp from growing in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.ENVIRONMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.KELP)
                .adminOnly(false)
                .build()));

        // 62. no-dripstone-growth
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-dripstone-growth")
                .displayName("No Dripstone Growth")
                .description("Prevents pointed dripstone from growing in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.ENVIRONMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.POINTED_DRIPSTONE)
                .adminOnly(false)
                .build()));
    }
}
