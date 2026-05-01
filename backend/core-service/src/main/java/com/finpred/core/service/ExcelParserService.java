package com.finpred.core.service;

import com.finpred.core.dto.ImportRowDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ExcelParserService {

    public List<ImportRowDto> parse(MultipartFile file, Long userId) throws Exception {
        List<ImportRowDto> rows = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell dateCell = row.getCell(0);
                Cell descCell = row.getCell(1);
                Cell amountCell = row.getCell(2);
                Cell catCell = row.getCell(3);

                if (dateCell == null || amountCell == null) continue;

                LocalDate date = dateCell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                String desc = descCell != null ? descCell.getStringCellValue() : "";
                BigDecimal amount = BigDecimal.valueOf(amountCell.getNumericCellValue());
                String cat = catCell != null ? catCell.getStringCellValue() : "Geral";

                ImportRowDto dto = ImportRowDto.builder()
                        .userId(userId)
                        .date(date)
                        .description(desc)
                        .amount(amount)
                        .category(cat)
                        .build();

                // Colunas opcionais para Produto
                Cell prodNameCell = row.getCell(4);
                if (prodNameCell != null && prodNameCell.getCellType() == CellType.STRING) {
                    dto.setProductName(prodNameCell.getStringCellValue());
                }

                Cell prodSaleCell = row.getCell(5);
                if (prodSaleCell != null && prodSaleCell.getCellType() == CellType.NUMERIC) {
                    dto.setProductSalePrice(BigDecimal.valueOf(prodSaleCell.getNumericCellValue()));
                }

                rows.add(dto);
            }
        }

        return rows;
    }
}
