package com.itsm.security;

import com.itsm.domain.user.Role;

import java.security.Principal;

/** Principal stored in the security context. {@link #getName()} is the user id (used for WebSocket user queues). */
public record AuthenticatedUser(Long id, String email, Role role, String fullName, String code) implements Principal {
    @Override
    public String getName() { return String.valueOf(id); }
}
