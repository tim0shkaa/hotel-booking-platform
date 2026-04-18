package edu.hotel.auth.service;

import edu.hotel.auth.dto.audit.AuditLogResponse;
import edu.hotel.auth.dto.refreshtoken.AuthResponse;
import edu.hotel.auth.dto.refreshtoken.RefreshRequest;
import edu.hotel.auth.dto.user.*;
import edu.hotel.auth.entity.AuditLog;
import edu.hotel.auth.entity.RefreshToken;
import edu.hotel.auth.entity.User;
import edu.hotel.common.exception.AlreadyExistsException;
import edu.hotel.auth.exception.AuthException;
import edu.hotel.common.exception.NotFoundException;
import edu.hotel.auth.mapper.AuditLogMapper;
import edu.hotel.auth.mapper.UserMapper;
import edu.hotel.auth.model.Action;
import edu.hotel.auth.model.Role;
import edu.hotel.auth.repository.AuditRepository;
import edu.hotel.auth.repository.RefreshTokenRepository;
import edu.hotel.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final RefreshTokenRepository refreshTokenRepository;

    private final AuditRepository auditRepository;

    private final UserMapper userMapper;

    private final AuditLogMapper auditLogMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public RegisterResponse register(
            RegisterRequest request, String ipAddress, String userAgent) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AlreadyExistsException("Пользователь с таким email уже существует");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.GUEST);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);

        saveAuditLog(user, Action.REGISTER, ipAddress, userAgent);

        RegisterResponse registerResponse = new RegisterResponse();
        registerResponse.setId(user.getId());
        return registerResponse;
    }

    @Override
    @Transactional
    public AuthResponse login(
            LoginRequest request, String ipAddress, String userAgent) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("Пользователя с таким email не существует"));

        if (!user.isActive()) {
            throw new AuthException("Аккаунт деактивирован");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthException("Неверный пароль");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String rowStringRefreshToken = jwtService.generateRefreshToken();

        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setUser(user);
        refreshToken.setTokenHash(jwtService.hashToken(rowStringRefreshToken));
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000));
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        saveAuditLog(user, Action.LOGIN, ipAddress, userAgent);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken(accessToken);
        authResponse.setRefreshToken(rowStringRefreshToken);

        return authResponse;
    }

    @Override
    @Transactional
    public AuthResponse refresh(
            RefreshRequest request, String ipAddress, String userAgent) {

        String hashToken = jwtService.hashToken(request.getRefreshToken());

        RefreshToken existToken = refreshTokenRepository.findByTokenHash(hashToken)
                .orElseThrow(() -> new NotFoundException("Сессии с таким токеном не существует"));

        if (existToken.isRevoked()) {
            throw new AuthException("Токен аннулирован");
        }

        if (existToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AuthException("Срок жизни токена истек");
        }

        existToken.setRevoked(true);
        refreshTokenRepository.save(existToken);

        String accessToken = jwtService.generateAccessToken(existToken.getUser());
        String rowStringRefreshToken = jwtService.generateRefreshToken();

        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setRevoked(false);
        newRefreshToken.setUser(existToken.getUser());
        newRefreshToken.setTokenHash(jwtService.hashToken(rowStringRefreshToken));
        newRefreshToken.setCreatedAt(LocalDateTime.now());
        newRefreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000));

        refreshTokenRepository.save(newRefreshToken);

        saveAuditLog(newRefreshToken.getUser(), Action.REFRESH_TOKEN, ipAddress, userAgent);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken(accessToken);
        authResponse.setRefreshToken(rowStringRefreshToken);

        return authResponse;
    }

    @Override
    @Transactional
    public void logout(
            String refreshToken, String ipAddress, String userAgent, String accessToken) {

        String hashToken = jwtService.hashToken(refreshToken);

        RefreshToken existToken = refreshTokenRepository.findByTokenHash(hashToken)
                .orElseThrow(() -> new NotFoundException("Сессии с таким токеном не существует"));

        if (existToken.isRevoked()) {
            throw new AuthException("Токен аннулирован");
        }

        existToken.setRevoked(true);

        redisTemplate.opsForValue().set(
                "blacklist:" + accessToken,
                "revoked",
                Duration.ofMillis(jwtService.getAccessTokenExpiration())
        );

        refreshTokenRepository.save(existToken);

        saveAuditLog(existToken.getUser(), Action.LOGOUT, ipAddress, userAgent);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserData(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователя с таким id не существует"));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse changePassword(Long userId, ChangePasswordRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователя с таким id не существует"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new AuthException("Неверный пароль");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllByUserId(userId);

        saveAuditLog(user, Action.PASSWORD_CHANGE, ipAddress, userAgent);

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsers(String role, Boolean active, Pageable pageable) {
        Page<User> users = userRepository.findAllWithFilters(role, active, pageable);
        return users.map(userMapper::toResponse);
    }

    @Override
    @Transactional
    public UserResponse changeRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователя с таким id не существует"));

        user.setRole(role);
        userRepository.save(user);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователя с таким id не существует"));

        user.setActive(false);
        userRepository.save(user);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(
            Long userId, String action, LocalDateTime from, LocalDateTime to, Pageable pageable) {

        Page<AuditLog> auditLogs = auditRepository.findAllWithFilters(userId, action, from, to, pageable);
        return auditLogs.map(auditLogMapper::toResponse);
    }


    private void saveAuditLog(User user, Action action, String ipAddress, String userAgent) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUser(user);
        auditLog.setAction(action);
        auditLog.setIpAddress(ipAddress);
        auditLog.setUserAgent(userAgent);
        auditLog.setCreatedAt(LocalDateTime.now());
        auditRepository.save(auditLog);
    }
}
