package com.restaurant.server.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * PHASE 9: per-request access log. Emits one log line per request with method,
 * path, status, duration, and remote IP. Avoids logging bodies.
 */
@Component
public class RequestLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("ACCESS");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long dur = System.currentTimeMillis() - start;
            log.info("{} {} -> {} ({} ms) ip={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    dur,
                    request.getRemoteAddr());
        }
    }
}