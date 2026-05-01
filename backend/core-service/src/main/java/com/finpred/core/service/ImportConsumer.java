package com.finpred.core.service;

import com.finpred.core.config.RabbitMQConfig;
import com.finpred.core.dto.ImportRowDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("prod")
@RequiredArgsConstructor
public class ImportConsumer {

    private final ImportDataService importDataService;

    @RabbitListener(queues = RabbitMQConfig.IMPORT_QUEUE)
    public void consume(ImportRowDto row) {
        try {
            log.info("[PROD Consumer] Recebida linha da fila: {}", row.getDescription());
            importDataService.saveRow(row);
        } catch (Exception e) {
            log.error("[PROD Consumer] Erro ao processar linha da fila", e);
        }
    }
}
