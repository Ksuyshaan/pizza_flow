package com.pizzaflow.order.controller;

import com.pizzaflow.common.ApiResponse;
import com.pizzaflow.common.exception.ConflictException;
import com.pizzaflow.order.entity.User;
import com.pizzaflow.order.entity.UserRole;
import com.pizzaflow.order.repository.UserRepository;
import com.pizzaflow.report.service.AuditService;
import com.pizzaflow.security.JwtTokenProvider;
import com.pizzaflow.security.UserPrincipal;
import com.pizzaflow.security.dto.AuthRequest;
import com.pizzaflow.security.dto.AuthResponse;
import com.pizzaflow.security.dto.RegisterRequest;
import com.pizzaflow.security.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuditService auditService;

    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider tokenProvider,
                          AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.auditService = auditService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest req) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = tokenProvider.generateToken(authentication);

        User user = userRepository.findByEmail(req.getEmail()).get();
        AuthResponse authResponse = new AuthResponse(token, UserResponse.fromEntity(user));

        auditService.log(user.getEmail(), user.getRole().name(), "LOGIN", "Успешная авторизация в системе");

        return ResponseEntity.ok(ApiResponse.ok("Вход выполнен успешно", authResponse));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ConflictException("Пользователь с email '" + req.getEmail() + "' уже зарегистрирован");
        }

        User user = new User(
                req.getEmail(),
                passwordEncoder.encode(req.getPassword()),
                req.getFullName(),
                req.getPhone(),
                req.getRole() != null ? req.getRole() : UserRole.CUSTOMER
        );

        User saved = userRepository.save(user);
        String token = tokenProvider.generateToken(saved.getEmail(), saved.getId(), saved.getRole().name());

        auditService.log(saved.getEmail(), saved.getRole().name(), "REGISTER", "Регистрация нового пользователя");

        AuthResponse authResponse = new AuthResponse(token, UserResponse.fromEntity(saved));
        return ResponseEntity.ok(ApiResponse.ok("Регистрация успешна", authResponse));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Не авторизован"));
        }
        User user = userRepository.findById(principal.getId()).get();
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.fromEntity(user)));
    }
}
