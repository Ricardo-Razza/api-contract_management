package com.contract_management.api.service;

import com.contract_management.api.dto.request.LocalInstalacaoRequestDTO;
import com.contract_management.api.dto.response.LocalInstalacaoResponseDTO;
import com.contract_management.api.exception.EntityNotFoundException;
import com.contract_management.api.model.LocalInstalacao;
import com.contract_management.api.model.Secretaria;
import com.contract_management.api.repository.InstalacaoImpressoraRepository;
import com.contract_management.api.repository.LocalInstalacaoRepository;
import com.contract_management.api.repository.SecretariaRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalInstalacaoService {

    private final LocalInstalacaoRepository localRepository;
    private final SecretariaRepository secretariaRepository;
    private final InstalacaoImpressoraRepository instalacaoRepository;
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void inicializarTabelaELocais() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `local_instalacao` (
                    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                    `nome` VARCHAR(255) NOT NULL,
                    `secretaria_id` BIGINT NOT NULL,
                    `endereco` VARCHAR(255) NULL,
                    `responsavel` VARCHAR(150) NULL,
                    `telefone` VARCHAR(50) NULL,
                    `ativo` TINYINT(1) NOT NULL DEFAULT 1,
                    CONSTRAINT `fk_local_secretaria` FOREIGN KEY (`secretaria_id`) REFERENCES `secretaria`(`id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
            """);

            jdbcTemplate.execute("""
                INSERT INTO `local_instalacao` (`nome`, `secretaria_id`, `endereco`, `responsavel`, `ativo`)
                SELECT i.local_instalacao, i.secretaria_id, MAX(i.endereco), MAX(i.responsavel), 1
                FROM `instalacao_impressora` i
                WHERE i.local_instalacao IS NOT NULL AND TRIM(i.local_instalacao) <> ''
                  AND NOT EXISTS (
                      SELECT 1 FROM `local_instalacao` l 
                      WHERE LOWER(TRIM(l.nome)) = LOWER(TRIM(i.local_instalacao)) 
                        AND l.secretaria_id = i.secretaria_id
                  )
                GROUP BY i.local_instalacao, i.secretaria_id;
            """);
            log.info("Tabela e locais de instalacao verificados com sucesso.");
        } catch (Exception e) {
            log.warn("Verificacao de tabela local_instalacao: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<LocalInstalacaoResponseDTO> listarTodos(Long secretariaId) {
        List<LocalInstalacao> locais = (secretariaId != null)
                ? localRepository.findBySecretariaIdAtivos(secretariaId)
                : localRepository.findAllAtivosComSecretaria();

        return locais.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LocalInstalacaoResponseDTO buscarPorId(Long id) {
        LocalInstalacao local = localRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Local de Instalação", id));
        return toResponseDTO(local);
    }

    @Transactional
    public LocalInstalacaoResponseDTO criar(LocalInstalacaoRequestDTO dto) {
        Secretaria secretaria = secretariaRepository.findById(dto.getSecretariaId())
                .orElseThrow(() -> new EntityNotFoundException("Secretaria", dto.getSecretariaId()));

        LocalInstalacao local = LocalInstalacao.builder()
                .nome(dto.getNome().trim())
                .secretaria(secretaria)
                .endereco(dto.getEndereco() != null ? dto.getEndereco().trim() : null)
                .responsavel(dto.getResponsavel() != null ? dto.getResponsavel().trim() : null)
                .telefone(dto.getTelefone() != null ? dto.getTelefone().trim() : null)
                .ativo(dto.getAtivo() != null ? dto.getAtivo() : true)
                .build();

        LocalInstalacao salvo = localRepository.save(local);
        return toResponseDTO(salvo);
    }

    @Transactional
    public LocalInstalacaoResponseDTO atualizar(Long id, LocalInstalacaoRequestDTO dto) {
        LocalInstalacao local = localRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Local de Instalação", id));

        Secretaria secretaria = secretariaRepository.findById(dto.getSecretariaId())
                .orElseThrow(() -> new EntityNotFoundException("Secretaria", dto.getSecretariaId()));

        local.setNome(dto.getNome().trim());
        local.setSecretaria(secretaria);
        local.setEndereco(dto.getEndereco() != null ? dto.getEndereco().trim() : null);
        local.setResponsavel(dto.getResponsavel() != null ? dto.getResponsavel().trim() : null);
        local.setTelefone(dto.getTelefone() != null ? dto.getTelefone().trim() : null);
        if (dto.getAtivo() != null) {
            local.setAtivo(dto.getAtivo());
        }

        LocalInstalacao salvo = localRepository.save(local);
        return toResponseDTO(salvo);
    }

    @Transactional
    public void excluir(Long id) {
        LocalInstalacao local = localRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Local de Instalação", id));
        local.setAtivo(false);
        localRepository.save(local);
    }

    private LocalInstalacaoResponseDTO toResponseDTO(LocalInstalacao local) {
        long count = instalacaoRepository.countByLocalInstalacaoIgnoreCaseAndStatus(local.getNome(), "ATIVA");

        return LocalInstalacaoResponseDTO.builder()
                .id(local.getId())
                .nome(local.getNome())
                .secretariaId(local.getSecretaria().getId())
                .secretariaNome(local.getSecretaria().getNome())
                .secretariaSigla(local.getSecretaria().getSigla())
                .endereco(local.getEndereco())
                .responsavel(local.getResponsavel())
                .telefone(local.getTelefone())
                .ativo(local.getAtivo())
                .quantidadeImpressorasAtivas(count)
                .build();
    }
}
