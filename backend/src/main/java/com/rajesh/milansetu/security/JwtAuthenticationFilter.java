package com.rajesh.milansetu.security;

import com.rajesh.milansetu.repository.RevokedTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs once per request. If a valid, non-revoked Bearer token is present, the user is loaded fresh from the DB
 * (so a deactivated account or changed role takes effect immediately) and put into the SecurityContext.
 * If not, the request continues anonymously and Spring Security returns 401 for protected URLs.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String JTI_ATTRIBUTE = "jwt.jti";
    public static final String EXP_ATTRIBUTE = "jwt.exp";

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;
    private final RevokedTokenRepository revokedTokens;

    public JwtAuthenticationFilter(JwtService jwtService, AppUserDetailsService userDetailsService,
                                   RevokedTokenRepository revokedTokens) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.revokedTokens = revokedTokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            jwtService.parse(header.substring(7)).ifPresent(data -> {
                if (revokedTokens.existsById(data.jti())) return;
                try {
                    AppUserPrincipal user = userDetailsService.loadById(data.userId());
                    if (!user.isEnabled()) return;
                    var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    request.setAttribute(JTI_ATTRIBUTE, data.jti());
                    request.setAttribute(EXP_ATTRIBUTE, data.expiresAt());
                } catch (UsernameNotFoundException ignored) {
                    // token for a deleted user: stay anonymous
                }
            });
        }
        chain.doFilter(request, response);
    }
}
