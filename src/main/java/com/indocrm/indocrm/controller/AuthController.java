// src/main/java/com/indocrm/indocrm/controller/AuthController.java

package com.indocrm.indocrm.controller;

import com.indocrm.indocrm.dto.AuthRequest;
import com.indocrm.indocrm.dto.AuthResponse;
import com.indocrm.indocrm.model.Employee;
import com.indocrm.indocrm.model.Telecaller;
import com.indocrm.indocrm.model.User;
import com.indocrm.indocrm.repository.TelecallerRepository;
import com.indocrm.indocrm.repository.UserRepository;
import com.indocrm.indocrm.security.JwtUtil;
import com.indocrm.indocrm.service.AttendanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final TelecallerRepository telecallerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AttendanceService attendanceService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          UserDetailsService userDetailsService,
                          UserRepository userRepository,
                          TelecallerRepository telecallerRepository,
                          PasswordEncoder passwordEncoder,
                          AttendanceService attendanceService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.telecallerRepository = telecallerRepository;
        this.passwordEncoder = passwordEncoder;
        this.attendanceService = attendanceService;
    }

    private static ResponseEntity<Map<String, Object>> fail(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);   // read by both frontends (error.message)
        body.put("error", message);
        body.put("status", status.value());
        return ResponseEntity.status(status).body(body);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {

        // Usernames are stored trimmed; a stray space typed/pasted in the login box
        // used to make a correct login fail.
        final String username = request.getUsername() == null ? "" : request.getUsername().trim();
        final String password = request.getPassword() == null ? "" : request.getPassword();

        if (username.isEmpty() || password.isEmpty()) {
            return fail(HttpStatus.BAD_REQUEST, "Username and password are required");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
        } catch (DisabledException | LockedException e) {
            log.warn("Login blocked for '{}': account disabled/locked", username);
            return fail(HttpStatus.FORBIDDEN, "Your account is disabled. Please contact your administrator.");
        } catch (AuthenticationException e) {
            log.warn("Login failed for '{}': {}", username, e.getClass().getSimpleName());
            return fail(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        final String jwt = jwtUtil.generateToken(userDetails);

        String fullName = userDetails.getUsername();
        String role = "USER";
        Telecaller telecaller = null;

        if (userDetails instanceof User u) {
            fullName = u.getFullName() != null ? u.getFullName() : u.getUsername();
            role = u.getRole() != null ? u.getRole() : "USER";
        } else if (userDetails instanceof Telecaller t) {
            telecaller = t;
            fullName = t.getFullName() != null ? t.getFullName() : t.getUsername();
            role = t.getRole() != null ? t.getRole() : "Telecaller";
        } else if (userDetails instanceof Employee e) {
            fullName = e.getFullName() != null ? e.getFullName() : e.getUsername();
            role = e.getRole() != null ? e.getRole() : "EMPLOYEE";
            // Legacy: a telecaller that only matched through its employee mirror.
            telecaller = telecallerRepository.findByUsername(username).orElse(null);
        }

        AuthResponse response = new AuthResponse();
        response.setToken(jwt);
        response.setUsername(userDetails.getUsername());
        response.setFullName(fullName);
        response.setRole(role);

        if (telecaller != null) {
            response.setDistricts(telecaller.getDistricts());
            // Login is also the attendance check-in event. It must NEVER block the login
            // itself - a failure here used to turn a correct password into a failed login.
            try {
                attendanceService.login(telecaller.getUsername(), null, null);
            } catch (Exception ex) {
                log.error("Attendance check-in failed for '{}' (login still allowed): {}",
                        username, ex.getMessage());
            }
        }

        return ResponseEntity.ok(response);
    }

    // Registration is now restricted to ADMIN / SUPER_ADMIN in SecurityConfig.
    // Role is intentionally NOT taken from the request body - every account created here
    // is a plain USER. Elevated roles are created by seeding / the admin UI only.
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody AuthRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank() ||
            request.getPassword() == null || request.getPassword().isBlank()) {
            return fail(HttpStatus.BAD_REQUEST, "Username and password are required");
        }

        String username = request.getUsername().trim();

        if (userRepository.findByUsername(username).isPresent()
                || telecallerRepository.existsByUsername(username)) {
            return fail(HttpStatus.CONFLICT, "Username already exists");
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setFullName(request.getFullName() != null ? request.getFullName() : username);
        newUser.setEmail(request.getEmail());
        newUser.setRole("USER");
        newUser.setEnabled(true);
        userRepository.save(newUser);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User registered successfully");
        response.put("username", newUser.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken() {
        // The JWT filter already validated the token before reaching this endpoint.
        Map<String, Object> response = new HashMap<>();
        response.put("valid", true);
        response.put("message", "Token is valid");
        return ResponseEntity.ok(response);
    }
}
