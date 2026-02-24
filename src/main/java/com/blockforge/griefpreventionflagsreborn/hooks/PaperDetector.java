package com.blockforge.griefpreventionflagsreborn.hooks;

/**
 * Utility class that detects at runtime whether the server is running on Paper
 * (or a Paper fork such as Purpur) versus vanilla Spigot.
 * <p>
 * Detection is performed once during class loading by checking for the existence
 * of a Paper-specific class. The result is cached in a static final field for
 * zero-overhead repeated access.
 */
public final class PaperDetector {

    private static final boolean IS_PAPER;

    static {
        boolean paper = false;
        try {
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
            paper = true;
        } catch (ClassNotFoundException ignored) {
            // Not running on Paper
        }
        IS_PAPER = paper;
    }

    private PaperDetector() {
        // Utility class - no instantiation
    }

    /**
     * Returns whether the server is running on Paper (or a Paper fork).
     *
     * @return true if Paper's native Adventure API is available
     */
    public static boolean isPaper() {
        return IS_PAPER;
    }
}
