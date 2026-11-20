package com.meocare.meocare.security;

import com.meocare.meocare.exception.ApiErrorResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            request.setAttribute(RestAuthenticationEntryPoint.AUTH_ERROR_ATTRIBUTE, "Invalid authorization token");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.parseClaims(token);
            Long userId = jwtService.extractUserId(claims);
            String email = jwtService.extractEmail(claims);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                CustomUserDetails userDetails = userId != null
                        ? userDetailsService.loadUserById(userId)
                        : (CustomUserDetails) userDetailsService.loadUserByUsername(email);

                if (!userDetails.isActive()) {
                    SecurityContextHolder.clearContext();
                    if (!isPublicAuthEndpoint(request)) {
                        writeForbidden(request, response, "Account is not active");
                        return;
                    }
                    filterChain.doFilter(request, response);
                    return;
                }

                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    SecurityContextHolder.clearContext();
                    request.setAttribute(RestAuthenticationEntryPoint.AUTH_ERROR_ATTRIBUTE, "Invalid authentication token");
                }
            }
        } catch (ExpiredJwtException ex) {
            SecurityContextHolder.clearContext();
            request.setAttribute(RestAuthenticationEntryPoint.AUTH_ERROR_ATTRIBUTE, "Authentication token has expired");
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
            SecurityContextHolder.clearContext();
            request.setAttribute(RestAuthenticationEntryPoint.AUTH_ERROR_ATTRIBUTE, "Invalid authentication token");
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicAuthEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "POST".equalsIgnoreCase(request.getMethod())
                && ("/api/auth/login".equals(path) || "/api/auth/register".equals(path));
    }

    private void writeForbidden(HttpServletRequest request,
                                HttpServletResponse response,
                                String message) throws IOException {
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                message,
                request.getRequestURI(),
                null
        );
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
