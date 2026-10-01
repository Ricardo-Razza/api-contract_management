package com.contract_management.api.modules.contrato.controller;

import com.contract_management.api.modules.contrato.dto.response.DocumentoAnexoResponseDTO;
import com.contract_management.api.modules.contrato.model.DocumentoAnexo;
import com.contract_management.api.modules.contrato.service.DocumentoAnexoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/anexos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DocumentoAnexoController {

    private final DocumentoAnexoService documentoAnexoService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoAnexoResponseDTO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "tipoDocumento", defaultValue = "OUTRO") String tipoDocumento,
            @RequestParam(value = "descricao", required = false) String descricao,
            @RequestParam(value = "contratoId", required = false) Long contratoId,
            @RequestParam(value = "ataId", required = false) Long ataId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentoAnexoService.salvar(file, tipoDocumento, descricao, contratoId, ataId));
    }

    @GetMapping("/contrato/{contratoId}")
    public ResponseEntity<List<DocumentoAnexoResponseDTO>> listarPorContrato(@PathVariable Long contratoId) {
        return ResponseEntity.ok(documentoAnexoService.listarPorContrato(contratoId));
    }

    @GetMapping("/ata/{ataId}")
    public ResponseEntity<List<DocumentoAnexoResponseDTO>> listarPorAta(@PathVariable Long ataId) {
        return ResponseEntity.ok(documentoAnexoService.listarPorAta(ataId));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        DocumentoAnexo doc = documentoAnexoService.buscarEntidadePorId(id);
        Resource resource = documentoAnexoService.carregarArquivoComoRecurso(doc);
        String encodedFilename = URLEncoder.encode(doc.getNomeOriginal(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getNomeOriginal() + "\"; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }

    @GetMapping("/{id}/visualizar")
    public ResponseEntity<Resource> visualizar(@PathVariable Long id) {
        DocumentoAnexo doc = documentoAnexoService.buscarEntidadePorId(id);
        Resource resource = documentoAnexoService.carregarArquivoComoRecurso(doc);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (doc.getContentType() != null && !doc.getContentType().isBlank() && !doc.getContentType().equalsIgnoreCase("application/octet-stream")) {
            try {
                mediaType = MediaType.parseMediaType(doc.getContentType());
            } catch (Exception ignored) {}
        } else if (doc.getNomeOriginal() != null) {
            String lower = doc.getNomeOriginal().toLowerCase();
            if (lower.endsWith(".pdf")) {
                mediaType = MediaType.APPLICATION_PDF;
            } else if (lower.endsWith(".png")) {
                mediaType = MediaType.IMAGE_PNG;
            } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                mediaType = MediaType.IMAGE_JPEG;
            } else if (lower.endsWith(".txt")) {
                mediaType = MediaType.TEXT_PLAIN;
            }
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getNomeOriginal() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        documentoAnexoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
