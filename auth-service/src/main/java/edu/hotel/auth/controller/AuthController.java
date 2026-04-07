package edu.hotel.auth.controller;

import edu.hotel.auth.dto.audit.AuditLogResponse;
import edu.hotel.auth.dto.refreshtoken.AuthResponse;
import edu.hotel.auth.dto.refreshtoken.RefreshRequest;
import edu.hotel.auth.dto.user.*;
import edu.hotel.auth.model.Role;
import edu.hotel.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpServletRequest) {

        String ip = httpServletRequest.getRemoteAddr();
        String userAgent = httpServletRequest.getHeader("User-Agent");

        return ResponseEntity.status(HttpStatus.CREATED).
                body(authService.register(request, ip, userAgent));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest) {

        String ip = httpServletRequest.getRemoteAddr();
        String userAgent = httpServletRequest.getHeader("User-Agent");

        return ResponseEntity.ok(authService.login(request, ip, userAgent));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshRequest request,
            HttpServletRequest httpServletRequest) {

        String ip = httpServletRequest.getRemoteAddr();
        String userAgent = httpServletRequest.getHeader("User-Agent");

        return ResponseEntity.ok(authService.refresh(request, ip, userAgent));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody RefreshRequest request,
            HttpServletRequest httpRequest) {

        String ip = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        authService.logout(request.getRefreshToken(), ip, userAgent);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/me")
    public ResponseEntity<UserResponse> getMe(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(authService.getCurrentUserData(userId));
    }

    @PutMapping("/users/me/password")
    public ResponseEntity<UserResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        Long userId = (Long) authentication.getPrincipal();
        String ip = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        return ResponseEntity.ok(authService.changePassword(userId, request, ip, userAgent));
    }

    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> getUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {

        return ResponseEntity.ok(authService.getUsers(role, active, pageable));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> changeRole(
            @PathVariable Long id,
            @RequestParam Role role) {

        return ResponseEntity.ok(authService.changeRole(id, role));
    }

    @PostMapping("/users/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable Long id) {
        return ResponseEntity.ok(authService.deactivateUser(id));
    }

    @GetMapping("/users/audit")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            Pageable pageable) {

        return ResponseEntity.ok(authService.getAuditLogs(userId, action, from, to, pageable));
    }
}
