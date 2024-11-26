package org.comdis.p2p;

/**
 * Almacena el estado de una peticion de amistad
 */
public enum FriendStatus {
    PENDING("pendiente"),
    ACCEPTED("aceptada"),
    REJECTED("rechazada");

    private final String value;

    FriendStatus(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
