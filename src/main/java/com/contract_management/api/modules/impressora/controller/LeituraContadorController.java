package com.contract_management.api.modules.impressora.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.contract_management.api.modules.impressora.dto.request.LeituraContadorRequestDTO;
import com.contract_management.api.modules.impressora.dto.response.ItemGradeLeituraDTO;
import com.contract_management.api.modules.impressora.dto.response.LeituraContadorResponseDTO;
import com.contract_management.api.modules.impressora.service.medicao.LeituraContadorService;

@RestController
@RequestMapping("/impressoras/leituras")
@RequiredArgsConstructor
@Tag(name = "Leituras de Impressoras", description = "Lançamento de contadores e apuração mensal de cópias e excedentes")
public class LeituraContadorController {

    private final LeituraContadorService leituraService;

    @GetMapping
    @Operation(summary = "Lista todas as leituras apuradas para uma competência (mês e ano)")
    public ResponseEntity<List<LeituraContadorResponseDTO>> listarPorMesEAno(
            @RequestParam Integer mes,
            @RequestParam Integer ano) {
        return ResponseEntity.ok(leituraService.listarPorMesEAno(mes, ano));
    }

    @GetMapping("/grade")
    @Operation(summary = "Retorna a grade completa de todas as impressoras ativas com suas leituras da competência")
    public ResponseEntity<List<ItemGradeLeituraDTO>> obterGradeLeituras(
            @RequestParam Integer mes,
            @RequestParam Integer ano) {
        return ResponseEntity.ok(leituraService.obterGradeLeituras(mes, ano));
    }

    @PostMapping
    @Operation(summary = "Lança a leitura de contador de uma impressora com cálculo automático de franquia e excedente")
    public ResponseEntity<LeituraContadorResponseDTO> lancarLeitura(@Valid @RequestBody LeituraContadorRequestDTO dto) {
        LeituraContadorResponseDTO criada = leituraService.lancarLeitura(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @PostMapping("/lote")
    @Operation(summary = "Salva leituras de contador em lote para múltiplas impressoras")
    public ResponseEntity<List<LeituraContadorResponseDTO>> salvarEmLote(@Valid @RequestBody List<LeituraContadorRequestDTO> dtos) {
        return ResponseEntity.ok(leituraService.salvarEmLote(dtos));
    }
}