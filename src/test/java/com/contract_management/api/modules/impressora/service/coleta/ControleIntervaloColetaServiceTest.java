package com.contract_management.api.modules.impressora.service.coleta;

import com.contract_management.api.common.exception.BusinessException;
import com.contract_management.api.modules.impressora.model.ColetaContadorItem;
import com.contract_management.api.modules.impressora.repository.ColetaContadorItemRepository;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ControleIntervaloColetaServiceTest {
    private final ColetaContadorItemRepository repository = mock(ColetaContadorItemRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-06T15:00:00Z"), ZoneOffset.UTC);
    private final ControleIntervaloColetaService controle = new ControleIntervaloColetaService(repository, clock);

    private void ultimaColeta(Long id, int segundosAtras) {
        when(repository.findFirstByImpressoraIdAndDataInicioColetaIsNotNullOrderByDataInicioColetaDesc(id))
                .thenReturn(Optional.of(ColetaContadorItem.builder().impressoraId(id)
                        .dataInicioColeta(LocalDateTime.now(clock).minusSeconds(segundosAtras)).build()));
    }

    @Test void bloqueiaAntesDeDoisMinutosEInformaTempoRestante() {
        ultimaColeta(1L, 119);
        BusinessException erro = assertThrows(BusinessException.class, () -> controle.reservar(1L));
        assertTrue(erro.getMessage().contains("1 segundos"));
    }

    @Test void permiteExatamenteAposDoisMinutos() {
        ultimaColeta(1L, 120);
        assertDoesNotThrow(() -> controle.reservar(1L));
    }

    @Test void naoPermiteSobreposicaoMesmoAposDoisMinutos() {
        ultimaColeta(1L, 300);
        controle.reservar(1L);
        assertTrue(assertThrows(BusinessException.class, () -> controle.reservar(1L))
                .getMessage().contains("em andamento"));
        assertDoesNotThrow(() -> controle.reservar(2L));
    }

    @Test void concluirNaoRemoveIntervalo() {
        controle.reservar(1L);
        ultimaColeta(1L, 60);
        controle.liberar(1L);
        assertThrows(BusinessException.class, () -> controle.reservar(1L));
    }

    @Test void loteIgnoraBloqueadasEContinuaComOutras() {
        controle.reservar(1L);
        ultimaColeta(2L, 60);
        assertEquals(List.of(3L), controle.reservarDisponiveis(List.of(1L, 2L, 3L, 3L)));
        assertThrows(BusinessException.class, () -> controle.reservar(3L));
    }

    @Test void loteComTodasBloqueadasNaoAgendaColeta() {
        ultimaColeta(1L, 60);
        assertThrows(BusinessException.class, () -> controle.reservarDisponiveis(List.of(1L)));
    }

    @Test void intervaloPersistidoContinuaValendoAposReinicio() {
        ultimaColeta(1L, 90);
        ControleIntervaloColetaService reiniciado = new ControleIntervaloColetaService(repository, clock);
        assertThrows(BusinessException.class, () -> reiniciado.reservar(1L));
    }

    @Test void tentativasSimultaneasReservamApenasUmaVez() {
        AtomicInteger aceitas = new AtomicInteger();
        var tarefas = java.util.stream.IntStream.range(0, 20).mapToObj(i -> CompletableFuture.runAsync(() -> {
            try { controle.reservar(1L); aceitas.incrementAndGet(); }
            catch (BusinessException esperado) { }
        })).toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(tarefas).join();
        assertEquals(1, aceitas.get());
    }
}
