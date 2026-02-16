package com.liquorshop.inventory.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                try {
                    // Check if token is expired
                    if (tokenProvider.isTokenExpired(jwt)) {
                        handleAuthenticationError(response, "Token expired", "TOKEN_EXPIRED", 
                                "Your session has expired. Please login again.");
                        return;
                    }

                    // Validate token format and signature
                    if (!tokenProvider.isTokenValidFormat(jwt)) {
                        handleAuthenticationError(response, "Invalid token", "INVALID_TOKEN", 
                                "The provided token is invalid.");
                        return;
                    }

                    // Validate token
                    if (tokenProvider.validateToken(jwt)) {
                        String username = tokenProvider.getUsernameFromToken(jwt);

                        try {
                            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                            // Check if user is still active/enabled
                            if (!userDetails.isEnabled()) {
                                handleAuthenticationError(response, "User account is disabled", "USER_DISABLED", 
                                        "Your account has been disabled. Please contact administrator.");
                                return;
                            }

                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        } catch (UsernameNotFoundException ex) {
                            handleAuthenticationError(response, "User not found", "USER_NOT_FOUND", 
                                    "User account not found.");
                            return;
                        }
                    } else {
                        handleAuthenticationError(response, "Invalid token", "INVALID_TOKEN", 
                                "The provided token is invalid.");
                        return;
                    }
                } catch (ExpiredJwtException ex) {
                    handleAuthenticationError(response, "Token expired", "TOKEN_EXPIRED", 
                            "Your session has expired. Please login again.");
                    return;
                } catch (MalformedJwtException | SignatureException ex) {
                    handleAuthenticationError(response, "Invalid token", "INVALID_TOKEN", 
                            "The provided token is invalid.");
                    return;
                }
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
            handleAuthenticationError(response, "Authentication error", "AUTH_ERROR", 
                    "An error occurred during authentication.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void handleAuthenticationError(HttpServletResponse response, String error, String errorCode, String message) 
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", error);
        errorResponse.put("errorCode", errorCode);
        errorResponse.put("message", message);
        errorResponse.put("status", HttpServletResponse.SC_UNAUTHORIZED);

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
