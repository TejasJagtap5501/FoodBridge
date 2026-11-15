package com.foodbridge.user;

public enum UserStatus {
    /** Registered but not yet permitted to operate (NGO awaiting verification). */
    PENDING,
    /** Fully usable account. */
    ACTIVE,
    /** Administratively disabled - cannot authenticate. */
    INACTIVE
}
