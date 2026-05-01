package com.finpred.core.controller;

import com.finpred.core.dto.AcquirerRateRequest;
import com.finpred.core.dto.AcquirerRateResponse;
import com.finpred.core.service.AcquirerRateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/core/acquirers")
@RequiredArgsConstructor
public class AcquirerRateController {

    private final AcquirerRateService acquirerRateService;

    @GetMapping
    public ResponseEntity<List<AcquirerRateResponse>> getAll(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(acquirerRateService.findByUser(userId));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestHeader("X-User-Id") Long userId,
                                    @Valid @RequestBody AcquirerRateRequest request) {
        try {
            AcquirerRateResponse response = acquirerRateService.create(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestHeader("X-User-Id") Long userId,
                                    @Valid @RequestBody AcquirerRateRequest request) {
        try {
            return ResponseEntity.ok(acquirerRateService.update(id, userId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        try {
            acquirerRateService.delete(id, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
