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
 * Registers all flags related to Minecraft 1.21 content, including
 * mace, trial chambers, vault blocks, wind charges, and copper mechanics.
 */
public final class ContentFlags121 {

    private ContentFlags121() {
        // Utility class
    }

    /**
     * Registers all 1.21 content flags into the given registry.
     *
     * @param registry the flag registry to register flags into
     */
    public static void registerAll(FlagRegistry registry) {

        // 125. no-mace-use
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-mace-use")
                .displayName("No Mace Use")
                .description("Prevents players from using the mace weapon in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.CONTENT_121)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.MACE)
                .adminOnly(false)
                .build()));

        // 126. no-trial-spawner
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-trial-spawner")
                .displayName("No Trial Spawner")
                .description("Prevents trial spawners from activating in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.CONTENT_121)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.TRIAL_SPAWNER)
                .adminOnly(false)
                .build()));

        // 127. no-vault-access
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-vault-access")
                .displayName("No Vault Access")
                .description("Prevents players from accessing vault blocks in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.CONTENT_121)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.VAULT)
                .adminOnly(false)
                .build()));

        // 128. no-wind-charge
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-wind-charge")
                .displayName("No Wind Charge")
                .description("Prevents players from using wind charges in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.CONTENT_121)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.WIND_CHARGE)
                .adminOnly(false)
                .build()));

        // 129. no-breeze-spawning
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-breeze-spawning")
                .displayName("No Breeze Spawning")
                .description("Prevents breeze mobs from spawning in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.CONTENT_121)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.BREEZE_ROD)
                .adminOnly(false)
                .build()));

        // 130. no-copper-oxidation
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-copper-oxidation")
                .displayName("No Copper Oxidation")
                .description("Prevents copper blocks from oxidizing in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.CONTENT_121)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.COPPER_BLOCK)
                .adminOnly(false)
                .build()));
    }
}
