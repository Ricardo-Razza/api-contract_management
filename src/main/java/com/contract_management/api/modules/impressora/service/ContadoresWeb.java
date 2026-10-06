package com.contract_management.api.modules.impressora.service;

import com.contract_management.api.modules.impressora.model.ColetaContadorItem;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Mantém ausências como null e preserva zeros efetivamente retornados. */
final class ContadoresWeb {
    private ContadoresWeb() {}

    static Integer json(String conteudo, String chave) {
        return numero(conteudo, "[\"']?" + Pattern.quote(chave) + "[\"']?\\s*:\\s*[\"']?(\\d+)[\"']?\\s*(?=[,}])");
    }

    static Integer xml(String conteudo, String... tags) {
        for (String tag : tags) {
            Integer valor = numero(conteudo, "<(?:[\\w.-]+:)?" + Pattern.quote(tag) + ">\\s*(\\d+)\\s*</");
            if (valor != null) return valor;
        }
        return null;
    }

    private static Integer numero(String conteudo, String regex) {
        Matcher m = Pattern.compile(regex).matcher(conteudo);
        if (!m.find()) return null;
        try {
            return Integer.valueOf(m.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static boolean aplicarSws(ColetaContadorItem item, String conteudo) {
        Integer total = json(conteudo, "GXI_BILLING_TOTAL_IMP_CNT");
        if (total != null) item.setContadorTotal(total);
        Integer print = json(conteudo, "GXI_BILLING_PRINT_TOTAL_IMP_CNT");
        Integer copy = json(conteudo, "GXI_BILLING_COPY_TOTAL_IMP_CNT");
        Integer scanner = json(conteudo, "GXI_BILLING_SEND_TO_TOTAL_CNT");
        if (print != null) item.setCopiasPrint(print);
        if (copy != null) item.setCopiasCopiador(copy);
        if (scanner != null) item.setCopiasScanner(scanner);
        return total != null || print != null || copy != null || scanner != null;
    }

    static boolean aplicarXml(ColetaContadorItem item, String conteudo) {
        Integer total = xml(conteudo, "TotalImpressions", "TotalPrintEnginePageCount");
        Integer mono = xml(conteudo, "MonochromeImpressions");
        Integer color = xml(conteudo, "ColorImpressions");
        Integer print = xml(conteudo, "PrintPages");
        Integer copy = xml(conteudo, "CopyImpressions");
        Integer scanner = xml(conteudo, "ScanImages", "TotalImagesScanned");
        if (total != null) item.setContadorTotal(total);
        if (mono != null) item.setContadorMono(mono);
        if (color != null) item.setContadorColor(color);
        if (print != null) item.setCopiasPrint(print);
        if (copy != null) item.setCopiasCopiador(copy);
        if (scanner != null) item.setCopiasScanner(scanner);
        return total != null || mono != null || color != null || print != null || copy != null || scanner != null;
    }
}
