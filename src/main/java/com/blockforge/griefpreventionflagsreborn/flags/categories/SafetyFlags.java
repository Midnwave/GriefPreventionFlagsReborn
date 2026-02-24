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
 * Registers all safety-related flags that protect players from various
 * damage sources and death penalties within claims.
 */
public final class SafetyFlags {

    private SafetyFlags() {
        // Utility class
    }

    /**
     * Registers all safety flags into the given registry.
     *
     * @param registry the flag registry to register flags into
     */
    public static void registerAll(FlagRegistry registry) {

        // 1. no-fall-damage
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-fall-damage")
                .displayName("No Fall Damage")
                .description("Prevents fall damage in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.SAFETY)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.FEATHER)
                .adminOnly(false)
                .build()));

        // 2. no-fire-damage
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-fire-damage")
                .displayName("No Fire Damage")
                .description("Prevents fire and lava damage in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.SAFETY)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.BLAZE_POWDER)
                .adminOnly(false)
                .build()));

        // 3. no-drowning
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-drowning")
                .displayName("No Drowning")
                .description("Prevents drowning damage in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.SAFETY)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.WATER_BUCKET)
                .adminOnly(false)
                .build()));

        // 4. no-explosion-damage
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-explosion-damage")
                .displayName("No Explosion Damage")
                .description("Prevents explosion damage to players in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.SAFETY)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.TNT)
                .adminOnly(false)
                .build()));

        // 5. no-void-damage
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-void-damage")
                .displayName("No Void Damage")
                .description("Prevents void damage in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.SAFETY)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.END_STONE)
                .adminOnly(false)
                .build()));

        // 6. no-suffocation-damage
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-suffocation-damage")
                .displayName("No Suffocation Damage")
                .description("Prevents suffocation damage from blocks in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.SAFETY)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.GRAVEL)
                .adminOnly(false)
                .build()));

        // 7. no-contact-damage
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-contact-damage")
                .displayName("No Contact Damage")
                .description("Prevents contact damage from blocks like cacti and berry bushes")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.SAFETY)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.CACTUS)
                .adminOnly(false)
                .build()));

        // 8. no-lightning-damage
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-lightning-damage")
                .displayName("No Lightning Damage")
                .description("Prevents lightning strike damage in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.SAFETY)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.LIGHTNING_ROD)
                .adminOnly(false)
                .build()));

        // 9. no-hunger
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-hunger")
                .displayName("No Hunger")
                .description("Prevents hunger depletion in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.SAFETY)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.COOKED_BEEF)
                .adminOnly(false)
                .build()));

        // 10. keep-inventory
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("keep-inventory")
                .displayName("Keep Inventory")
                .description("Players keep their inventory on death in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.SAFETY)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.CHEST)
                .adminOnly(false)
                .build()));

        // 11. keep-level
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("keep-level")
                .displayName("Keep Level")
                .description("Players keep their experience levels on death in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.SAFETY)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.EXPERIENCE_BOTTLE)
                .adminOnly(false)
                .build()));

        // 12. no-player-drops
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-player-drops")
                .displayName("No Player Drops")
                .description("Prevents players from dropping items on death in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.SAFETY)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.DROPPER)
                .adminOnly(false)
                .build()));

        // 13. no-poison-damage
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-poison-damage")
                .displayName("No Poison Damage")
                .description("Prevents poison effect damage in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.SAFETY)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.SPIDER_EYE)
                .adminOnly(false)
                .build()));

        // 14. no-wither-effect-damage
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-wither-effect-damage")
                .displayName("No Wither Effect Damage")
                .description("Prevents wither effect damage in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.SAFETY)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.WITHER_SKELETON_SKULL)
                .adminOnly(false)
                .build()));
    }
}
