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

    RegisterResponse register(RegisterRequest request, String ipAddress, String userAgent);

    AuthResponse login(LoginRequest request, String ipAddress, String userAgent);

    AuthResponse refresh(RefreshRequest request, String ipAddress, String userAgent);

    void logout(String refreshToken, String ipAddress, String userAgent);

    UserResponse getCurrentUserData(Long userId);

    UserResponse changePassword(Long userId, ChangePasswordRequest request, String ipAddress, String userAgent);

    Page<UserResponse> getUsers(String role, Boolean active, Pageable pageable);

    UserResponse changeRole(Long userId, Role role);

    UserResponse deactivateUser(Long userId);

    Page<AuditLogResponse> getAuditLogs(Long userId, String action, LocalDateTime from, LocalDateTime to, Pageable pageable);
}
