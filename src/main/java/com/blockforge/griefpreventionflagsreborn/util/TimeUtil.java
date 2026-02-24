package com.blockforge.griefpreventionflagsreborn.util;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for formatting durations and timestamps into human-readable strings.
 * <p>
 * Mirrors the formatting patterns used in the HorizonUtilities plugin for consistency
 * across the BlockForge plugin ecosystem.
 */
public final class TimeUtil {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TimeUtil() {
        // Utility class - no instantiation
    }

    /**
     * Formats a duration in milliseconds into a human-readable string.
     * <p>
     * Examples:
     * <ul>
     *   <li>5000 -> "5s"</li>
     *   <li>150000 -> "2m 30s"</li>
     *   <li>9015000 -> "2h 30m 15s"</li>
     *   <li>90015000 -> "1d 1h 0m 15s"</li>
     * </ul>
     *
     * @param millis the duration in milliseconds
     * @return a formatted duration string
     */
    @NotNull
    public static String formatDuration(long millis) {
        if (millis < 0) {
            return "expired";
        }

        long totalSeconds = millis / 1000;

        if (totalSeconds < 60) {
            return totalSeconds + "s";
        }

        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0 || days > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            sb.append(minutes).append("m ");
        }
        sb.append(seconds).append("s");

        return sb.toString().trim();
    }

    /**
     * Formats an epoch timestamp in milliseconds into a human-readable date/time string.
     * Uses the server's default timezone.
     * <p>
     * Example: 1740400200000 -> "2025-02-24 14:30:00"
     *
     * @param epochMillis the epoch timestamp in milliseconds
     * @return a formatted timestamp string in "yyyy-MM-dd HH:mm:ss" format
     */
    @NotNull
    public static String formatTimestamp(long epochMillis) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(epochMillis),
                ZoneId.systemDefault()
        );
        return TIMESTAMP_FORMAT.format(dateTime);
    }
}
