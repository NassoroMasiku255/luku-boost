package com.lukuboost.electricityloan.admin.controller;

import com.lukuboost.electricityloan.audit.service.AuditService;
import com.lukuboost.electricityloan.common.dto.ApiResponse;
import com.lukuboost.electricityloan.common.entity.ApiClient;
import com.lukuboost.electricityloan.common.exception.BusinessException;
import com.lukuboost.electricityloan.common.exception.ResourceNotFoundException;
import com.lukuboost.electricityloan.common.repository.ApiClientRepository;
import com.lukuboost.electricityloan.common.security.AdminPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/api-clients")
@RequiredArgsConstructor
@Tag(name = "Admin API Clients", description = "Manage API clients for Customer API access")
public class AdminApiClientController {

    private final ApiClientRepository apiClientRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @GetMapping
    @Operation(summary = "List all API clients")
    @PreAuthorize("hasAuthority('CONFIG_VIEW') or hasAuthority('CONFIG_MANAGE')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listClients() {
        List<Map<String, Object>> clients = apiClientRepository.findAll().stream()
                .map(this::toSafeMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(clients));
    }

    @PostMapping
    @Operation(summary = "Create a new API client and return credentials")
    @PreAuthorize("hasAuthority('CONFIG_UPDATE') or hasAuthority('CONFIG_MANAGE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createClient(
            @RequestBody Map<String, Object> body, Authentication authentication) {

        String clientName = (String) body.get("clientName");
        if (clientName == null || clientName.isBlank()) {
            throw new BusinessException("INVALID_CLIENT_NAME", "Client name is required.", HttpStatus.BAD_REQUEST);
        }

        String description = (String) body.get("description");
        String allowedIps = (String) body.get("allowedIps");
        Integer rateLimit = body.containsKey("rateLimitPerMinute")
                ? ((Number) body.get("rateLimitPerMinute")).intValue() : 60;

        // Generate client ID and API key
        String clientId = generateClientId();
        String rawApiKey = generateApiKey();

        ApiClient client = ApiClient.builder()
                .clientName(clientName)
                .clientId(clientId)
                .apiKeyHash(passwordEncoder.encode(rawApiKey))
                .description(description)
                .allowedIps(allowedIps)
                .rateLimitPerMinute(rateLimit)
                .enabled(true)
                .build();

        if (authentication.getPrincipal() instanceof AdminPrincipal admin) {
            client.setCreatedBy(admin.userId());
        }

        client = apiClientRepository.save(client);

        auditService.logAction("API_CLIENT_CREATED", "API_CLIENT", client.getId().toString(),
                null, clientName, "Created API client: " + clientName);

        // Return the credentials ONCE — they cannot be retrieved again
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", client.getId());
        result.put("clientName", client.getClientName());
        result.put("clientId", clientId);
        result.put("apiKey", rawApiKey);
        result.put("description", client.getDescription());
        result.put("allowedIps", client.getAllowedIps());
        result.put("rateLimitPerMinute", client.getRateLimitPerMinute());
        result.put("enabled", client.getEnabled());
        result.put("message", "Save these credentials securely. The API key will NOT be shown again.");

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update API client settings (not credentials)")
    @PreAuthorize("hasAuthority('CONFIG_UPDATE') or hasAuthority('CONFIG_MANAGE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateClient(
            @PathVariable UUID id, @RequestBody Map<String, Object> body) {

        ApiClient client = apiClientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ApiClient", id.toString()));

        if (body.containsKey("clientName")) client.setClientName((String) body.get("clientName"));
        if (body.containsKey("description")) client.setDescription((String) body.get("description"));
        if (body.containsKey("allowedIps")) client.setAllowedIps((String) body.get("allowedIps"));
        if (body.containsKey("enabled")) client.setEnabled((Boolean) body.get("enabled"));
        if (body.containsKey("rateLimitPerMinute"))
            client.setRateLimitPerMinute(((Number) body.get("rateLimitPerMinute")).intValue());

        client = apiClientRepository.save(client);

        auditService.logAction("API_CLIENT_UPDATED", "API_CLIENT", id.toString(),
                null, null, "Updated API client: " + client.getClientName());

        return ResponseEntity.ok(ApiResponse.success(toSafeMap(client)));
    }

    @PostMapping("/{id}/regenerate-key")
    @Operation(summary = "Regenerate API key for a client")
    @PreAuthorize("hasAuthority('CONFIG_UPDATE') or hasAuthority('CONFIG_MANAGE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> regenerateKey(@PathVariable UUID id) {

        ApiClient client = apiClientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ApiClient", id.toString()));

        String newApiKey = generateApiKey();
        client.setApiKeyHash(passwordEncoder.encode(newApiKey));
        apiClientRepository.save(client);

        auditService.logAction("API_KEY_REGENERATED", "API_CLIENT", id.toString(),
                null, null, "Regenerated API key for: " + client.getClientName());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("clientId", client.getClientId());
        result.put("apiKey", newApiKey);
        result.put("message", "Save this key securely. It will NOT be shown again.");

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an API client")
    @PreAuthorize("hasAuthority('CONFIG_UPDATE') or hasAuthority('CONFIG_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> deleteClient(@PathVariable UUID id) {
        ApiClient client = apiClientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ApiClient", id.toString()));

        apiClientRepository.delete(client);

        auditService.logAction("API_CLIENT_DELETED", "API_CLIENT", id.toString(),
                client.getClientName(), null, "Deleted API client: " + client.getClientName());

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // Map client to safe representation (no key hash)
    private Map<String, Object> toSafeMap(ApiClient client) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", client.getId());
        map.put("clientName", client.getClientName());
        map.put("clientId", client.getClientId());
        map.put("description", client.getDescription());
        map.put("allowedIps", client.getAllowedIps());
        map.put("rateLimitPerMinute", client.getRateLimitPerMinute());
        map.put("enabled", client.getEnabled());
        map.put("lastUsedAt", client.getLastUsedAt());
        map.put("createdAt", client.getCreatedAt());
        return map;
    }

    private String generateClientId() {
        return "lkb_" + randomHex(16);
    }

    private String generateApiKey() {
        return "lkb_key_" + randomHex(32);
    }

    private String randomHex(int byteLength) {
        byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
