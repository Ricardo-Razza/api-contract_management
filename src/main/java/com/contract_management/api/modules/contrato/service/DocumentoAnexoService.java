package com.contract_management.api.modules.contrato.service;

import com.contract_management.api.common.exception.BusinessException;
import com.contract_management.api.common.exception.EntityNotFoundException;
import com.contract_management.api.modules.contrato.dto.response.DocumentoAnexoResponseDTO;
import com.contract_management.api.modules.contrato.model.DocumentoAnexo;
import com.contract_management.api.modules.contrato.repository.DocumentoAnexoRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentoAnexoService {

    private final DocumentoAnexoRepository documentoAnexoRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.upload.dir:uploads/documentos}")
    private String uploadDirConfig;

    private Path uploadPath;

    @PostConstruct
    public void init() {
        try {
            // Auto-criação da tabela caso ddl-auto seja 'none'
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS documento_anexo (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    nome_original VARCHAR(255) NOT NULL,
                    nome_arquivo VARCHAR(255) NOT NULL,
                    tipo_documento VARCHAR(50) NOT NULL,
                    content_type VARCHAR(100),
                    tamanho_bytes BIGINT,
                    descricao VARCHAR(500),
                    contrato_id BIGINT,
                    ata_id BIGINT,
                    criado_em DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_doc_contrato (contrato_id),
                    INDEX idx_doc_ata (ata_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
            """);
            log.info("Tabela documento_anexo inicializada com sucesso.");

            // Inicialização do diretório de armazenamento de arquivos
            this.uploadPath = Paths.get(uploadDirConfig).toAbsolutePath().normalize();
            Files.createDirectories(this.uploadPath);
            log.info("Diretório de uploads configurado em: {}", this.uploadPath);
        } catch (Exception e) {
            log.error("Erro ao inicializar serviço de documentos anexos: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public DocumentoAnexoResponseDTO salvar(MultipartFile file, String tipoDocumento, String descricao, Long contratoId, Long ataId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Nenhum arquivo enviado ou o arquivo está vazio.");
        }

        if (contratoId == null && ataId == null) {
            throw new BusinessException("O documento deve estar vinculado a um Contrato ou a uma Ata.");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "documento");
        String extensao = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extensao = originalFilename.substring(dotIndex);
        }

        String nomeArmazenamento = UUID.randomUUID().toString() + extensao;
        Path targetLocation = this.uploadPath.resolve(nomeArmazenamento);

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessException("Falha ao salvar o arquivo no disco: " + ex.getMessage());
        }

        String detectedContentType = file.getContentType();
        if ((detectedContentType == null || detectedContentType.isBlank() || detectedContentType.equalsIgnoreCase("application/octet-stream")) && !extensao.isBlank()) {
            String extLower = extensao.toLowerCase();
            if (extLower.equals(".pdf")) detectedContentType = "application/pdf";
            else if (extLower.equals(".png")) detectedContentType = "image/png";
            else if (extLower.equals(".jpg") || extLower.equals(".jpeg")) detectedContentType = "image/jpeg";
            else if (extLower.equals(".txt")) detectedContentType = "text/plain";
            else if (extLower.equals(".doc")) detectedContentType = "application/msword";
            else if (extLower.equals(".docx")) detectedContentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }

        DocumentoAnexo doc = DocumentoAnexo.builder()
                .nomeOriginal(originalFilename)
                .nomeArquivo(nomeArmazenamento)
                .tipoDocumento(tipoDocumento != null && !tipoDocumento.isBlank() ? tipoDocumento : "OUTRO")
                .contentType(detectedContentType)
                .tamanhoBytes(file.getSize())
                .descricao(descricao)
                .contratoId(contratoId)
                .ataId(ataId)
                .build();

        doc = documentoAnexoRepository.save(doc);
        return toDTO(doc);
    }

    @Transactional(readOnly = true)
    public List<DocumentoAnexoResponseDTO> listarPorContrato(Long contratoId) {
        return documentoAnexoRepository.findByContratoIdOrderByCriadoEmDesc(contratoId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentoAnexoResponseDTO> listarPorAta(Long ataId) {
        return documentoAnexoRepository.findByAtaIdOrderByCriadoEmDesc(ataId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DocumentoAnexo buscarEntidadePorId(Long id) {
        return documentoAnexoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Documento anexo", id));
    }

    @Transactional(readOnly = true)
    public Resource carregarArquivoComoRecurso(DocumentoAnexo doc) {
        try {
            Path filePath = this.uploadPath.resolve(doc.getNomeArquivo()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new EntityNotFoundException("Arquivo físico não encontrado para o documento ID: " + doc.getId());
            }
        } catch (MalformedURLException ex) {
            throw new BusinessException("Caminho do arquivo inválido: " + ex.getMessage());
        }
    }

    @Transactional
    public void deletar(Long id) {
        DocumentoAnexo doc = buscarEntidadePorId(id);
        try {
            Path filePath = this.uploadPath.resolve(doc.getNomeArquivo()).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Não foi possível excluir o arquivo físico {}: {}", doc.getNomeArquivo(), e.getMessage());
        }
        documentoAnexoRepository.delete(doc);
    }

    public DocumentoAnexoResponseDTO toDTO(DocumentoAnexo doc) {
        return DocumentoAnexoResponseDTO.builder()
                .id(doc.getId())
                .nomeOriginal(doc.getNomeOriginal())
                .tipoDocumento(doc.getTipoDocumento())
                .contentType(doc.getContentType())
                .tamanhoBytes(doc.getTamanhoBytes())
                .descricao(doc.getDescricao())
                .contratoId(doc.getContratoId())
                .ataId(doc.getAtaId())
                .criadoEm(doc.getCriadoEm())
                .urlDownload("/api/anexos/" + doc.getId() + "/download")
                .urlVisualizar("/api/anexos/" + doc.getId() + "/visualizar")
                .build();
    }
}
