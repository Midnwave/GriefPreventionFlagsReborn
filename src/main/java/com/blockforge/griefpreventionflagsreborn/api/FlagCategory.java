package com.blockforge.griefpreventionflagsreborn.api;

import org.bukkit.Material;

/**
 * Categories used to group and organize flags in the GUI and documentation.
 * Each category has a display name, a representative icon material, and a
 * MiniMessage color tag for styled rendering.
 */
public enum FlagCategory {

    SAFETY("Safety", Material.SHIELD, "<green>"),
    PVP("PvP", Material.DIAMOND_SWORD, "<red>"),
    MOBS("Mobs", Material.ZOMBIE_HEAD, "<dark_green>"),
    ENVIRONMENT("Environment", Material.OAK_SAPLING, "<dark_aqua>"),
    MOVEMENT("Movement", Material.LEATHER_BOOTS, "<aqua>"),
    COMMANDS("Commands", Material.COMMAND_BLOCK, "<gold>"),
    ECONOMY("Economy", Material.GOLD_INGOT, "<yellow>"),
    TECHNICAL("Technical", Material.REDSTONE, "<dark_red>"),
    VEHICLE_ENTITY("Vehicles/Entities", Material.OAK_BOAT, "<blue>"),
    BLOCK_INTERACTION("Block Interaction", Material.CRAFTING_TABLE, "<light_purple>"),
    WORLD_MODIFICATION("World Modification", Material.TNT, "<dark_purple>"),
    CONTENT_121("1.21 Content", Material.MACE, "<white>");

    private final String displayName;
    private final Material icon;
    private final String colorTag;

    FlagCategory(String displayName, Material icon, String colorTag) {
        this.displayName = displayName;
        this.icon = icon;
        this.colorTag = colorTag;
    }

    /**
     * Returns the human-readable display name for this category.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the Material used as the icon for this category in GUIs.
     *
     * @return the icon material
     */
    public Material getIcon() {
        return icon;
    }

    /**
     * Returns the MiniMessage color tag used for styling this category.
     *
     * @return the MiniMessage color tag (e.g. "{@literal <green>}")
     */
    public String getColorTag() {
        return colorTag;
    }
}
