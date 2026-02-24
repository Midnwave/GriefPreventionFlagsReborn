package com.blockforge.griefpreventionflagsreborn.api;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Defines the scope at which a flag can be set.
 * Higher priority scopes override lower priority ones during value resolution.
 * Resolution order: SERVER (lowest) -> WORLD -> CLAIM -> SUBCLAIM (highest).
 */
public enum FlagScope {

    SERVER(0),
    WORLD(1),
    CLAIM(2),
    SUBCLAIM(3);

    private final int priority;

    FlagScope(int priority) {
        this.priority = priority;
    }

    /**
     * Returns the priority of this scope. Higher values override lower values.
     *
     * @return the numeric priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Returns all scopes ordered from lowest priority to highest priority.
     *
     * @return an unmodifiable list of scopes sorted by ascending priority
     */
    public static List<FlagScope> getOrderedByPriority() {
        return Arrays.stream(values())
                .sorted(Comparator.comparingInt(FlagScope::getPriority))
                .toList();
    }

    /**
     * Returns all scopes ordered from highest priority to lowest priority.
     *
     * @return an unmodifiable list of scopes sorted by descending priority
     */
    public static List<FlagScope> getOrderedByPriorityDescending() {
        return Arrays.stream(values())
                .sorted(Comparator.comparingInt(FlagScope::getPriority).reversed())
                .toList();
    }
}
