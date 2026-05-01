package com.finpred.core.service;

import com.finpred.core.dto.ImportRowDto;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CsvParserService {

    public List<ImportRowDto> parse(MultipartFile file, Long userId) throws Exception {
        List<ImportRowDto> rows = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build()) {

            List<String[]> allData = csvReader.readAll();
            for (String[] row : allData) {
                if (row.length < 4) continue; // Pular linhas incompletas

                ImportRowDto dto = ImportRowDto.builder()
                        .userId(userId)
                        .date(LocalDate.parse(row[0], dateFormatter))
                        .description(row[1])
                        .amount(new BigDecimal(row[2].replace(",", ".")))
                        .category(row[3])
                        .build();

                // Colunas opcionais para produto (se existirem na posição 4 e 5)
                if (row.length > 4 && row[4] != null && !row[4].trim().isEmpty()) {
                    dto.setProductName(row[4].trim());
                }
                if (row.length > 5 && row[5] != null && !row[5].trim().isEmpty()) {
                    dto.setProductSalePrice(new BigDecimal(row[5].replace(",", ".")));
                }

                rows.add(dto);
            }
        }

        return rows;
    }
}
