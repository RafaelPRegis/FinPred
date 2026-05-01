package com.finpred.core.controller;

import com.finpred.core.dto.TransactionRequest;
import com.finpred.core.dto.TransactionResponse;
import com.finpred.core.model.TransactionType;
import com.finpred.core.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/core/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAll(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        if (type != null) {
            return ResponseEntity.ok(transactionService.findByUserAndType(userId, type));
        }
        if (start != null && end != null) {
            return ResponseEntity.ok(transactionService.findByUserAndDateRange(userId, start, end));
        }
        return ResponseEntity.ok(transactionService.findByUser(userId));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestHeader("X-User-Id") Long userId,
                                    @Valid @RequestBody TransactionRequest request) {
        try {
            TransactionResponse response = transactionService.create(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestHeader("X-User-Id") Long userId,
                                    @Valid @RequestBody TransactionRequest request) {
        try {
            return ResponseEntity.ok(transactionService.update(id, userId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        try {
            transactionService.delete(id, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
