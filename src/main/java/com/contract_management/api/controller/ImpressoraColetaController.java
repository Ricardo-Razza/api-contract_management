package com.contract_management.api.controller;

import com.contract_management.api.dto.request.IniciarColetaRequestDTO;
import com.contract_management.api.dto.response.ColetaProgressoDTO;
import com.contract_management.api.dto.response.ColetaSessaoDTO;
import com.contract_management.api.service.ColetorImpressoraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.Map;

@RestController
@RequestMapping("/impressoras/coletas")
@RequiredArgsConstructor
@Tag(name = "Coleta Automática de Impressoras", description = "Serviços de captura de prints de contadores e sincronização automática")
public class ImpressoraColetaController {

    private final ColetorImpressoraService coletorService;

    @PostMapping("/iniciar")
    @Operation(summary = "Inicia uma sessão assíncrona de coleta e captura de telas de contadores")
    public ResponseEntity<ColetaProgressoDTO> iniciarColeta(@Valid @RequestBody IniciarColetaRequestDTO request) {
        return ResponseEntity.ok(coletorService.iniciarColeta(request));
    }

    @GetMapping("/ativa")
    @Operation(summary = "Obtém o progresso em tempo real da sessão atualmente em andamento")
    public ResponseEntity<ColetaProgressoDTO> obterProgressoAtivo() {
        ColetaProgressoDTO progresso = coletorService.obterProgressoAtivo();
        if (progresso == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(progresso);
    }

    @GetMapping("/ultima")
    @Operation(summary = "Obtém a última sessão de coleta realizada")
    public ResponseEntity<ColetaSessaoDTO> obterUltimaSessao() {
        ColetaSessaoDTO sessao = coletorService.obterUltimaSessao();
        if (sessao == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(sessao);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtém os detalhes e a lista de equipamentos de uma sessão de coleta")
    public ResponseEntity<ColetaSessaoDTO> obterSessaoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(coletorService.obterSessao(id));
    }

    @GetMapping("/{id}/download-zip")
    @Operation(summary = "Baixa o pacote compactado em ZIP com todos os prints da sessão")
    public ResponseEntity<Resource> baixarZip(@PathVariable Long id) {
        File zipFile = coletorService.gerarZipSessao(id);
        Resource resource = new FileSystemResource(zipFile);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFile.getName() + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(resource);
    }

    @GetMapping("/{id}/imagem/{nomeArquivo}")
    @Operation(summary = "Serve a imagem do print de contador para visualização na interface")
    public ResponseEntity<byte[]> obterImagem(@PathVariable Long id, @PathVariable String nomeArquivo) {
        byte[] imagemBytes = coletorService.obterImagem(id, nomeArquivo);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                .body(imagemBytes);
    }

    @PostMapping("/{id}/aplicar-leituras")
    @Operation(summary = "Aplica os contadores coletados automaticamente nas leituras do mês no sistema")
    public ResponseEntity<Map<String, Object>> aplicarLeituras(@PathVariable Long id) {
        int gravadas = coletorService.aplicarLeituras(id);
        return ResponseEntity.ok(Map.of(
                "status", "SUCESSO",
                "leiturasGravadas", gravadas,
                "mensagem", gravadas + " contadores foram sincronizados com sucesso nas leituras do mês."
        ));
    }

    @PostMapping("/{id}/recoletar-falhas")
    @Operation(summary = "Tenta reconectar e capturar apenas os equipamentos que falharam ou estavam offline")
    public ResponseEntity<Map<String, String>> recoletarFalhas(@PathVariable Long id) {
        coletorService.recoletarFalhas(id);
        return ResponseEntity.ok(Map.of(
                "status", "SUCESSO",
                "mensagem", "Recoleta dos equipamentos offline iniciada em segundo plano."
        ));
    }

    @PostMapping("/itens/{itemId}/recoletar")
    @Operation(summary = "Tenta reconectar e capturar um equipamento específico")
    public ResponseEntity<Map<String, String>> recoletarItem(@PathVariable Long itemId) {
        coletorService.recoletarItem(itemId);
        return ResponseEntity.ok(Map.of(
                "status", "SUCESSO",
                "mensagem", "Tentativa de coleta iniciada para o equipamento."
        ));
    }
}
