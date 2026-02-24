package com.blockforge.griefpreventionflagsreborn.flags.categories;

import com.blockforge.griefpreventionflagsreborn.api.FlagCategory;
import com.blockforge.griefpreventionflagsreborn.api.FlagDefinition;
import com.blockforge.griefpreventionflagsreborn.api.FlagScope;
import com.blockforge.griefpreventionflagsreborn.api.FlagType;
import com.blockforge.griefpreventionflagsreborn.flags.BooleanFlag;
import com.blockforge.griefpreventionflagsreborn.api.FlagRegistry;
import com.blockforge.griefpreventionflagsreborn.flags.MaterialListFlag;
import org.bukkit.Material;

import java.util.Collections;
import java.util.EnumSet;

/**
 * Registers all world modification flags that control block breaking,
 * block placing, protected materials, and other world-altering actions.
 */
public final class WorldModificationFlags {

    private WorldModificationFlags() {
        // Utility class
    }

    /**
     * Registers all world modification flags into the given registry.
     *
     * @param registry the flag registry to register flags into
     */
    public static void registerAll(FlagRegistry registry) {

        // 115. no-block-break
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-block-break")
                .displayName("No Block Break")
                .description("Prevents players from breaking blocks in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.WORLD_MODIFICATION)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.DIAMOND_PICKAXE)
                .adminOnly(false)
                .build()));

        // 116. no-block-place
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-block-place")
                .displayName("No Block Place")
                .description("Prevents players from placing blocks in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.WORLD_MODIFICATION)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.GRASS_BLOCK)
                .adminOnly(false)
                .build()));

        // 117. protected-blocks
        registry.registerFlag(new MaterialListFlag(FlagDefinition.builder()
                .id("protected-blocks")
                .displayName("Protected Blocks")
                .description("List of block types that cannot be broken in this area")
                .type(FlagType.MATERIAL_LIST)
                .category(FlagCategory.WORLD_MODIFICATION)
                .defaultValue(Collections.emptyList())
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.BEDROCK)
                .adminOnly(false)
                .build()));

        // 118. blocked-place-blocks
        registry.registerFlag(new MaterialListFlag(FlagDefinition.builder()
                .id("blocked-place-blocks")
                .displayName("Blocked Place Blocks")
                .description("List of block types that cannot be placed in this area")
                .type(FlagType.MATERIAL_LIST)
                .category(FlagCategory.WORLD_MODIFICATION)
                .defaultValue(Collections.emptyList())
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.BARRIER)
                .adminOnly(false)
                .build()));

        // 119. no-lighter-use
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-lighter-use")
                .displayName("No Lighter Use")
                .description("Prevents players from using flint and steel or fire charges in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.WORLD_MODIFICATION)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.FLINT_AND_STEEL)
                .adminOnly(false)
                .build()));

        // 120. no-bucket-use
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-bucket-use")
                .displayName("No Bucket Use")
                .description("Prevents players from using buckets in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.WORLD_MODIFICATION)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.BUCKET)
                .adminOnly(false)
                .build()));

        // 121. no-item-frame-break
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-item-frame-break")
                .displayName("No Item Frame Break")
                .description("Prevents players from breaking item frames in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.WORLD_MODIFICATION)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.ITEM_FRAME)
                .adminOnly(false)
                .build()));

        // 122. no-painting-break
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-painting-break")
                .displayName("No Painting Break")
                .description("Prevents players from breaking paintings in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.WORLD_MODIFICATION)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.PAINTING)
                .adminOnly(false)
                .build()));

        // 123. no-container-access
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-container-access")
                .displayName("No Container Access")
                .description("Prevents players from accessing all container types in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.WORLD_MODIFICATION)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.BARREL)
                .adminOnly(false)
                .build()));

        // 124. no-note-block-play
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-note-block-play")
                .displayName("No Note Block Play")
                .description("Prevents players from playing note blocks in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.WORLD_MODIFICATION)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.NOTE_BLOCK)
                .adminOnly(false)
                .build()));
    }
}
