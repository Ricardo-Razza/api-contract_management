package com.contract_management.api.modules.impressora.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

import com.contract_management.api.common.exception.EntityNotFoundException;
import com.contract_management.api.modules.impressora.dto.request.LocalInstalacaoRequestDTO;
import com.contract_management.api.modules.impressora.dto.response.LocalInstalacaoResponseDTO;
import com.contract_management.api.modules.impressora.model.LocalInstalacao;
import com.contract_management.api.modules.impressora.repository.InstalacaoImpressoraRepository;
import com.contract_management.api.modules.impressora.repository.LocalInstalacaoRepository;
import com.contract_management.api.modules.secretaria.model.Secretaria;
import com.contract_management.api.modules.secretaria.repository.SecretariaRepository;

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

            Integer coluna = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'instalacao_impressora' " +
                    "AND COLUMN_NAME = 'local_instalacao_id'", Integer.class);
            if (coluna != null && coluna == 0) {
                jdbcTemplate.execute("ALTER TABLE instalacao_impressora ADD COLUMN local_instalacao_id BIGINT NULL");
                jdbcTemplate.execute("CREATE INDEX idx_instalacao_local ON instalacao_impressora(local_instalacao_id)");
            }
            sincronizarLocaisDasInstalacoes();
            jdbcTemplate.update("""
                UPDATE instalacao_impressora i
                SET i.local_instalacao_id = (
                    SELECT MIN(l.id) FROM local_instalacao l
                    WHERE l.secretaria_id = i.secretaria_id
                      AND LOWER(TRIM(l.nome)) = LOWER(TRIM(i.local_instalacao))
                ) WHERE i.local_instalacao_id IS NULL
            """);
            log.info("Tabela e locais de instalacao verificados com sucesso.");
        } catch (Exception e) {
            log.warn("Verificacao de tabela local_instalacao: {}", e.getMessage());
        }
    }

    @Transactional
    public int sincronizarLocaisDasInstalacoes() {
        try {
            return jdbcTemplate.update("""
                INSERT INTO `local_instalacao` (`nome`, `secretaria_id`, `endereco`, `responsavel`, `ativo`)
                SELECT TRIM(i.local_instalacao), i.secretaria_id, MAX(i.endereco), MAX(i.responsavel), 1
                FROM `instalacao_impressora` i
                WHERE i.local_instalacao_id IS NULL
                  AND i.local_instalacao IS NOT NULL AND TRIM(i.local_instalacao) <> ''
                  AND NOT EXISTS (
                      SELECT 1 FROM `local_instalacao` l 
                      WHERE LOWER(TRIM(l.nome)) = LOWER(TRIM(i.local_instalacao)) 
                        AND l.secretaria_id = i.secretaria_id
                  )
                GROUP BY TRIM(i.local_instalacao), i.secretaria_id;
            """);
        } catch (Exception e) {
            log.warn("Falha ao sincronizar locais das instalacoes: {}", e.getMessage());
            return 0;
        }
    }

    @Transactional
    public List<LocalInstalacaoResponseDTO> listarTodos(Long secretariaId) {
        try {
            if (localRepository.count() == 0) {
                sincronizarLocaisDasInstalacoes();
            }
        } catch (Exception ignored) {
        }

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

    @Transactional(readOnly = true)
    public List<com.contract_management.api.modules.impressora.dto.response.InstalacaoHistoricoDTO> historico(Long id) {
        if (!localRepository.existsById(id)) throw new EntityNotFoundException("Local de Instalação", id);
        return instalacaoRepository.findHistoricoByLocalId(id).stream()
                .map(com.contract_management.api.modules.impressora.dto.response.InstalacaoHistoricoDTO::from).toList();
    }

    private LocalInstalacaoResponseDTO toResponseDTO(LocalInstalacao local) {
        long count = instalacaoRepository.countByLocalInstalacaoIdAndStatus(local.getId(), "ATIVA");

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