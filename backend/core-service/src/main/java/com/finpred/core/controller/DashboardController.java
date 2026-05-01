package com.finpred.core.controller;

import com.finpred.core.dto.DashboardSummary;
import com.finpred.core.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/core")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardSummary> getDashboard(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(dashboardService.getSummary(userId));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "core-service",
                "status", "UP",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
