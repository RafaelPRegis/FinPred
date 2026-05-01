package com.finpred.core.service;

import com.finpred.core.dto.ImportRowDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@Profile("dev")
@RequiredArgsConstructor
public class DevImportProcessor implements ImportProcessor {

    private final ImportDataService importDataService;

    @Async
    @Override
    public void process(List<ImportRowDto> rows) {
        log.info("[DEV] Iniciando processamento assíncrono em memória de {} linhas...", rows.size());
        for (ImportRowDto row : rows) {
            try {
                importDataService.saveRow(row);
            } catch (Exception e) {
                log.error("[DEV] Erro ao processar linha: {}", row, e);
            }
        }
        log.info("[DEV] Processamento de importação concluído!");
    }
}
