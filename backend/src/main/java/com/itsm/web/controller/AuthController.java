package com.itsm.web.controller;

import com.itsm.service.AuthService;
import com.itsm.web.dto.Requests;
import com.itsm.web.dto.Views.AuthResponse;
import com.itsm.web.dto.Views.UserView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody Requests.Login req) { return auth.login(req); }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody Requests.Register req) { return auth.register(req); }

    @GetMapping("/me")
    public UserView me() { return auth.me(); }

    @PutMapping("/me")
    public UserView updateProfile(@Valid @RequestBody Requests.UpdateProfile req) { return auth.updateProfile(req); }
}
