package com.lukuboost.electricityloan.common.security;

import java.util.Set;

public record AdminPrincipal(Long userId, String username, Set<String> permissions) {

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
}
