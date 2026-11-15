package com.finsight.service;

import com.finsight.model.Symbol;
import com.finsight.repository.SymbolRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SymbolService {

    private final SymbolRepository symbolRepository;

    public SymbolService(SymbolRepository symbolRepository) {
        this.symbolRepository = symbolRepository;
    }

    public List<Symbol> findAll() {
        return symbolRepository.findAll();
    }
}
