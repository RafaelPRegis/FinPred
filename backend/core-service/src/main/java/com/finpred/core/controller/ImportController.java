package com.finpred.core.controller;

import com.finpred.core.dto.ImportRowDto;
import com.finpred.core.service.CsvParserService;
import com.finpred.core.service.ExcelParserService;
import com.finpred.core.service.ImportProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/core/import")
@RequiredArgsConstructor
public class ImportController {

    private final CsvParserService csvParserService;
    private final ExcelParserService excelParserService;
    private final ImportProcessor importProcessor;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestHeader("X-User-Id") Long userId,
                                        @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Arquivo vazio."));
        }

        try {
            List<ImportRowDto> rows;
            String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";

            if (fileName.endsWith(".csv")) {
                rows = csvParserService.parse(file, userId);
            } else if (fileName.endsWith(".xlsx")) {
                rows = excelParserService.parse(file, userId);
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Formato de arquivo não suportado. Use .csv ou .xlsx"));
            }

            // Envia para processamento assíncrono (RabbitMQ ou @Async em memória)
            importProcessor.process(rows);

            return ResponseEntity.ok(Map.of(
                    "message", "Arquivo aceito e enviado para processamento.",
                    "rowsDetected", rows.size()
            ));

        } catch (Exception e) {
            log.error("Erro ao processar arquivo: ", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Erro ao ler arquivo: " + e.getMessage()));
        }
    }

    @GetMapping("/template/csv")
    public ResponseEntity<byte[]> downloadCsvTemplate() {
        String csvContent = "Data,Descricao,Valor,Categoria,NomeProduto(Opcional),PrecoVendaProduto(Opcional)\n" +
                "2026-04-01,Venda Balcão,150.50,Vendas,Produto Teste,150.50\n" +
                "2026-04-02,Conta de Luz,-300.00,Energia,,\n";

        byte[] bytes = csvContent.getBytes();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "finpred_template_importacao.csv");

        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}
