package edu.hotel.auth.service;

import edu.hotel.auth.dto.audit.AuditLogResponse;
import edu.hotel.auth.dto.refreshtoken.AuthResponse;
import edu.hotel.auth.dto.refreshtoken.RefreshRequest;
import edu.hotel.auth.dto.user.*;
import edu.hotel.auth.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

public interface AuthService {

    // POST /auth/register
    RegisterResponse register(RegisterRequest request, String ipAddress, String userAgent);

    // POST /auth/login
    AuthResponse login(LoginRequest request, String ipAddress, String userAgent);

    // POST /auth/refresh
    AuthResponse refresh(RefreshRequest request, String ipAddress, String userAgent);

    // POST /auth/logout
    void logout(String refreshToken, String ipAddress, String userAgent);

    // GET /users/me
    UserResponse getCurrentUserData(Long userId);

    // PUT /users/me/password
    UserResponse changePassword(Long userId, ChangePasswordRequest request, String ipAddress, String userAgent);

    // GET /users
    Page<UserResponse> getUsers(String role, Boolean active, Pageable pageable);

    // PUT /users/{id}/role
    UserResponse changeRole(Long userId, Role role);

    // POST /users/{id}/deactivate
    UserResponse deactivateUser(Long userId);

    // GET /users/audit
    Page<AuditLogResponse> getAuditLogs(Long userId, String action, LocalDateTime from, LocalDateTime to, Pageable pageable);
}
