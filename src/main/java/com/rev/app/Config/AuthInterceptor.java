package com.rev.app.Config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        // Defensive check: explicitly skip interceptor logic for public pages
        if (uri.equals("/") || uri.equals("/login") || uri.equals("/register") || uri.equals("/forgot-password")) {
            return true;
        }

        HttpSession session = request.getSession();
        if (session.getAttribute("loggedInUser") == null) {
            response.sendRedirect("/login");
            return false;
        }
        return true;
    }
}
