package com.blockforge.griefpreventionflagsreborn.flags.categories;

import com.blockforge.griefpreventionflagsreborn.api.FlagCategory;
import com.blockforge.griefpreventionflagsreborn.api.FlagDefinition;
import com.blockforge.griefpreventionflagsreborn.api.FlagScope;
import com.blockforge.griefpreventionflagsreborn.api.FlagType;
import com.blockforge.griefpreventionflagsreborn.flags.BooleanFlag;
import com.blockforge.griefpreventionflagsreborn.api.FlagRegistry;
import org.bukkit.Material;

import java.util.EnumSet;

/**
 * Registers all technical flags that control redstone mechanics,
 * pistons, dispensers, hoppers, and other technical block behaviors.
 */
public final class TechnicalFlags {

    private TechnicalFlags() {
        // Utility class
    }

    /**
     * Registers all technical flags into the given registry.
     *
     * @param registry the flag registry to register flags into
     */
    public static void registerAll(FlagRegistry registry) {

        // 85. no-redstone
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-redstone")
                .displayName("No Redstone")
                .description("Prevents all redstone components from activating in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.TECHNICAL)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.REDSTONE)
                .adminOnly(false)
                .build()));

        // 86. no-piston
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-piston")
                .displayName("No Piston")
                .description("Prevents pistons and sticky pistons from activating in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.TECHNICAL)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.PISTON)
                .adminOnly(false)
                .build()));

        // 87. no-dispenser
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-dispenser")
                .displayName("No Dispenser")
                .description("Prevents dispensers from dispensing items in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.TECHNICAL)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.DISPENSER)
                .adminOnly(false)
                .build()));

        // 88. no-hopper
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-hopper")
                .displayName("No Hopper")
                .description("Prevents hoppers from transferring items in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.TECHNICAL)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.HOPPER)
                .adminOnly(false)
                .build()));

        // 89. no-dropper
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-dropper")
                .displayName("No Dropper")
                .description("Prevents droppers from dropping items in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.TECHNICAL)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.DROPPER)
                .adminOnly(false)
                .build()));

        // 90. no-observer
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-observer")
                .displayName("No Observer")
                .description("Prevents observers from detecting block changes in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.TECHNICAL)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.OBSERVER)
                .adminOnly(false)
                .build()));

        // 91. no-tnt-prime
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-tnt-prime")
                .displayName("No TNT Prime")
                .description("Prevents TNT from being ignited in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.TECHNICAL)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.TNT)
                .adminOnly(false)
                .build()));

        // 92. no-sculk-sensor
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-sculk-sensor")
                .displayName("No Sculk Sensor")
                .description("Prevents sculk sensors from detecting vibrations in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.TECHNICAL)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.SCULK_SENSOR)
                .adminOnly(false)
                .build()));

        // 93. no-target-block
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-target-block")
                .displayName("No Target Block")
                .description("Prevents target blocks from emitting redstone signals in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.TECHNICAL)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.TARGET)
                .adminOnly(false)
                .build()));

        // 94. no-daylight-detector
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-daylight-detector")
                .displayName("No Daylight Detector")
                .description("Prevents daylight detectors from emitting redstone signals in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.TECHNICAL)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.DAYLIGHT_DETECTOR)
                .adminOnly(false)
                .build()));
    }
}
