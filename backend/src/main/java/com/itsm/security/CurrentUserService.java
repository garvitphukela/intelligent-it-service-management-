package com.itsm.security;

import com.itsm.domain.user.User;
import com.itsm.exception.InvalidLoginException;
import com.itsm.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Resolves the user behind the current request. */
@Service
public class CurrentUserService {

    private final UserRepository users;

    public CurrentUserService(UserRepository users) {
        this.users = users;
    }

    public AuthenticatedUser principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser u)) {
            throw new InvalidLoginException("Not authenticated");
        }
        return u;
    }

    public User user() {
        User u = users.findById(principal().id()).orElseThrow(() -> new InvalidLoginException("User no longer exists"));
        if (!u.isActive()) throw new InvalidLoginException("Account is deactivated");
        return u;
    }
}
