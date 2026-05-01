package com.finpred.core.service;

import com.finpred.core.dto.ImportRowDto;
import com.finpred.core.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@Profile("prod")
@RequiredArgsConstructor
public class ProdImportProcessor implements ImportProcessor {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void process(List<ImportRowDto> rows) {
        log.info("[PROD] Enviando {} linhas para a fila RabbitMQ...", rows.size());
        for (ImportRowDto row : rows) {
            rabbitTemplate.convertAndSend(RabbitMQConfig.IMPORT_QUEUE, row);
        }
        log.info("[PROD] Lote enviado ao RabbitMQ com sucesso.");
    }
}
