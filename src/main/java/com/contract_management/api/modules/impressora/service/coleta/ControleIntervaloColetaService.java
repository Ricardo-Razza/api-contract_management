package com.contract_management.api.modules.impressora.service.coleta;

import com.contract_management.api.common.exception.BusinessException;
import com.contract_management.api.modules.impressora.repository.ColetaContadorItemRepository;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ControleIntervaloColetaService {
    private final ColetaContadorItemRepository repository;
    private final Clock clock;
    private final Set<Long> reservadas = new HashSet<>();
    private static final Duration INTERVALO = Duration.ofMinutes(2);

    @Autowired
    public ControleIntervaloColetaService(ColetaContadorItemRepository repository) {
        this(repository, Clock.systemDefaultZone());
    }

    ControleIntervaloColetaService(ColetaContadorItemRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public synchronized void reservar(Long impressoraId) {
        String motivo = motivoBloqueio(impressoraId);
        if (motivo != null) throw new BusinessException(motivo);
        reservadas.add(impressoraId);
    }

    public synchronized List<Long> reservarDisponiveis(List<Long> impressoraIds) {
        List<Long> disponiveis = new ArrayList<>();
        String ultimoMotivo = null;
        for (Long id : impressoraIds.stream().distinct().toList()) {
            String motivo = motivoBloqueio(id);
            if (motivo == null) {
                disponiveis.add(id);
            } else ultimoMotivo = motivo;
        }
        if (disponiveis.isEmpty()) throw new BusinessException(ultimoMotivo != null ? ultimoMotivo : "Nenhuma impressora disponível para coleta.");
        reservadas.addAll(disponiveis);
        return disponiveis;
    }

    public synchronized void liberar(Long impressoraId) {
        reservadas.remove(impressoraId);
    }

    private String motivoBloqueio(Long id) {
        if (id == null) return "Impressora sem identificação para coleta.";
        if (reservadas.contains(id)) return "A impressora " + id + " já tem uma coleta agendada ou em andamento. Aguarde a conclusão.";
        return repository.findFirstByImpressoraIdAndDataInicioColetaIsNotNullOrderByDataInicioColetaDesc(id)
                .map(item -> {
                    Duration restante = Duration.between(LocalDateTime.now(clock), item.getDataInicioColeta().plus(INTERVALO));
                    if (restante.isNegative() || restante.isZero()) return null;
                    long segundos = (restante.toMillis() + 999) / 1000;
                    return "Aguarde " + segundos + " segundos para coletar novamente a impressora " + id + ". Intervalo mínimo: 2 minutos.";
                }).orElse(null);
    }
}
