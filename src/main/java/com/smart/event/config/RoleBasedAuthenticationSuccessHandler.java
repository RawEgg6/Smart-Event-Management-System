package com.smart.event.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class RoleBasedAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if ("ROLE_ADMIN".equals(role)) {
                response.sendRedirect("/admin/dashboard");
                return;
            }
            if ("ROLE_ORGANIZER".equals(role)) {
                response.sendRedirect("/organizer/dashboard");
                return;
            }
            if ("ROLE_STUDENT".equals(role)) {
                response.sendRedirect("/student/home");
                return;
            }
        }
        response.sendRedirect("/");
    }
}