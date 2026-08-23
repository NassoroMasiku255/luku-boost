package com.lukuboost.electricityloan.common.security;

import java.util.Set;
import java.util.UUID;

public record AdminPrincipal(UUID userId, String username, Set<String> permissions) {

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
}
