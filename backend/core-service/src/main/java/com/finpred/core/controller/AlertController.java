package com.finpred.core.controller;

import com.finpred.core.dto.AlertDTO;
import com.finpred.core.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller de Alertas Inteligentes.
 * 
 * Endpoint único que retorna todos os alertas financeiros gerados
 * dinamicamente com base nos dados do usuário (produtos, transações).
 */
@RestController
@RequestMapping("/api/core")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    /**
     * Retorna alertas inteligentes do usuário.
     * Ordenados por severidade: danger > warning > info > success.
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<AlertDTO>> getAlerts(@RequestHeader("X-User-Id") Long userId) {
        List<AlertDTO> alerts = alertService.generateAlerts(userId);
        return ResponseEntity.ok(alerts);
    }
}
