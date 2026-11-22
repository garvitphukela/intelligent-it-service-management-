package com.itsm.service;

import com.itsm.domain.user.Role;
import com.itsm.domain.user.User;
import com.itsm.domain.user.UserFactory;
import com.itsm.exception.ConflictException;
import com.itsm.exception.InvalidInputException;
import com.itsm.exception.InvalidLoginException;
import com.itsm.repository.UserRepository;
import com.itsm.security.CurrentUserService;
import com.itsm.security.JwtService;
import com.itsm.web.dto.Mapper;
import com.itsm.web.dto.Requests;
import com.itsm.web.dto.Views.AuthResponse;
import com.itsm.web.dto.Views.UserView;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@Transactional
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final CurrentUserService currentUser;
    private final AssignmentService assignment;
    private final Clock clock;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt, CurrentUserService currentUser,
                       AssignmentService assignment, Clock clock) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.currentUser = currentUser;
        this.assignment = assignment;
        this.clock = clock;
    }

    public AuthResponse login(Requests.Login req) {
        User u = users.findByEmailIgnoreCase(req.email().trim())
                .filter(x -> encoder.matches(req.password(), x.getPasswordHash()))
                .orElseThrow(() -> new InvalidLoginException("Invalid email or password"));
        if (!u.isActive()) throw new InvalidLoginException("This account has been deactivated. Contact your administrator.");
        return token(u);
    }

    /** Self-registration always creates an Employee; staff accounts are created by administrators. */
    public AuthResponse register(Requests.Register req) {
        validatePassword(req.password());
        if (users.existsByEmailIgnoreCase(req.email().trim())) {
            throw new ConflictException("An account with this email already exists");
        }
        User u = UserFactory.create(UserFactory.UserSpec.basic(Role.EMPLOYEE, nextCode(Role.EMPLOYEE),
                req.fullName().trim(), req.email().trim(), encoder.encode(req.password())));
        u.updateProfile(req.fullName().trim(), req.phone(), req.department());
        users.save(u);
        return token(u);
    }

    @Transactional(readOnly = true)
    public UserView me() {
        User u = currentUser.user();
        return Mapper.user(u, u instanceof com.itsm.domain.user.Technician t ? assignment.workloadOf(t) : null);
    }

    public UserView updateProfile(Requests.UpdateProfile req) {
        User u = currentUser.user();
        u.updateProfile(req.fullName(), req.phone(), req.department());
        if (req.preferredChannels() != null) u.setPreferredChannels(req.preferredChannels());
        return Mapper.user(u, null);
    }

    public static void validatePassword(String password) {
        if (password == null || password.length() < 8 || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new InvalidInputException("Password must be at least 8 characters and contain letters and digits");
        }
    }

    public String nextCode(Role role) {
        String prefix = UserFactory.codePrefix(role);
        long n = users.count() + 100;
        String code;
        do {
            code = prefix + (++n);
        } while (users.existsByCode(code));
        return code;
    }

    private AuthResponse token(User u) {
        return new AuthResponse(jwt.issue(u), Instant.now(clock).plus(jwt.ttl()),
                Mapper.user(u, u instanceof com.itsm.domain.user.Technician t ? assignment.workloadOf(t) : null));
    }
}
