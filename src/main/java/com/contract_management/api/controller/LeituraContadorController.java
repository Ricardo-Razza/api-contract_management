package com.contract_management.api.controller;

import com.contract_management.api.dto.request.LeituraContadorRequestDTO;
import com.contract_management.api.dto.response.LeituraContadorResponseDTO;
import com.contract_management.api.service.LeituraContadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping
    @Operation(summary = "Lança a leitura de contador de uma impressora com cálculo automático de franquia e excedente")
    public ResponseEntity<LeituraContadorResponseDTO> lancarLeitura(@Valid @RequestBody LeituraContadorRequestDTO dto) {
        LeituraContadorResponseDTO criada = leituraService.lancarLeitura(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }
}
