package com.ecommerce.auth.entity;

/**
 * User Roles for Role-Based Access Control (RBAC).
 * Supports standard Spring Security authorities (ROLE_USER, ROLE_ADMIN, ROLE_SUPER_ADMIN)
 * and maps to database values (USER, ADMIN, SUPER_ADMIN).
 */
public enum Role {
    ROLE_USER("USER"),
    ROLE_ADMIN("ADMIN"),
    ROLE_SUPER_ADMIN("SUPER_ADMIN");

    private final String dbValue;

    Role(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static Role fromDbValue(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        String clean = dbValue.trim().toUpperCase();
        for (Role role : values()) {
            if (role.dbValue.equalsIgnoreCase(clean) || role.name().equalsIgnoreCase(clean)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role value: " + dbValue);
    }
}
