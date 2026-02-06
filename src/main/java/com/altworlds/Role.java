package com.altworlds;

public enum Role {
    OWNER,   // Control total
    ADMIN,   // Invitar, expulsar, banear
    MEMBER,  // Construir
    VISITOR; // Solo mirar

    // Este mÃ©todo es necesario para que funcione la jerarquÃ­a de permisos
    public boolean isAtLeast(Role other) {
        return this.ordinal() <= other.ordinal();
    }
}