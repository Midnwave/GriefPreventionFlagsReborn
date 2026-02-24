package com.blockforge.griefpreventionflagsreborn.flags.categories;

import com.blockforge.griefpreventionflagsreborn.api.FlagCategory;
import com.blockforge.griefpreventionflagsreborn.api.FlagDefinition;
import com.blockforge.griefpreventionflagsreborn.api.FlagScope;
import com.blockforge.griefpreventionflagsreborn.api.FlagType;
import com.blockforge.griefpreventionflagsreborn.flags.BooleanFlag;
import com.blockforge.griefpreventionflagsreborn.api.FlagRegistry;
import com.blockforge.griefpreventionflagsreborn.flags.NumberFlag;
import com.blockforge.griefpreventionflagsreborn.flags.StringFlag;
import org.bukkit.Material;

import java.util.EnumSet;

/**
 * Registers all movement-related flags that control player movement,
 * teleportation, entry/exit behavior, and messages within claims.
 */
public final class MovementFlags {

    private MovementFlags() {
        // Utility class
    }

    /**
     * Registers all movement flags into the given registry.
     *
     * @param registry the flag registry to register flags into
     */
    public static void registerAll(FlagRegistry registry) {

        // 63. no-enter
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-enter")
                .displayName("No Enter")
                .description("Prevents players from entering this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.MOVEMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.BARRIER)
                .adminOnly(false)
                .build()));

        // 64. no-exit
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-exit")
                .displayName("No Exit")
                .description("Prevents players from leaving this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.MOVEMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.IRON_BARS)
                .adminOnly(false)
                .build()));

        // 65. no-flight
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-flight")
                .displayName("No Flight")
                .description("Prevents all forms of flight in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.MOVEMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.ELYTRA)
                .adminOnly(false)
                .build()));

        // 66. no-elytra
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-elytra")
                .displayName("No Elytra")
                .description("Prevents elytra gliding in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.MOVEMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.ELYTRA)
                .adminOnly(false)
                .build()));

        // 67. no-enderpearl
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-enderpearl")
                .displayName("No Ender Pearl")
                .description("Prevents ender pearl teleportation in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.MOVEMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.ENDER_PEARL)
                .adminOnly(false)
                .build()));

        // 68. no-chorus-fruit
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-chorus-fruit")
                .displayName("No Chorus Fruit")
                .description("Prevents chorus fruit teleportation in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.MOVEMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.CHORUS_FRUIT)
                .adminOnly(false)
                .build()));

        // 69. entry-message
        registry.registerFlag(new StringFlag(FlagDefinition.builder()
                .id("entry-message")
                .displayName("Entry Message")
                .description("Message displayed to players when they enter this area")
                .type(FlagType.STRING)
                .category(FlagCategory.MOVEMENT)
                .defaultValue("")
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.OAK_SIGN)
                .adminOnly(false)
                .build()));

        // 70. exit-message
        registry.registerFlag(new StringFlag(FlagDefinition.builder()
                .id("exit-message")
                .displayName("Exit Message")
                .description("Message displayed to players when they leave this area")
                .type(FlagType.STRING)
                .category(FlagCategory.MOVEMENT)
                .defaultValue("")
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.OAK_SIGN)
                .adminOnly(false)
                .build()));

        // 71. command-on-entry
        registry.registerFlag(new StringFlag(FlagDefinition.builder()
                .id("command-on-entry")
                .displayName("Command On Entry")
                .description("Command executed when a player enters this area")
                .type(FlagType.STRING)
                .category(FlagCategory.MOVEMENT)
                .defaultValue("")
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.COMMAND_BLOCK)
                .adminOnly(false)
                .build()));

        // 72. command-on-exit
        registry.registerFlag(new StringFlag(FlagDefinition.builder()
                .id("command-on-exit")
                .displayName("Command On Exit")
                .description("Command executed when a player leaves this area")
                .type(FlagType.STRING)
                .category(FlagCategory.MOVEMENT)
                .defaultValue("")
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.CHAIN_COMMAND_BLOCK)
                .adminOnly(false)
                .build()));

        // 73. no-riptide
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-riptide")
                .displayName("No Riptide")
                .description("Prevents use of riptide-enchanted tridents in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.MOVEMENT)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.TRIDENT)
                .adminOnly(false)
                .build()));

        // 74. walk-speed
        registry.registerFlag(new NumberFlag(FlagDefinition.builder()
                .id("walk-speed")
                .displayName("Walk Speed")
                .description("Custom walk speed for players in this area (-1 = default, 0.0-1.0 custom speed)")
                .type(FlagType.DOUBLE)
                .category(FlagCategory.MOVEMENT)
                .defaultValue(-1.0)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.LEATHER_BOOTS)
                .adminOnly(false)
                .build(), -1.0, 1.0));
    }
}
