package com.contract_management.api.modules.contrato.service;

import com.contract_management.api.modules.contrato.dto.request.DocumentoFiltro;
import com.contract_management.api.modules.contrato.dto.response.AtaResponseDTO;
import com.contract_management.api.modules.contrato.dto.response.ContratoResponseDTO;
import com.contract_management.api.modules.equipe.dto.response.EquipeContratoResponseDTO;
import com.contract_management.api.modules.secretaria.dto.response.SecretariaResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.text.Collator;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

/** Preserva a busca multi-termo e a ordenação das telas ao paginar a resposta no servidor. */
public final class DocumentoListagem {
    private DocumentoListagem() {}

    private record Documento(Long id, Integer numero, Integer ano, LocalDate dataFim, String tipo,
            String situacao, String objeto, String contratado, String portaria, String observacao,
            List<SecretariaResponseDTO> secretarias, List<EquipeContratoResponseDTO> equipe) {}

    public static Page<ContratoResponseDTO> contratos(List<ContratoResponseDTO> itens, Pageable page, DocumentoFiltro filtro) {
        return paginar(itens, page, filtro, c -> new Documento(c.getId(), c.getNumero(), c.getAno(), c.getDataFim(),
                c.getTipo(), c.getSituacao(), c.getObjeto(), c.getNomeContratado(), c.getPortariaDesignacao(),
                c.getObservacao(), c.getSecretarias(), c.getEquipe()), false);
    }

    public static Page<AtaResponseDTO> atas(List<AtaResponseDTO> itens, Pageable page, DocumentoFiltro filtro) {
        return paginar(itens, page, filtro, a -> new Documento(a.getId(), a.getNumero(), a.getAno(), a.getDataFim(),
                a.getTipo(), a.getSituacao(), a.getObjeto(), null, a.getPortariaDesignacao(),
                a.getObservacao(), a.getSecretarias(), a.getEquipe()), true);
    }

    private static <T> Page<T> paginar(List<T> itens, Pageable page, DocumentoFiltro filtro, Function<T, Documento> map, boolean ata) {
        var selecionados = new ArrayList<>(itens.stream().filter(item -> corresponde(map.apply(item), filtro, ata)).toList());
        var collator = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        Comparator<T> comparator = (a, b) -> 0;
        for (var order : page.getSort()) {
            Comparator<T> next = (a, b) -> comparar(map.apply(a), map.apply(b), order.getProperty(), collator);
            if (order.isDescending()) next = next.reversed();
            comparator = comparator.thenComparing(next);
        }
        selecionados.sort(comparator);
        int inicio = (int) Math.min(page.getOffset(), selecionados.size());
        int fim = Math.min(inicio + page.getPageSize(), selecionados.size());
        return new PageImpl<>(selecionados.subList(inicio, fim), page, selecionados.size());
    }

    private static boolean corresponde(Documento d, DocumentoFiltro f, boolean ata) {
        if (f.getAno() != null && !f.getAno().equals(d.ano())) return false;
        if (!f.getTipo().isBlank() && !(ata ? texto(d.tipo()).trim().equalsIgnoreCase(f.getTipo().trim()) : Objects.equals(d.tipo(), f.getTipo()))) return false;
        var situacao = d.situacao() == null || d.situacao().isEmpty() ? "ATIVO" : d.situacao();
        if (!f.getStatus().isBlank() && !(ata ? situacao.trim().equalsIgnoreCase(f.getStatus().trim()) : situacao.equals(f.getStatus()))) return false;
        if (!vigencia(d.dataFim(), f.getVigencia())) return false;
        var secs = d.secretarias() == null ? List.<SecretariaResponseDTO>of() : d.secretarias();
        if (!f.getSecretarias().isEmpty() && secs.stream().noneMatch(s -> f.getSecretarias().contains(s.getId()))) return false;
        List<String> pessoas = new ArrayList<>();
        if (d.equipe() != null) for (var equipe : d.equipe()) if (equipe.getMembros() != null)
            for (var membro : equipe.getMembros()) pessoas.add(texto(membro.getServidorNome()) + " " + texto(membro.getServidorCargo()) + " " + texto(membro.getFuncaoNome()));
        if (!f.getPessoas().isEmpty() && f.getPessoas().stream().noneMatch(p -> pessoas.stream().anyMatch(m -> busca(m, p)))) return false;
        var targets = new StringBuilder().append(d.numero()).append(' ').append(d.ano()).append(' ')
                .append(d.numero()).append('/').append(d.ano()).append(' ').append(texto(d.objeto())).append(' ')
                .append(texto(d.contratado())).append(' ').append(texto(d.portaria())).append(' ')
                .append(texto(d.tipo())).append(' ').append(texto(d.situacao())).append(' ').append(texto(d.observacao()));
        for (var sec : secs) targets.append(' ').append(texto(sec.getSigla())).append(' ').append(texto(sec.getNome()));
        pessoas.forEach(p -> targets.append(' ').append(p));
        return busca(targets.toString(), f.getSearch());
    }

    static boolean busca(String targets, String search) {
        String composite = normalizar(targets);
        var words = Arrays.asList(composite.split("[\\s/,-]+"));
        return Arrays.stream(normalizar(search).split("[\\s/,-]+"))
                .filter(token -> !token.isEmpty()).allMatch(token -> token.matches("\\d+")
                        ? words.contains(token) || composite.contains("/" + token) || composite.contains(token + "/")
                        : composite.contains(token));
    }

    private static boolean vigencia(LocalDate fim, String filtro) {
        if (filtro.isBlank()) return true;
        if (fim == null) return false;
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), fim);
        return switch (filtro) {
            case "VIGENTE" -> dias > 60;
            case "ATENCAO" -> dias > 30 && dias <= 60;
            case "CRITICA" -> dias >= 0 && dias <= 30;
            case "EM_ALERTA" -> dias >= 0 && dias <= 60;
            case "VENCIDO" -> dias < 0;
            case "TODOS_VIGENTES" -> dias >= 0;
            default -> true;
        };
    }

    private static int comparar(Documento a, Documento b, String campo, Collator collator) {
        return switch (campo) {
            case "numero" -> { int ano = Integer.compare(a.ano(), b.ano()); yield ano != 0 ? ano : Integer.compare(a.numero(), b.numero()); }
            case "ano" -> Integer.compare(a.ano(), b.ano());
            case "vigencia", "dataFim" -> compararData(a.dataFim(), b.dataFim());
            case "tipo", "tipo.tipoArp" -> collator.compare(texto(a.tipo()), texto(b.tipo()));
            case "situacao", "ativo.situacao" -> collator.compare(texto(a.situacao()), texto(b.situacao()));
            case "objeto" -> collator.compare(texto(a.objeto()), texto(b.objeto()));
            case "nomeContratado" -> collator.compare(texto(a.contratado()), texto(b.contratado()));
            default -> Long.compare(a.id(), b.id());
        };
    }

    private static int compararData(LocalDate a, LocalDate b) { return Comparator.nullsFirst(Comparator.<LocalDate>naturalOrder()).compare(a, b); }
    private static String texto(String s) { return s == null ? "" : s; }
    private static String normalizar(String s) { return Normalizer.normalize(texto(s), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim(); }
}
