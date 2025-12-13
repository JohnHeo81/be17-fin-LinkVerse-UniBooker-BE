package org.example.unibooker.config.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.unibooker.domain.user.service.RateLimitService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Rate Limiting 필터
 * - API 호출 횟수 제한
 * - 제한 초과 시 429 응답
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // OPTIONS 요청은 통과
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Rate Limit 제외 경로
        String uri = request.getRequestURI();
        if (isExcludedPath(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 식별자 추출 (IP 기반)
        String identifier = getClientIp(request);

        // 엔드포인트 카테고리 결정
        String category = getEndpointCategory(uri);

        // Rate Limit 확인
        if (!rateLimitService.isAllowed(identifier, category)) {
            sendRateLimitResponse(response, identifier, category);
            return;
        }

        // Rate Limit 헤더 추가
        int remaining = rateLimitService.getRemainingCount(identifier, category);
        long resetTime = rateLimitService.getRemainingTime(identifier, category);
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetTime));

        filterChain.doFilter(request, response);
    }

    /**
     * Rate Limit 제외 경로 확인
     */
    private boolean isExcludedPath(String uri) {
        return uri.startsWith("/swagger") ||
                uri.startsWith("/v3/api-docs") ||
                uri.startsWith("/actuator") ||
                uri.startsWith("/ws");
    }

    /**
     * 엔드포인트 카테고리 결정
     */
    private String getEndpointCategory(String uri) {
        if (uri.contains("/login")) {
            return "login";
        }
        if (uri.contains("/reservation") || uri.contains("/queues")) {
            return "reservation";
        }
        return "general";
    }

    /**
     * 클라이언트 IP 추출
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 429 Too Many Requests 응답
     */
    private void sendRateLimitResponse(HttpServletResponse response,
                                       String identifier,
                                       String category) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");

        long retryAfter = rateLimitService.getRemainingTime(identifier, category);

        Map<String, Object> body = new HashMap<>();
        body.put("code", 20010);
        body.put("message", "요청 횟수를 초과했습니다. " + retryAfter + "초 후에 다시 시도해주세요.");
        body.put("isSuccess", false);
        body.put("retryAfter", retryAfter);

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}