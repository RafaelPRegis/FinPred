package com.finpred.core.controller;

import com.finpred.core.dto.CashFlowProjection;
import com.finpred.core.dto.DreReport;
import com.finpred.core.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller de Relatórios Financeiros — Fase 6.
 * 
 * Endpoints:
 * - GET /api/core/reports/dre          → DRE do período
 * - GET /api/core/reports/cashflow     → Fluxo de Caixa Projetado
 */
@RestController
@RequestMapping("/api/core/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    /**
     * Gera DRE para o período especificado.
     * Se não informados, usa os últimos 6 meses.
     */
    @GetMapping("/dre")
    public ResponseEntity<DreReport> getDre(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        if (start == null) start = LocalDate.now().minusMonths(6).withDayOfMonth(1);
        if (end == null) end = LocalDate.now();

        log.info("GET /reports/dre — userId={}, start={}, end={}", userId, start, end);
        DreReport dre = reportService.generateDre(userId, start, end);
        return ResponseEntity.ok(dre);
    }

    /**
     * Gera projeção de fluxo de caixa (6 meses históricos + 6 projetados).
     */
    @GetMapping("/cashflow")
    public ResponseEntity<CashFlowProjection> getCashFlowProjection(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("GET /reports/cashflow — userId={}", userId);
        CashFlowProjection projection = reportService.generateCashFlowProjection(userId);
        return ResponseEntity.ok(projection);
    }
}
