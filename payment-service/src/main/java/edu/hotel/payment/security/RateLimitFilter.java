package edu.hotel.payment.security;

import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        long limit;
        String key;

        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            limit = 100;
            key = "rate:ip:" + request.getRemoteAddr();
        } else if (auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            limit = 1000;
            key = "rate:user:" + auth.getPrincipal();
        } else {
            limit = 500;
            key = "rate:user:" + auth.getPrincipal();
        }

        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            filterChain.doFilter(request, response);
            return;
        }
        if (count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }

        if (count > limit) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.getWriter().write("Too many response");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
