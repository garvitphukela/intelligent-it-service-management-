package com.itsm.service;

import com.itsm.domain.user.*;
import com.itsm.exception.ConflictException;
import com.itsm.exception.InvalidInputException;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.repository.UserRepository;
import com.itsm.security.CurrentUserService;
import com.itsm.web.dto.Mapper;
import com.itsm.web.dto.Requests;
import com.itsm.web.dto.Views.UserView;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Administrator operations on users and technicians. */
@Service
@Transactional
public class UserAdminService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthService auth;
    private final AssignmentService assignment;
    private final CurrentUserService currentUser;

    public UserAdminService(UserRepository users, PasswordEncoder encoder, AuthService auth,
                            AssignmentService assignment, CurrentUserService currentUser) {
        this.users = users;
        this.encoder = encoder;
        this.auth = auth;
        this.assignment = assignment;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<UserView> list(Role role) {
        Map<Long, Integer> load = assignment.currentWorkloads();
        return users.findAll().stream()
                .filter(u -> role == null || u.getRole() == role)
                .sorted(Comparator.comparing((User u) -> u.getRole().ordinal()).thenComparing(User::getCode))
                .map(u -> Mapper.user(u, load.getOrDefault(u.getId(), 0)))
                .toList();
    }

    public UserView create(Requests.CreateUser req) {
        AuthService.validatePassword(req.password());
        if (users.existsByEmailIgnoreCase(req.email())) throw new ConflictException("Email already registered");
        if (req.role().isSupportEngineer() && (req.skills() == null || req.skills().isEmpty())) {
            throw new InvalidInputException("Technicians need at least one skill");
        }
        User u = UserFactory.create(new UserFactory.UserSpec(req.role(), auth.nextCode(req.role()), req.fullName().trim(),
                req.email().trim(), encoder.encode(req.password()), req.skills(), req.experienceYears(), req.maxWorkload()));
        u.updateProfile(req.fullName(), req.phone(), req.department());
        users.save(u);
        return Mapper.user(u, 0);
    }

    public UserView update(Long id, Requests.UpdateUser req) {
        User u = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User " + id + " not found"));
        u.updateProfile(req.fullName(), req.phone() != null ? req.phone() : u.getPhone(),
                req.department() != null ? req.department() : u.getDepartment());
        if (req.active() != null) {
            if (!req.active() && u.getId().equals(currentUser.principal().id())) {
                throw new ConflictException("You cannot deactivate your own account");
            }
            if (req.active()) u.activate(); else u.deactivate();
        }
        if (req.preferredChannels() != null) u.setPreferredChannels(req.preferredChannels());
        if (req.newPassword() != null) {
            AuthService.validatePassword(req.newPassword());
            u.changePassword(encoder.encode(req.newPassword()));
        }
        if (u instanceof Technician t) {
            if (req.skills() != null) t.setSkills(req.skills());
            if (req.maxWorkload() != null || req.experienceYears() != null) {
                t.updateCapacity(req.maxWorkload() != null ? req.maxWorkload() : t.getMaxWorkload(),
                        req.experienceYears() != null ? req.experienceYears() : t.getExperienceYears());
            }
            if (req.available() != null) t.setAvailable(req.available());
        }
        return Mapper.user(u, u instanceof Technician t ? assignment.workloadOf(t) : null);
    }
}
