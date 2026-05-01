package com.finpred.core.service;

import com.finpred.core.dto.ImportRowDto;
import java.util.List;

public interface ImportProcessor {
    void process(List<ImportRowDto> rows);
}
