package org.example.pim_system.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Redirects user after login based on role:
     * - Admin → /index
     * - Employee → /display
 * - Other roles → /index (default)
 */
@Component
public class RoleBasedAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_EMPLOYEE = "ROLE_EMPLOYEE";
    private static final String REDIRECT_ADMIN = "/index";
    private static final String REDIRECT_EMPLOYEE = "/display";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        Collection<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        String redirectUrl = REDIRECT_ADMIN; // default

        if (roles.contains(ROLE_EMPLOYEE) && !roles.contains(ROLE_ADMIN)) {
            redirectUrl = REDIRECT_EMPLOYEE;
        } else if (roles.contains(ROLE_ADMIN)) {
            redirectUrl = REDIRECT_ADMIN;
        }

        response.sendRedirect(request.getContextPath() + redirectUrl);
    }
}
