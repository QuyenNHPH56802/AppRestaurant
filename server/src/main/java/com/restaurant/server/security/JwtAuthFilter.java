package com.restaurant.server.security;

import com.restaurant.server.entity.User;
import com.restaurant.server.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Parses Authorization: Bearer <jwt>. Loads the user from DB to ensure they are still
 * ACTIVE; disabled users are not authenticated. The role is propagated as ROLE_xxx for
 * {@code @PreAuthorize("hasRole('ADMIN')")} support.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7).trim();
            Optional<JwtService.AuthPrincipal> parsed = jwtService.parse(token);
            if (parsed.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
                JwtService.AuthPrincipal p = parsed.get();
                Optional<User> userOpt = userRepository.findById(p.userId());
                if (userOpt.isPresent()) {
                    User u = userOpt.get();
                    if (u.getStatus() == User.Status.ACTIVE) {
                        var authToken = new UsernamePasswordAuthenticationToken(
                                new AuthenticatedUser(u),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()))
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        }
        chain.doFilter(request, response);
    }

    /** Wrapper exposed to controllers so they can read the full User without a second DB hit. */
    public record AuthenticatedUser(User user) {
        public Long id() { return user.getId(); }
        public String username() { return user.getUsername(); }
        public User.Role role() { return user.getRole(); }
        public String lang() { return user.getLang(); }
    }
}