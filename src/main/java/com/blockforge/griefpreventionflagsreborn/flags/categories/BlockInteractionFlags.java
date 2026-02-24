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
 * Registers all block interaction flags that control player access
 * to containers, doors, buttons, levers, and other interactive blocks.
 */
public final class BlockInteractionFlags {

    private BlockInteractionFlags() {
        // Utility class
    }

    /**
     * Registers all block interaction flags into the given registry.
     *
     * @param registry the flag registry to register flags into
     */
    public static void registerAll(FlagRegistry registry) {

        // 103. no-chest-access
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-chest-access")
                .displayName("No Chest Access")
                .description("Prevents players from opening chests in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.BLOCK_INTERACTION)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.CHEST)
                .adminOnly(false)
                .build()));

        // 104. no-door-access
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-door-access")
                .displayName("No Door Access")
                .description("Prevents players from opening doors in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.BLOCK_INTERACTION)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.OAK_DOOR)
                .adminOnly(false)
                .build()));

        // 105. no-button-access
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-button-access")
                .displayName("No Button Access")
                .description("Prevents players from pressing buttons in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.BLOCK_INTERACTION)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.STONE_BUTTON)
                .adminOnly(false)
                .build()));

        // 106. no-lever-access
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-lever-access")
                .displayName("No Lever Access")
                .description("Prevents players from using levers in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.BLOCK_INTERACTION)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.LEVER)
                .adminOnly(false)
                .build()));

        // 107. no-trapdoor-access
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-trapdoor-access")
                .displayName("No Trapdoor Access")
                .description("Prevents players from opening trapdoors in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.BLOCK_INTERACTION)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.OAK_TRAPDOOR)
                .adminOnly(false)
                .build()));

        // 108. no-fence-gate-access
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-fence-gate-access")
                .displayName("No Fence Gate Access")
                .description("Prevents players from opening fence gates in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.BLOCK_INTERACTION)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.OAK_FENCE_GATE)
                .adminOnly(false)
                .build()));

        // 109. no-anvil-access
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-anvil-access")
                .displayName("No Anvil Access")
                .description("Prevents players from using anvils in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.BLOCK_INTERACTION)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.ANVIL)
                .adminOnly(false)
                .build()));

        // 110. no-enchanting-table
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-enchanting-table")
                .displayName("No Enchanting Table")
                .description("Prevents players from using enchanting tables in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.BLOCK_INTERACTION)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.ENCHANTING_TABLE)
                .adminOnly(false)
                .build()));

        // 111. no-brewing-stand-access
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-brewing-stand-access")
                .displayName("No Brewing Stand Access")
                .description("Prevents players from using brewing stands in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.BLOCK_INTERACTION)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.BREWING_STAND)
                .adminOnly(false)
                .build()));

        // 112. no-beacon-access
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-beacon-access")
                .displayName("No Beacon Access")
                .description("Prevents players from accessing beacons in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.BLOCK_INTERACTION)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.BEACON)
                .adminOnly(false)
                .build()));

        // 113. no-ender-chest-access
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-ender-chest-access")
                .displayName("No Ender Chest Access")
                .description("Prevents players from opening ender chests in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.BLOCK_INTERACTION)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.ENDER_CHEST)
                .adminOnly(false)
                .build()));

        // 114. no-sign-edit
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-sign-edit")
                .displayName("No Sign Edit")
                .description("Prevents players from editing signs in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.BLOCK_INTERACTION)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.OAK_SIGN)
                .adminOnly(false)
                .build()));
    }
}
