package com.usellm.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ReferrerPolicyHeaderFilter extends OncePerRequestFilter {

    static final String REFERRER_POLICY_HEADER = "Referrer-Policy";
    static final String REFERRER_POLICY_VALUE = "no-referrer";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        response.setHeader(REFERRER_POLICY_HEADER, REFERRER_POLICY_VALUE);
        filterChain.doFilter(request, response);
    }
}
