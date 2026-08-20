package com.logistics.ai.incident.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller kiểm tra và đối soát cấu hình hệ thống đa môi trường (Profiles).
 * Cung cấp endpoint GET /api/v1/incident/config để xác minh profile LLM hiện tại.
 */
@RestController
@RequestMapping("/api/v1/incident")
public class SystemConfigController {

    private final Environment environment;

    @Value("${spring.application.name:ai-logistics-incident-reporter}")
    private String applicationName;

    @Value("${app.llm.provider:Unknown}")
    private String llmProvider;

    @Value("${app.llm.model-name:Unknown}")
    private String llmModelName;

    @Value("${app.llm.endpoint:Unknown}")
    private String llmEndpoint;

    public SystemConfigController(Environment environment) {
        this.environment = environment;
    }

    /**
     * Endpoint lấy thông tin cấu hình profile và model LLM đang kích hoạt.
     * @return JSON chứa thông tin chi tiết về profile và model AI.
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getSystemConfig() {
        Map<String, Object> config = new LinkedHashMap<>();

        String[] activeProfiles = environment.getActiveProfiles();
        String currentProfile = (activeProfiles != null && activeProfiles.length > 0)
                ? String.join(", ", activeProfiles)
                : "default (local)";

        config.put("status", "SUCCESS");
        config.put("applicationName", applicationName);
        config.put("activeProfile", currentProfile);
        config.put("llmProvider", llmProvider);
        config.put("activeModel", llmModelName);
        config.put("endpoint", llmEndpoint);
        config.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(config);
    }
}
