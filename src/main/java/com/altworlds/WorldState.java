package com.altworlds;

/**
 * Represents the lifecycle state of a managed world.
 */
public enum WorldState {
    LOADING,           // World is loading or within min uptime.
    ACTIVE,            // World has players or locks.
    IDLE,              // World is empty but within cooldown.
    READY_TO_UNLOAD    // Safe to unload.
}