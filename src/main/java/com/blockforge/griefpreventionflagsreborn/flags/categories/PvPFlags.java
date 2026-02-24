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

/**
 * Registers all PvP-related flags that control player-versus-player
 * combat mechanics within claims.
 */
public final class PvPFlags {

    private PvPFlags() {
        // Utility class
    }

    /**
     * Registers all PvP flags into the given registry.
     *
     * @param registry the flag registry to register flags into
     */
    public static void registerAll(FlagRegistry registry) {

        // 15. no-pvp
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-pvp")
                .displayName("No PvP")
                .description("Disables all player-versus-player combat in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.PVP)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.IRON_SWORD)
                .adminOnly(false)
                .build()));

        // 16. no-player-damage
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-player-damage")
                .displayName("No Player Damage")
                .description("Prevents all damage dealt to players by other players")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.PVP)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.SHIELD)
                .adminOnly(false)
                .build()));

        // 17. no-potion-pvp
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-potion-pvp")
                .displayName("No Potion PvP")
                .description("Prevents players from using harmful potions on other players")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.PVP)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.SPLASH_POTION)
                .adminOnly(false)
                .build()));

        // 18. no-end-crystal-pvp
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-end-crystal-pvp")
                .displayName("No End Crystal PvP")
                .description("Prevents players from using end crystals to damage other players")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.PVP)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.END_CRYSTAL)
                .adminOnly(false)
                .build()));

        // 19. no-projectile-pvp
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-projectile-pvp")
                .displayName("No Projectile PvP")
                .description("Prevents players from using projectiles to damage other players")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.PVP)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.BOW)
                .adminOnly(false)
                .build()));

        // 20. bounty-hunting-allowed
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("bounty-hunting-allowed")
                .displayName("Bounty Hunting Allowed")
                .description("Allows the HorizonUtilities bounty system to function in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.PVP)
                .defaultValue(true)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.DIAMOND_SWORD)
                .adminOnly(false)
                .build()));

        // 21. pvp-deny-message
        registry.registerFlag(new StringFlag(FlagDefinition.builder()
                .id("pvp-deny-message")
                .displayName("PvP Deny Message")
                .description("Custom message shown when PvP is denied in this area")
                .type(FlagType.STRING)
                .category(FlagCategory.PVP)
                .defaultValue("")
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.OAK_SIGN)
                .adminOnly(false)
                .build()));

        // 22. forced-pvp
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("forced-pvp")
                .displayName("Forced PvP")
                .description("Forces PvP to be enabled in this area regardless of other settings")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.PVP)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.NETHERITE_SWORD)
                .adminOnly(true)
                .build()));
    }
}
