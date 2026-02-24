package com.blockforge.griefpreventionflagsreborn.flags.categories;

import com.blockforge.griefpreventionflagsreborn.api.FlagCategory;
import com.blockforge.griefpreventionflagsreborn.api.FlagDefinition;
import com.blockforge.griefpreventionflagsreborn.api.FlagScope;
import com.blockforge.griefpreventionflagsreborn.api.FlagType;
import com.blockforge.griefpreventionflagsreborn.flags.BooleanFlag;
import com.blockforge.griefpreventionflagsreborn.api.FlagRegistry;
import com.blockforge.griefpreventionflagsreborn.flags.NumberFlag;
import org.bukkit.Material;

import java.util.EnumSet;

/**
 * Registers all economy-related flags that control job taxes, item/XP drops,
 * skill boosts, and villager trading within claims.
 */
public final class EconomyFlags {

    private EconomyFlags() {
        // Utility class
    }

    /**
     * Registers all economy flags into the given registry.
     *
     * @param registry the flag registry to register flags into
     */
    public static void registerAll(FlagRegistry registry) {

        // 79. job-tax-enabled
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("job-tax-enabled")
                .displayName("Job Tax Enabled")
                .description("Enables the HorizonUtilities job tax system in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.ECONOMY)
                .defaultValue(true)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.GOLD_NUGGET)
                .adminOnly(false)
                .build()));

        // 80. job-tax-rate
        registry.registerFlag(new NumberFlag(FlagDefinition.builder()
                .id("job-tax-rate")
                .displayName("Job Tax Rate")
                .description("Tax rate for job income (0.0-1.0)")
                .type(FlagType.DOUBLE)
                .category(FlagCategory.ECONOMY)
                .defaultValue(0.10)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.GOLD_INGOT)
                .adminOnly(false)
                .build(), 0.0, 1.0));

        // 81. no-item-drops
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-item-drops")
                .displayName("No Item Drops")
                .description("Prevents items from being dropped by blocks and entities in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.ECONOMY)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.HOPPER)
                .adminOnly(false)
                .build()));

        // 82. no-xp-drops
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-xp-drops")
                .displayName("No XP Drops")
                .description("Prevents experience orbs from dropping in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.ECONOMY)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.EXPERIENCE_BOTTLE)
                .adminOnly(false)
                .build()));

        // 83. skill-boost-zone
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("skill-boost-zone")
                .displayName("Skill Boost Zone")
                .description("Enables AuraSkills experience boost in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.ECONOMY)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.ENCHANTING_TABLE)
                .adminOnly(true)
                .build()));

        // 84. no-villager-trade
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-villager-trade")
                .displayName("No Villager Trade")
                .description("Prevents players from trading with villagers in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.ECONOMY)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.EMERALD)
                .adminOnly(false)
                .build()));
    }
}
