package com.foodbridge.user;

/**
 * Platform roles. Stored as a string in the database and embedded in the JWT
 * as the {@code role} claim. Spring Security authorities are derived as
 * {@code ROLE_<name>}.
 */
public enum Role {
    DONOR,
    NGO,
    ADMIN
}
