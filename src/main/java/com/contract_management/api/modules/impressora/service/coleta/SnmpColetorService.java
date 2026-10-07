package com.contract_management.api.modules.impressora.service.coleta;

import lombok.extern.slf4j.Slf4j;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class SnmpColetorService {

    private static final String DEFAULT_COMMUNITY = "public";
    private static final int TIMEOUT_MS = 1800;
    private static final int RETRIES = 1;

    // OIDs Padronizados (RFC 3805 - Printer MIB e MIB-II)
    private static final OID OID_SYS_DESCR = new OID("1.3.6.1.2.1.1.1.0");
    private static final OID OID_SERIAL_PRINTER_MIB = new OID("1.3.6.1.2.1.43.5.1.1.17.1");
    private static final OID OID_SERIAL_RICOH = new OID("1.3.6.1.4.1.367.3.2.1.2.1.4.0");
    private static final OID OID_TOTAL_PRINTER_MIB_1 = new OID("1.3.6.1.2.1.43.10.2.1.4.1.1");
    private static final OID OID_TOTAL_PRINTER_MIB_ALT = new OID("1.3.6.1.2.1.43.10.2.1.4.1");
    private static final OID OID_TOTAL_PRINTER_MIB = new OID("1.3.6.1.2.1.43.10.2.1.4");
    private static final OID OID_PANTUM_TOTAL = new OID("1.3.6.1.4.1.40093.10.3.1.1");
    private static final OID OID_PANTUM_SERIAL = new OID("1.3.6.1.4.1.40093.6.1.2");
    private static final OID OID_PANTUM_TONER = new OID("1.3.6.1.4.1.40093.6.3.1");

    // OIDs Proprietários Ricoh (Private Enterprise 367)
    private static final OID OID_RICOH_TOTAL = new OID("1.3.6.1.4.1.367.3.2.1.2.19.5.1.9.1");
    private static final OID OID_RICOH_COLOR_TOTAL = new OID("1.3.6.1.4.1.367.3.2.1.2.19.5.1.9.13");
    private static final OID OID_RICOH_MONO_TOTAL = new OID("1.3.6.1.4.1.367.3.2.1.2.19.5.1.9.14");
    private static final OID OID_RICOH_PRINT_MONO = new OID("1.3.6.1.4.1.367.3.2.1.2.19.5.1.9.9");
    private static final OID OID_RICOH_PRINT_COLOR = new OID("1.3.6.1.4.1.367.3.2.1.2.19.5.1.9.11");
    private static final OID OID_RICOH_COPY_MONO = new OID("1.3.6.1.4.1.367.3.2.1.2.19.5.1.9.3");
    private static final OID OID_RICOH_COPY_COLOR = new OID("1.3.6.1.4.1.367.3.2.1.2.19.5.1.9.5");
    private static final OID OID_RICOH_SCANNER = new OID("1.3.6.1.4.1.367.3.2.1.2.19.5.1.9.15");
    private static final OID OID_RICOH_TONER_PERCENT = new OID("1.3.6.1.4.1.367.3.2.1.2.24.1.1.5.1");

    public record SnmpResultado(
            boolean sucesso,
            Integer contadorTotal,
            Integer contadorMono,
            Integer contadorColor,
            Integer copiasPrint,
            Integer copiasCopiador,
            Integer copiasScanner,
            String numeroSerie,
            Integer nivelToner,
            String modeloDetectado
    ) {
        public static SnmpResultado falha() {
            return new SnmpResultado(false, null, null, null, null, null, null, null, null, null);
        }
    }

    public SnmpResultado coletar(String ip) {
        return coletar(ip, null);
    }

    public SnmpResultado coletar(String ip, String modelo) {
        if (ip == null || ip.isBlank()) {
            return SnmpResultado.falha();
        }

        Snmp snmp = null;
        try {
            DefaultUdpTransportMapping transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();

            CommunityTarget<Address> target = new CommunityTarget<>();
            target.setCommunity(new OctetString(DEFAULT_COMMUNITY));
            target.setAddress(GenericAddress.parse("udp:" + ip.trim() + "/161"));
            target.setVersion(SnmpConstants.version2c);
            target.setTimeout(TIMEOUT_MS);
            target.setRetries(RETRIES);

            // 1. Teste de conectividade e identificação básica via sysDescr
            Variable vSys = safeGet(snmp, target, OID_SYS_DESCR);
            if (vSys == null) {
                // Tenta fallback com SNMP v1 se v2c não responder
                target.setVersion(SnmpConstants.version1);
                vSys = safeGet(snmp, target, OID_SYS_DESCR);
            }

            if (vSys == null) {
                return SnmpResultado.falha();
            }

            String sysDescr = extrairTextoLimpo(vSys.toString());

            // Pantum usa a Printer MIB; os índices variam conforme o modelo.
            boolean pantum = (modelo != null && modelo.toUpperCase(java.util.Locale.ROOT).contains("PANTUM")) ||
                    (sysDescr != null && sysDescr.toUpperCase(java.util.Locale.ROOT).contains("PANTUM"));
            if (pantum) {
                Integer total = consultarContadorPantum(snmp, target);
                String serial = extrairTextoLimpo(safeGetStr(snmp, target, OID_PANTUM_SERIAL));
                if (serial == null) serial = extrairTextoLimpo(safeGetStr(snmp, target, OID_SERIAL_PRINTER_MIB));
                if (serial != null && serial.matches("[0: ]+")) serial = null;
                Integer toner = safeGetInt(snmp, target, OID_PANTUM_TONER);
                if (toner != null && (toner < 0 || toner > 100)) toner = null;
                return new SnmpResultado(true, total, total, total != null ? 0 : null,
                        null, null, null, serial, toner, sysDescr);
            }

            // 2. Extração segura do Número de Série
            String serial = extrairTextoLimpo(safeGetStr(snmp, target, OID_SERIAL_PRINTER_MIB));
            if (serial == null || serial.matches("^0{2,}:.*") || serial.equalsIgnoreCase("N/D")) {
                serial = extrairTextoLimpo(safeGetStr(snmp, target, OID_SERIAL_RICOH));
            }
            if ((serial == null || serial.isBlank()) && sysDescr != null) {
                Matcher m = Pattern.compile("S/N\\s*[:=]?\\s*([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE).matcher(sysDescr);
                if (m.find()) {
                    serial = m.group(1).trim();
                }
            }

            // 3. Extração dos Contadores Totais
            Integer total = safeGetInt(snmp, target, OID_RICOH_TOTAL);
            if (total == null || total == 0) {
                total = safeGetInt(snmp, target, OID_TOTAL_PRINTER_MIB_1);
            }
            if (total == null || total == 0) {
                total = safeGetInt(snmp, target, OID_TOTAL_PRINTER_MIB_ALT);
            }
            if (total == null) {
                total = consultarContadorPadrao(snmp, target);
            }

            // 4. Extração Mono vs Color
            Integer ricohMonoTotal = safeGetInt(snmp, target, OID_RICOH_MONO_TOTAL);
            Integer ricohColorTotal = safeGetInt(snmp, target, OID_RICOH_COLOR_TOTAL);

            Integer contadorMono = null;
            Integer contadorColor = null;

            if (ricohMonoTotal != null && ricohMonoTotal > 0) {
                contadorMono = ricohMonoTotal;
                contadorColor = (ricohColorTotal != null) ? ricohColorTotal : 0;
            } else if (total != null && total > 0) {
                contadorMono = total;
                contadorColor = 0;
            }

            // 5. Contadores detalhados de cópia, impressão e scanner
            Integer ricohPrintMono = safeGetInt(snmp, target, OID_RICOH_PRINT_MONO);
            Integer ricohPrintColor = safeGetInt(snmp, target, OID_RICOH_PRINT_COLOR);
            Integer ricohCopyMono = safeGetInt(snmp, target, OID_RICOH_COPY_MONO);
            Integer ricohCopyColor = safeGetInt(snmp, target, OID_RICOH_COPY_COLOR);
            Integer ricohScanner = safeGetInt(snmp, target, OID_RICOH_SCANNER);

            Integer copiasPrint = null;
            Integer copiasCopiador = null;
            Integer copiasScanner = ricohScanner;

            if (ricohPrintMono != null || ricohPrintColor != null) {
                copiasPrint = (ricohPrintMono != null ? ricohPrintMono : 0) + (ricohPrintColor != null ? ricohPrintColor : 0);
            }
            if (ricohCopyMono != null || ricohCopyColor != null) {
                copiasCopiador = (ricohCopyMono != null ? ricohCopyMono : 0) + (ricohCopyColor != null ? ricohCopyColor : 0);
            }

            // 6. Consulta Nível de Toner (%)
            Integer nivelToner = consultarTonerPercent(snmp, target);

            boolean sucesso = (total != null && total > 0) || (serial != null && !serial.isBlank());

            return new SnmpResultado(
                    sucesso,
                    total,
                    contadorMono,
                    contadorColor,
                    copiasPrint,
                    copiasCopiador,
                    copiasScanner,
                    serial,
                    nivelToner,
                    sysDescr
            );

        } catch (Exception e) {
            log.debug("Erro ao consultar SNMP para IP {}: {}", ip, e.getMessage());
            return SnmpResultado.falha();
        } finally {
            if (snmp != null) {
                try {
                    snmp.close();
                } catch (IOException ignored) {}
            }
        }
    }

    ResponseEvent<Address> enviarConsulta(Snmp snmp, Target<Address> target, PDU request) throws IOException {
        // Todo envio do coletor passa por esta lista explícita de operações de leitura.
        if (request == null || (request.getType() != PDU.GET && request.getType() != PDU.GETNEXT)) {
            throw new SecurityException("Coletor SNMP somente leitura: apenas GET e GETNEXT são permitidos.");
        }
        return snmp.send(request, target);
    }

    private Variable safeGet(Snmp snmp, Target<Address> target, OID oid) {
        try {
            PDU pdu = new PDU();
            pdu.setType(PDU.GET);
            pdu.add(new VariableBinding(oid));
            ResponseEvent<Address> resp = enviarConsulta(snmp, target, pdu);
            if (resp != null && resp.getResponse() != null) {
                PDU r = resp.getResponse();
                if (r.getErrorStatus() == PDU.noError && r.size() > 0) {
                    Variable v = r.get(0).getVariable();
                    if (!(v instanceof Null)) {
                        return v;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    Integer consultarContadorPantum(Snmp snmp, Target<Address> target) {
        // Confirmado em M7100DW e P3300: o contador padrão pode ser um zero fixo.
        Integer total = safeGetInt(snmp, target, OID_PANTUM_TOTAL);
        if (total != null && total >= 0) return total;
        Integer padrao = consultarContadorPadrao(snmp, target);
        // Sem o OID proprietário, zero não comprova uma leitura válida nesta marca.
        return padrao != null && padrao > 0 ? padrao : null;
    }

    Integer consultarContadorPadrao(Snmp snmp, Target<Address> target) {
        // GETNEXT encontra hrDeviceIndex/prtMarkerIndex sem presumir 1.1.
        OID cursor = new OID(OID_TOTAL_PRINTER_MIB);
        try {
            for (int tentativa = 0; tentativa < 16; tentativa++) {
                PDU request = new PDU();
                request.setType(PDU.GETNEXT);
                request.add(new VariableBinding(cursor));
                ResponseEvent<Address> event = enviarConsulta(snmp, target, request);
                PDU response = event != null ? event.getResponse() : null;
                if (response == null || response.getErrorStatus() != PDU.noError || response.size() == 0) return null;
                VariableBinding binding = response.get(0);
                OID oid = binding.getOid();
                if (!oid.startsWith(OID_TOTAL_PRINTER_MIB) || oid.compareTo(cursor) <= 0 || binding.getVariable() instanceof Null) return null;
                cursor = new OID(oid);
                Integer total = extrairInt(binding.getVariable().toString());
                if (total == null || total < 0) continue;
                // Aceita somente contadores em impressões (7) ou folhas (8).
                OID unitOid = new OID(oid);
                unitOid.set(OID_TOTAL_PRINTER_MIB.size() - 1, 3);
                Integer unit = safeGetInt(snmp, target, unitOid);
                if (unit != null && (unit == 7 || unit == 8)) return total;
            }
        } catch (Exception e) {
            log.debug("Erro ao descobrir contador na Printer MIB: {}", e.getMessage());
        }
        return null;
    }

    private String safeGetStr(Snmp snmp, Target<Address> target, OID oid) {
        Variable v = safeGet(snmp, target, oid);
        return v != null ? v.toString() : null;
    }

    private Integer safeGetInt(Snmp snmp, Target<Address> target, OID oid) {
        Variable v = safeGet(snmp, target, oid);
        if (v == null) return null;
        return extrairInt(v.toString());
    }

    private Integer consultarTonerPercent(Snmp snmp, Target<Address> target) {
        // 1. Tenta OID proprietário Ricoh (retorna direto a % inteira)
        Integer ricohPct = safeGetInt(snmp, target, OID_RICOH_TONER_PERCENT);
        if (ricohPct != null && ricohPct >= 0 && ricohPct <= 100) {
            return ricohPct;
        }

        // 2. Tenta RFC 3805 padrão (Printer MIB Supply)
        try {
            for (int idx = 1; idx <= 4; idx++) {
                Variable capVar = safeGet(snmp, target, new OID("1.3.6.1.2.1.43.11.1.1.8.1." + idx));
                Variable curVar = safeGet(snmp, target, new OID("1.3.6.1.2.1.43.11.1.1.9.1." + idx));

                if (curVar != null) {
                    int cur = extrairInt(curVar.toString()) != null ? extrairInt(curVar.toString()) : -1;
                    int cap = (capVar != null && extrairInt(capVar.toString()) != null) ? extrairInt(capVar.toString()) : -1;

                    if (cur == -3) {
                        return 100; // RFC 3805: someRemaining (toner normal e operacional)
                    } else if (cur == 0) {
                        return 0; // Vazio
                    } else if (cap > 0 && cur >= 0) {
                        return Math.min(100, Math.max(0, (cur * 100) / cap));
                    } else if (cap == -2 && cur > 0) {
                        return Math.min(100, cur);
                    } else if (cap == 100 && cur >= 0) {
                        return Math.min(100, cur);
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Integer extrairInt(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String extrairTextoLimpo(String val) {
        if (val == null || val.isBlank()) return null;
        String limpo = val.trim();
        if (limpo.startsWith("\"") && limpo.endsWith("\"") && limpo.length() >= 2) {
            limpo = limpo.substring(1, limpo.length() - 1).trim();
        }
        return limpo.isEmpty() ? null : limpo;
    }
}
