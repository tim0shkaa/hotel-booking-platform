package edu.hotel.auth.service;

import edu.hotel.auth.dto.audit.AuditLogResponse;
import edu.hotel.auth.dto.refreshtoken.AuthResponse;
import edu.hotel.auth.dto.refreshtoken.RefreshRequest;
import edu.hotel.auth.dto.user.*;
import edu.hotel.auth.entity.AuditLog;
import edu.hotel.auth.entity.RefreshToken;
import edu.hotel.auth.entity.User;
import edu.hotel.auth.exception.AuthException;
import edu.hotel.auth.mapper.AuditLogMapper;
import edu.hotel.auth.mapper.UserMapper;
import edu.hotel.auth.model.Action;
import edu.hotel.auth.model.Role;
import edu.hotel.auth.repository.AuditRepository;
import edu.hotel.auth.repository.RefreshTokenRepository;
import edu.hotel.auth.repository.UserRepository;
import edu.hotel.common.exception.AlreadyExistsException;
import edu.hotel.common.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private AuditRepository auditRepository;
    @Mock private UserMapper userMapper;
    @Mock private AuditLogMapper auditLogMapper;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_shouldCreateUserWithGuestRoleAndSaveAudit() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@mail.com");
        request.setPassword("password");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed");

        RegisterResponse result = authService.register(request, "127.0.0.1", "Mozilla");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("test@mail.com");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getRole()).isEqualTo(Role.GUEST);
        assertThat(saved.isActive()).isTrue();

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo(Action.REGISTER);
    }

    @Test
    void register_shouldThrowAlreadyExistsException_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@mail.com");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> authService.register(request, "127.0.0.1", "Mozilla"))
                .isInstanceOf(AlreadyExistsException.class);

        verify(userRepository, never()).save(any());
        verify(auditRepository, never()).save(any());
    }

    @Test
    void login_shouldReturnTokensAndSaveAudit() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 86400000L);

        LoginRequest request = new LoginRequest();
        request.setEmail("test@mail.com");
        request.setPassword("password");

        User user = new User();
        user.setId(1L);
        user.setActive(true);
        user.setPasswordHash("hashed");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("accessToken");
        when(jwtService.generateRefreshToken()).thenReturn("refreshToken");
        when(jwtService.hashToken("refreshToken")).thenReturn("hashedRefresh");

        AuthResponse result = authService.login(request, "127.0.0.1", "Mozilla");

        assertThat(result.getAccessToken()).isEqualTo("accessToken");
        assertThat(result.getRefreshToken()).isEqualTo("refreshToken");

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getTokenHash()).isEqualTo("hashedRefresh");
        assertThat(tokenCaptor.getValue().isRevoked()).isFalse();

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo(Action.LOGIN);
    }

    @Test
    void login_shouldThrowNotFoundException_whenUserNotFound() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@mail.com");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "Mozilla"))
                .isInstanceOf(NotFoundException.class);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void login_shouldThrowAuthException_whenAccountDeactivated() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@mail.com");

        User user = new User();
        user.setActive(false);

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "Mozilla"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("деактивирован");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void login_shouldThrowAuthException_whenWrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@mail.com");
        request.setPassword("wrong");

        User user = new User();
        user.setActive(true);
        user.setPasswordHash("hashed");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "Mozilla"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("пароль");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_shouldRevokeOldTokenAndIssueNew() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 86400000L);

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("oldToken");

        User user = new User();
        user.setId(1L);

        RefreshToken existToken = new RefreshToken();
        existToken.setRevoked(false);
        existToken.setExpiresAt(LocalDateTime.now().plusDays(1));
        existToken.setUser(user);

        when(jwtService.hashToken("oldToken")).thenReturn("hashedOld");
        when(refreshTokenRepository.findByTokenHash("hashedOld")).thenReturn(Optional.of(existToken));
        when(jwtService.generateAccessToken(user)).thenReturn("newAccess");
        when(jwtService.generateRefreshToken()).thenReturn("newRefresh");
        when(jwtService.hashToken("newRefresh")).thenReturn("hashedNew");

        AuthResponse result = authService.refresh(request, "127.0.0.1", "Mozilla");

        assertThat(result.getAccessToken()).isEqualTo("newAccess");
        assertThat(result.getRefreshToken()).isEqualTo("newRefresh");
        assertThat(existToken.isRevoked()).isTrue();

        verify(refreshTokenRepository, times(2)).save(any());
    }

    @Test
    void refresh_shouldThrowAuthException_whenTokenRevoked() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("token");

        RefreshToken existToken = new RefreshToken();
        existToken.setRevoked(true);

        when(jwtService.hashToken("token")).thenReturn("hashed");
        when(refreshTokenRepository.findByTokenHash("hashed")).thenReturn(Optional.of(existToken));

        assertThatThrownBy(() -> authService.refresh(request, "127.0.0.1", "Mozilla"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("аннулирован");
    }

    @Test
    void refresh_shouldThrowAuthException_whenTokenExpired() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("token");

        RefreshToken existToken = new RefreshToken();
        existToken.setRevoked(false);
        existToken.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(jwtService.hashToken("token")).thenReturn("hashed");
        when(refreshTokenRepository.findByTokenHash("hashed")).thenReturn(Optional.of(existToken));

        assertThatThrownBy(() -> authService.refresh(request, "127.0.0.1", "Mozilla"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("истек");
    }

    @Test
    void logout_shouldRevokeTokenAndBlacklistAccessToken() {
        User user = new User();

        RefreshToken existToken = new RefreshToken();
        existToken.setRevoked(false);
        existToken.setUser(user);

        when(jwtService.hashToken("refreshToken")).thenReturn("hashed");
        when(refreshTokenRepository.findByTokenHash("hashed")).thenReturn(Optional.of(existToken));
        when(jwtService.getAccessTokenExpiration()).thenReturn(3600000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authService.logout("refreshToken", "127.0.0.1", "Mozilla", "accessToken");

        assertThat(existToken.isRevoked()).isTrue();
        verify(valueOperations).set(eq("blacklist:accessToken"), eq("revoked"), any());
        verify(refreshTokenRepository).save(existToken);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo(Action.LOGOUT);
    }

    @Test
    void logout_shouldThrowAuthException_whenTokenAlreadyRevoked() {
        RefreshToken existToken = new RefreshToken();
        existToken.setRevoked(true);

        when(jwtService.hashToken("token")).thenReturn("hashed");
        when(refreshTokenRepository.findByTokenHash("hashed")).thenReturn(Optional.of(existToken));

        assertThatThrownBy(() -> authService.logout("token", "127.0.0.1", "Mozilla", "access"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("аннулирован");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void getCurrentUserData_shouldReturnUserResponse() {
        Long userId = 1L;
        User user = new User();
        UserResponse response = new UserResponse();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = authService.getCurrentUserData(userId);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void changePassword_shouldUpdatePasswordAndRevokeAllTokens() {
        Long userId = 1L;
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("old");
        request.setNewPassword("new");

        User user = new User();
        user.setId(userId);
        user.setPasswordHash("hashedOld");

        UserResponse response = new UserResponse();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "hashedOld")).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("hashedNew");
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = authService.changePassword(userId, request, "127.0.0.1", "Mozilla");

        assertThat(result).isEqualTo(response);
        assertThat(user.getPasswordHash()).isEqualTo("hashedNew");
        verify(refreshTokenRepository).revokeAllByUserId(userId);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo(Action.PASSWORD_CHANGE);
    }

    @Test
    void changePassword_shouldThrowAuthException_whenWrongOldPassword() {
        Long userId = 1L;
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrong");

        User user = new User();
        user.setPasswordHash("hashed");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(userId, request, "127.0.0.1", "Mozilla"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("пароль");

        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).revokeAllByUserId(any());
    }

    @Test
    void getUsers_shouldReturnFilteredPage() {
        Pageable pageable = Pageable.unpaged();
        User user = new User();
        UserResponse response = new UserResponse();

        when(userRepository.findAllWithFilters(eq("GUEST"), eq(true), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(userMapper.toResponse(user)).thenReturn(response);

        Page<UserResponse> result = authService.getUsers("GUEST", true, pageable);

        assertThat(result).hasSize(1);
        assertThat(result.getContent().getFirst()).isEqualTo(response);
    }

    @Test
    void changeRole_shouldUpdateRole() {
        Long userId = 1L;
        User user = new User();
        UserResponse response = new UserResponse();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = authService.changeRole(userId, Role.ADMIN);

        assertThat(result).isEqualTo(response);
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        verify(userRepository).save(user);
    }

    @Test
    void deactivateUser_shouldSetActiveToFalse() {
        Long userId = 1L;
        User user = new User();
        user.setActive(true);
        UserResponse response = new UserResponse();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = authService.deactivateUser(userId);

        assertThat(result).isEqualTo(response);
        assertThat(user.isActive()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void getAuditLogs_shouldReturnPage() {
        Pageable pageable = Pageable.unpaged();
        AuditLog auditLog = new AuditLog();
        AuditLogResponse response = new AuditLogResponse();

        when(auditRepository.findAllWithFilters(any(), any(), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(auditLog)));
        when(auditLogMapper.toResponse(auditLog)).thenReturn(response);

        Page<AuditLogResponse> result = authService.getAuditLogs(null, null, null, null, pageable);

        assertThat(result).hasSize(1);
        assertThat(result.getContent().getFirst()).isEqualTo(response);
    }
}