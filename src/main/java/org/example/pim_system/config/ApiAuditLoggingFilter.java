package org.example.pim_system.config;

// This generic API-level audit filter has been disabled.
// All audit logging is now handled explicitly in each controller
// with human-readable action names and meaningful change details.
//
// The class is kept as a placeholder to avoid breaking any Spring
// component-scan references. It no longer filters any requests.

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiAuditLoggingFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip ALL requests — audit logging is now done at the controller level.
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, response);
    }
}
