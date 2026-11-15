package com.foodbridge.security;

import com.foodbridge.user.Role;
import com.foodbridge.user.UserStatus;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Stateless JWT authentication filter.
 *
 * <p>Runs for API requests only. A valid token with a non-INACTIVE status
 * produces an {@link AuthenticatedUser} principal with a {@code ROLE_x}
 * authority; anything else leaves the request anonymous so that authorization
 * rules can reject it with the correct 401/403.</p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER)) {
            String token = header.substring(BEARER.length()).trim();
            jwtService.parse(token).ifPresent(claims -> authenticate(claims, request));
        }
        chain.doFilter(request, response);
    }

    private void authenticate(Claims claims, HttpServletRequest request) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }
        try {
            Long userId = Long.valueOf(claims.getSubject());
            Role role = Role.valueOf(claims.get(JwtService.CLAIM_ROLE, String.class));
            UserStatus status = UserStatus.valueOf(claims.get(JwtService.CLAIM_STATUS, String.class));
            String email = claims.get(JwtService.CLAIM_EMAIL, String.class);
            String name = claims.get(JwtService.CLAIM_NAME, String.class);

            if (status == UserStatus.INACTIVE) {
                return;
            }

            AuthenticatedUser principal = new AuthenticatedUser(userId, email, name, role, status);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (IllegalArgumentException | NullPointerException ex) {
            // Malformed claims - treat as anonymous.
            SecurityContextHolder.clearContext();
        }
    }
}
