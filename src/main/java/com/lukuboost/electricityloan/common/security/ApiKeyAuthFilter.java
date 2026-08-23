package com.lukuboost.electricityloan.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lukuboost.electricityloan.common.dto.ApiResponse;
import com.lukuboost.electricityloan.common.entity.ApiClient;
import com.lukuboost.electricityloan.common.repository.ApiClientRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String CLIENT_ID_HEADER = "X-Client-Id";
    private static final String API_KEY_HEADER = "X-Api-Key";

    private final ApiClientRepository apiClientRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthFilter(ApiClientRepository apiClientRepository,
                            @Lazy PasswordEncoder passwordEncoder,
                            ObjectMapper objectMapper) {
        this.apiClientRepository = apiClientRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String clientId = request.getHeader(CLIENT_ID_HEADER);
        String apiKey = request.getHeader(API_KEY_HEADER);

        if (clientId == null || clientId.isBlank() || apiKey == null || apiKey.isBlank()) {
            writeError(response, HttpStatus.UNAUTHORIZED, "MISSING_CREDENTIALS",
                    "X-Client-Id and X-Api-Key headers are required.");
            return;
        }

        Optional<ApiClient> clientOpt = apiClientRepository.findByClientIdAndEnabledTrue(clientId);

        if (clientOpt.isEmpty()) {
            log.warn("API key auth failed: unknown or disabled client_id={}", clientId);
            writeError(response, HttpStatus.UNAUTHORIZED, "INVALID_CLIENT",
                    "Invalid or disabled API client.");
            return;
        }

        ApiClient client = clientOpt.get();

        if (!passwordEncoder.matches(apiKey, client.getApiKeyHash())) {
            log.warn("API key auth failed: invalid api_key for client_id={}", clientId);
            writeError(response, HttpStatus.UNAUTHORIZED, "INVALID_API_KEY",
                    "Invalid API key.");
            return;
        }

        // Check IP whitelist if configured
        String allowedIps = client.getAllowedIps();
        if (allowedIps != null && !allowedIps.isBlank()) {
            String requestIp = getClientIp(request);
            boolean allowed = false;
            for (String ip : allowedIps.split(",")) {
                if (ip.trim().equals(requestIp) || ip.trim().equals("*")) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                log.warn("API key auth failed: IP {} not allowed for client_id={}", requestIp, clientId);
                writeError(response, HttpStatus.FORBIDDEN, "IP_NOT_ALLOWED",
                        "Request from this IP address is not allowed.");
                return;
            }
        }

        // Update last used timestamp (async-safe: don't block the request)
        try {
            client.setLastUsedAt(Instant.now());
            apiClientRepository.save(client);
        } catch (Exception e) {
            log.debug("Failed to update lastUsedAt for client_id={}", clientId);
        }

        // Set client info as request attributes for downstream use
        request.setAttribute("apiClientId", client.getId());
        request.setAttribute("apiClientName", client.getClientName());

        log.debug("API key auth success for client_id={}, name={}", clientId, client.getClientName());

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Only filter Customer API requests
        return !path.startsWith("/api/v1/customer/");
    }

    private void writeError(HttpServletResponse response, HttpStatus status,
                             String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<?> errorResponse = ApiResponse.error(code, message);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
