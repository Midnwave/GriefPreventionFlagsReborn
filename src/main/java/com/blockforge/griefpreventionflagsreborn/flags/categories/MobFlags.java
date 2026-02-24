package com.blockforge.griefpreventionflagsreborn.flags.categories;

import com.blockforge.griefpreventionflagsreborn.api.FlagCategory;
import com.blockforge.griefpreventionflagsreborn.api.FlagDefinition;
import com.blockforge.griefpreventionflagsreborn.api.FlagScope;
import com.blockforge.griefpreventionflagsreborn.api.FlagType;
import com.blockforge.griefpreventionflagsreborn.flags.BooleanFlag;
import com.blockforge.griefpreventionflagsreborn.flags.EntityTypeListFlag;
import com.blockforge.griefpreventionflagsreborn.api.FlagRegistry;
import com.blockforge.griefpreventionflagsreborn.flags.NumberFlag;
import org.bukkit.Material;

import java.util.Collections;
import java.util.EnumSet;

/**
 * Registers all mob-related flags that control mob spawning, behavior,
 * damage, and griefing within claims.
 */
public final class MobFlags {

    private MobFlags() {
        // Utility class
    }

    /**
     * Registers all mob flags into the given registry.
     *
     * @param registry the flag registry to register flags into
     */
    public static void registerAll(FlagRegistry registry) {

        // 23. no-mob-spawning
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-mob-spawning")
                .displayName("No Mob Spawning")
                .description("Prevents all mob spawning in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.MOBS)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.SPAWNER)
                .adminOnly(false)
                .build()));

        // 24. no-monster-spawning
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-monster-spawning")
                .displayName("No Monster Spawning")
                .description("Prevents hostile monster spawning in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.MOBS)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.ZOMBIE_HEAD)
                .adminOnly(false)
                .build()));

        // 25. no-animal-spawning
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-animal-spawning")
                .displayName("No Animal Spawning")
                .description("Prevents passive animal spawning in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.MOBS)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.PIG_SPAWN_EGG)
                .adminOnly(false)
                .build()));

        // 26. no-phantom-spawning
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-phantom-spawning")
                .displayName("No Phantom Spawning")
                .description("Prevents phantom spawning in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.MOBS)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.PHANTOM_MEMBRANE)
                .adminOnly(false)
                .build()));

        // 27. no-slime-spawning
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-slime-spawning")
                .displayName("No Slime Spawning")
                .description("Prevents slime spawning in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.MOBS)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.SLIME_BALL)
                .adminOnly(false)
                .build()));

        // 28. no-warden-spawning
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-warden-spawning")
                .displayName("No Warden Spawning")
                .description("Prevents warden spawning in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.MOBS)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.SCULK_SHRIEKER)
                .adminOnly(false)
                .build()));

        // 29. allowed-mobs
        registry.registerFlag(new EntityTypeListFlag(FlagDefinition.builder()
                .id("allowed-mobs")
                .displayName("Allowed Mobs")
                .description("Only these mob types are allowed to spawn in this area")
                .type(FlagType.ENTITY_TYPE_LIST)
                .category(FlagCategory.MOBS)
                .defaultValue(Collections.emptyList())
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.NAME_TAG)
                .adminOnly(false)
                .build()));

        // 30. blocked-mobs
        registry.registerFlag(new EntityTypeListFlag(FlagDefinition.builder()
                .id("blocked-mobs")
                .displayName("Blocked Mobs")
                .description("These mob types are prevented from spawning in this area")
                .type(FlagType.ENTITY_TYPE_LIST)
                .category(FlagCategory.MOBS)
                .defaultValue(Collections.emptyList())
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.BARRIER)
                .adminOnly(false)
                .build()));

        // 31. no-monster-damage
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-monster-damage")
                .displayName("No Monster Damage")
                .description("Prevents hostile monsters from damaging players in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.MOBS)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.IRON_CHESTPLATE)
                .adminOnly(false)
                .build()));

        // 32. no-mob-damage
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-mob-damage")
                .displayName("No Mob Damage")
                .description("Prevents all mobs from damaging players in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.MOBS)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.DIAMOND_CHESTPLATE)
                .adminOnly(false)
                .build()));

        // 33. no-monster-target
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-monster-target")
                .displayName("No Monster Target")
                .description("Prevents hostile monsters from targeting players in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.MOBS)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.ENDER_EYE)
                .adminOnly(false)
                .build()));

        // 34. no-enderman-grief
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-enderman-grief")
                .displayName("No Enderman Grief")
                .description("Prevents endermen from picking up or placing blocks in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.MOBS)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.ENDER_PEARL)
                .adminOnly(false)
                .build()));

        // 35. no-creeper-explosion
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-creeper-explosion")
                .displayName("No Creeper Explosion")
                .description("Prevents creeper explosions from destroying blocks in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.MOBS)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.CREEPER_HEAD)
                .adminOnly(false)
                .build()));

        // 36. no-wither-damage
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-wither-damage")
                .displayName("No Wither Damage")
                .description("Prevents the Wither boss from destroying blocks in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.MOBS)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.NETHER_STAR)
                .adminOnly(false)
                .build()));

        // 37. no-zombie-break-doors
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-zombie-break-doors")
                .displayName("No Zombie Break Doors")
                .description("Prevents zombies from breaking doors in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.MOBS)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.OAK_DOOR)
                .adminOnly(false)
                .build()));

        // 38. mob-damage-multiplier
        registry.registerFlag(new NumberFlag(FlagDefinition.builder()
                .id("mob-damage-multiplier")
                .displayName("Mob Damage Multiplier")
                .description("Multiplier for damage dealt by mobs (0.0 = no damage, 10.0 = 10x damage)")
                .type(FlagType.DOUBLE)
                .category(FlagCategory.MOBS)
                .defaultValue(1.0)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.IRON_AXE)
                .adminOnly(false)
                .build(), 0.0, 10.0));

        // 39. no-spawner-mobs
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-spawner-mobs")
                .displayName("No Spawner Mobs")
                .description("Prevents mobs from spawning via mob spawners in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.MOBS)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.SPAWNER)
                .adminOnly(false)
                .build()));

        // 40. no-mob-grief
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-mob-grief")
                .displayName("No Mob Grief")
                .description("Prevents all mob griefing (block destruction, trampling, etc.) in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.MOBS)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.DIRT)
                .adminOnly(false)
                .build()));
    }
}
