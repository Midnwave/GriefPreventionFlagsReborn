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
 * Registers all vehicle and entity interaction flags that control
 * vehicle placement, animal breeding, leash use, and entity manipulation.
 */
public final class VehicleEntityFlags {

    private VehicleEntityFlags() {
        // Utility class
    }

    /**
     * Registers all vehicle and entity flags into the given registry.
     *
     * @param registry the flag registry to register flags into
     */
    public static void registerAll(FlagRegistry registry) {

        // 95. no-vehicle-place
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-vehicle-place")
                .displayName("No Vehicle Place")
                .description("Prevents placing vehicles (boats, minecarts) in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.VEHICLE_ENTITY)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.OAK_BOAT)
                .adminOnly(false)
                .build()));

        // 96. no-vehicle-destroy
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-vehicle-destroy")
                .displayName("No Vehicle Destroy")
                .description("Prevents destroying vehicles (boats, minecarts) in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.VEHICLE_ENTITY)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.OAK_BOAT)
                .adminOnly(false)
                .build()));

        // 97. no-vehicle-enter
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-vehicle-enter")
                .displayName("No Vehicle Enter")
                .description("Prevents players from entering vehicles in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.VEHICLE_ENTITY)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.MINECART)
                .adminOnly(false)
                .build()));

        // 98. no-animal-breed
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-animal-breed")
                .displayName("No Animal Breed")
                .description("Prevents players from breeding animals in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.VEHICLE_ENTITY)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.WHEAT)
                .adminOnly(false)
                .build()));

        // 99. no-animal-tame
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-animal-tame")
                .displayName("No Animal Tame")
                .description("Prevents players from taming animals in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.VEHICLE_ENTITY)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.BONE)
                .adminOnly(false)
                .build()));

        // 100. no-lead-use
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-lead-use")
                .displayName("No Lead Use")
                .description("Prevents players from using leads on entities in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.VEHICLE_ENTITY)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.LEAD)
                .adminOnly(false)
                .build()));

        // 101. no-item-frame-rotate
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-item-frame-rotate")
                .displayName("No Item Frame Rotate")
                .description("Prevents players from rotating items in item frames in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.VEHICLE_ENTITY)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.ITEM_FRAME)
                .adminOnly(false)
                .build()));

        // 102. no-armor-stand-manipulate
        registry.registerFlag(new BooleanFlag(FlagDefinition.builder()
                .id("no-armor-stand-manipulate")
                .displayName("No Armor Stand Manipulate")
                .description("Prevents players from manipulating armor stands in this area")
                .type(FlagType.BOOLEAN)
                .category(FlagCategory.VEHICLE_ENTITY)
                .defaultValue(false)
                .allowedScopes(EnumSet.allOf(FlagScope.class))
                .guiIcon(Material.ARMOR_STAND)
                .adminOnly(false)
                .build()));
    }
}
