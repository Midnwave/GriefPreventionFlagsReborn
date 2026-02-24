package com.blockforge.griefpreventionflagsreborn.api;

import java.util.List;

/**
 * Defines the data types that a flag value can hold.
 * Each type maps to a Java class used for runtime type checking and serialization.
 */
public enum FlagType {

    BOOLEAN(Boolean.class),
    INTEGER(Integer.class),
    DOUBLE(Double.class),
    STRING(String.class),
    STRING_LIST(List.class),
    MATERIAL_LIST(List.class),
    ENTITY_TYPE_LIST(List.class);

    private final Class<?> javaType;

    FlagType(Class<?> javaType) {
        this.javaType = javaType;
    }

    /**
     * Returns the Java class that this flag type maps to.
     *
     * @return the Java class representing this type
     */
    public Class<?> getJavaType() {
        return javaType;
    }
}
