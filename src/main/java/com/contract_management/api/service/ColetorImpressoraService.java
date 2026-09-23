package com.contract_management.api.service;

import com.contract_management.api.dto.request.IniciarColetaRequestDTO;
import com.contract_management.api.dto.response.ColetaItemDTO;
import com.contract_management.api.dto.response.ColetaProgressoDTO;
import com.contract_management.api.dto.response.ColetaSessaoDTO;
import com.contract_management.api.model.*;
import com.contract_management.api.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ColetorImpressoraService {

    static {
        // Permite acessar impressoras em rede local com certificados autoassinados sem SAN/hostname
        System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
    }

    private final ColetaContadorSessaoRepository sessaoRepository;
    private final ColetaContadorItemRepository itemRepository;
    private final ImpressoraRepository impressoraRepository;
    private final InstalacaoImpressoraRepository instalacaoRepository;
    private final LeituraContadorRepository leituraRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final String BASE_UPLOAD_DIR = "uploads/contadores";
    private final ExecutorService executor = Executors.newFixedThreadPool(8);
    private HttpClient httpClient;

    @PostConstruct
    public void inicializar() {
        System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
        criarTabelasSeNecessario();
        configurarHttpClient();
        criarDiretorioBase();
    }

    private void criarTabelasSeNecessario() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `coleta_contador_sessao` (
                    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                    `ano_referencia` INT NOT NULL,
                    `mes_referencia` INT NOT NULL,
                    `data_inicio` DATETIME NOT NULL,
                    `data_fim` DATETIME NULL,
                    `status` VARCHAR(50) NOT NULL,
                    `total_impressoras` INT NOT NULL DEFAULT 0,
                    `total_sucesso` INT NOT NULL DEFAULT 0,
                    `total_falhas` INT NOT NULL DEFAULT 0,
                    `diretorio_prints` VARCHAR(255) NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
            """);

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `coleta_contador_item` (
                    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                    `sessao_id` BIGINT NOT NULL,
                    `impressora_id` BIGINT NULL,
                    `item_pedido` INT NULL,
                    `ip` VARCHAR(45) NULL,
                    `modelo` VARCHAR(100) NULL,
                    `secretaria_sigla` VARCHAR(50) NULL,
                    `local_instalacao` VARCHAR(255) NULL,
                    `status` VARCHAR(50) NOT NULL,
                    `mensagem` TEXT NULL,
                    `nome_arquivo` VARCHAR(150) NULL,
                    `caminho_arquivo` VARCHAR(255) NULL,
                    `contador_total` INT NULL,
                    `contador_mono` INT NULL,
                    `contador_color` INT NULL,
                    `copias_print` INT NULL,
                    `copias_copiador` INT NULL,
                    `copias_scanner` INT NULL,
                    `data_coleta` DATETIME NULL,
                    CONSTRAINT `fk_coleta_item_sessao` FOREIGN KEY (`sessao_id`) REFERENCES `coleta_contador_sessao`(`id`) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
            """);
            log.info("Tabelas de coleta de contadores verificadas com sucesso.");
        } catch (Exception e) {
            log.warn("Verificacao de tabelas de coleta: {}", e.getMessage());
        }
    }

    private void configurarHttpClient() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            this.httpClient = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .connectTimeout(Duration.ofSeconds(3))
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();
        } catch (Exception e) {
            log.error("Erro ao inicializar HttpClient confiavel: {}", e.getMessage());
            this.httpClient = HttpClient.newHttpClient();
        }
    }

    private void criarDiretorioBase() {
        try {
            Files.createDirectories(Paths.get(BASE_UPLOAD_DIR));
        } catch (IOException e) {
            log.warn("Nao foi possivel criar diretorio base {}: {}", BASE_UPLOAD_DIR, e.getMessage());
        }
    }

    private boolean isIpValido(String ip) {
        if (ip == null || ip.isBlank()) return false;
        String trimmed = ip.trim();
        if (trimmed.equalsIgnoreCase("USB") || trimmed.toLowerCase().contains("andrius")) return false;
        return trimmed.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
    }

    private boolean isModeloPantum(String modelo) {
        return modelo != null && modelo.toUpperCase().contains("PANTUM");
    }

    private boolean isSamsung(String modelo) {
        return modelo != null && (modelo.toUpperCase().contains("SAMSUNG") || modelo.toUpperCase().contains("M4070"));
    }

    private boolean isHp(String modelo) {
        if (modelo == null) return false;
        String m = modelo.toUpperCase();
        return m.contains("HP") || m.contains("HEWLETT") || m.contains("LASERJET") || m.contains("DESKJET") || m.contains("PAGEWIDE") || m.contains("432");
    }

    public synchronized ColetaProgressoDTO iniciarColeta(IniciarColetaRequestDTO request) {
        // Verifica se ja ha uma coleta em andamento
        Optional<ColetaContadorSessao> emAndamento = sessaoRepository.findFirstByStatusOrderByDataInicioDesc("EM_ANDAMENTO");
        if (emAndamento.isPresent()) {
            return converterProgresso(emAndamento.get());
        }

        List<InstalacaoImpressora> instalacoes = instalacaoRepository.findAllAtivasWithDetails();

        // Filtra instalacoes que tenham impressora com IP valido e compativel (exclui Pantum, USB e rede Andrius que requerem leitura manual)
        List<InstalacaoImpressora> candidatas = instalacoes.stream()
                .filter(inst -> inst.getImpressora() != null &&
                                Boolean.TRUE.equals(inst.getImpressora().getAtivo()) &&
                                isIpValido(inst.getImpressora().getIp()) &&
                                !isModeloPantum(inst.getImpressora().getModelo()))
                .filter(inst -> request.getSecretariaId() == null ||
                                (inst.getSecretaria() != null && inst.getSecretaria().getId().equals(request.getSecretariaId())))
                .filter(inst -> request.getEmpenhoId() == null ||
                                (inst.getEmpenho() != null && inst.getEmpenho().getId().equals(request.getEmpenhoId())))
                .sorted(Comparator.comparing(inst -> inst.getImpressora().getItemPedido() != null ? inst.getImpressora().getItemPedido() : 999))
                .toList();

        if (candidatas.isEmpty()) {
            throw new RuntimeException("Nenhuma impressora ativa com IP de rede compatível encontrada para os critérios informados.");
        }

        String subPasta = String.format("%d_%02d", request.getAno(), request.getMes());
        Path dirPath = Paths.get(BASE_UPLOAD_DIR, subPasta);
        try {
            Files.createDirectories(dirPath);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao criar diretorio para salvar prints: " + e.getMessage());
        }

        ColetaContadorSessao sessao = ColetaContadorSessao.builder()
                .anoReferencia(request.getAno())
                .mesReferencia(request.getMes())
                .dataInicio(LocalDateTime.now())
                .status("EM_ANDAMENTO")
                .totalImpressoras(candidatas.size())
                .totalSucesso(0)
                .totalFalhas(0)
                .diretorioPrints(dirPath.toString())
                .build();

        sessao = sessaoRepository.save(sessao);

        List<ColetaContadorItem> itens = new ArrayList<>();
        for (InstalacaoImpressora inst : candidatas) {
            Impressora imp = inst.getImpressora();
            String nomeArquivo = String.format("%02d - %s.png", imp.getItemPedido() != null ? imp.getItemPedido() : 0, imp.getIp().trim());
            ColetaContadorItem item = ColetaContadorItem.builder()
                    .sessao(sessao)
                    .impressoraId(imp.getId())
                    .itemPedido(imp.getItemPedido())
                    .ip(imp.getIp().trim())
                    .modelo(imp.getModelo())
                    .secretariaSigla(inst.getSecretaria() != null ? inst.getSecretaria().getSigla() : null)
                    .localInstalacao(inst.getLocalInstalacao())
                    .status("PENDENTE")
                    .nomeArquivo(nomeArquivo)
                    .caminhoArquivo(dirPath.resolve(nomeArquivo).toString())
                    .build();
            itens.add(item);
        }

        itemRepository.saveAll(itens);
        sessao.setItens(itens);

        // Dispara processamento assincrono
        final Long sessaoId = sessao.getId();
        CompletableFuture.runAsync(() -> executarProcessamentoLote(sessaoId), executor);

        return converterProgresso(sessao);
    }

    private void executarProcessamentoLote(Long sessaoId) {
        log.info("Iniciando processamento em lote da sessao de coleta ID {}", sessaoId);
        List<ColetaContadorItem> itens = itemRepository.findBySessaoIdOrderByItemPedidoAsc(sessaoId);
        if (itens.isEmpty()) return;

        String chromePath = buscarExecutavelNavegador();

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (ColetaContadorItem item : itens) {
            futures.add(CompletableFuture.runAsync(() -> processarItemIndividual(item, chromePath), executor));
        }

        // Aguarda todas as tarefas finalizarem
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Atualiza contadores finais da sessao
        sessaoRepository.findById(sessaoId).ifPresent(sessao -> {
            List<ColetaContadorItem> atualizados = itemRepository.findBySessaoIdOrderByItemPedidoAsc(sessaoId);
            int sucessos = (int) atualizados.stream().filter(i -> "SUCESSO".equals(i.getStatus())).count();
            int falhas = atualizados.size() - sucessos;

            sessao.setTotalSucesso(sucessos);
            sessao.setTotalFalhas(falhas);
            sessao.setStatus("CONCLUIDO");
            sessao.setDataFim(LocalDateTime.now());
            sessaoRepository.save(sessao);
            log.info("Sessao de coleta ID {} finalizada com {} sucessos e {} falhas.", sessaoId, sucessos, falhas);
        });
    }

    private void processarItemIndividual(ColetaContadorItem item, String chromePath) {
        String ip = item.getIp();
        String modelo = item.getModelo() != null ? item.getModelo().toUpperCase() : "";

        // 1. Verificacao rapida de conectividade (Fail-fast em portas 443 e 80 com tolerância a rede local)
        boolean porta443 = isPortaAberta(ip, 443, 3000);
        boolean porta80 = isPortaAberta(ip, 80, 2500);

        if (!porta443 && !porta80) {
            item.setStatus("OFFLINE");
            item.setMensagem("Dispositivo desligado ou sem resposta na rede (timeout portas 443/80)");
            item.setDataColeta(LocalDateTime.now());
            itemRepository.save(item);
            atualizarProgressoParcial(item.getSessao().getId());
            return;
        }

        File arquivoDestino = new File(item.getCaminhoArquivo());
        boolean printGerado = false;

        if (isSamsung(modelo)) {
            SamsungDados dadosSamsung = extrairDadosSamsung(item);
            if (chromePath != null) {
                printGerado = tirarScreenshotSamsung(chromePath, item, dadosSamsung, arquivoDestino);
            }
        } else if (isHp(modelo)) {
            HpDados dadosHp = extrairDadosHp(item);
            if (chromePath != null) {
                printGerado = tirarScreenshotHp(chromePath, item, dadosHp, arquivoDestino);
            }
        } else {
            String url = resolverUrlPainel(ip, modelo, porta443, porta80);
            extrairDadosContador(item, url);
            if (chromePath != null) {
                printGerado = tirarScreenshot(chromePath, url, arquivoDestino);
            }
        }

        if (printGerado) {
            item.setStatus("SUCESSO");
            if (item.getContadorTotal() != null && item.getContadorTotal() > 0) {
                item.setMensagem("Contadores e comprovante visual capturados com sucesso.");
            } else {
                item.setMensagem("Comprovante visual capturado em alta resolução.");
            }
        } else if (item.getContadorTotal() != null && item.getContadorTotal() > 0) {
            item.setStatus("SUCESSO");
            item.setMensagem("Contadores lidos com sucesso (sem comprovante visual do navegador).");
        } else {
            item.setStatus("ERRO");
            item.setMensagem("Equipamento acessado, mas os contadores não puderam ser extraídos.");
        }

        item.setDataColeta(LocalDateTime.now());
        itemRepository.save(item);
        atualizarProgressoParcial(item.getSessao().getId());
    }

    private synchronized void atualizarProgressoParcial(Long sessaoId) {
        sessaoRepository.findById(sessaoId).ifPresent(s -> {
            List<ColetaContadorItem> itens = itemRepository.findBySessaoIdOrderByItemPedidoAsc(sessaoId);
            int sucessos = (int) itens.stream().filter(i -> "SUCESSO".equals(i.getStatus())).count();
            int falhas = (int) itens.stream().filter(i -> "OFFLINE".equals(i.getStatus()) || "ERRO".equals(i.getStatus())).count();
            s.setTotalSucesso(sucessos);
            s.setTotalFalhas(falhas);
            sessaoRepository.save(s);
        });
    }

    private boolean isPortaAberta(String ip, int porta, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, porta), timeoutMs);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String resolverUrlPainel(String ip, String modelo, boolean porta443, boolean porta80) {
        if (isSamsung(modelo)) {
            return (porta80 ? "http://" : "https://") + ip + "/sws/index.html";
        }
        if (isHp(modelo)) {
            return (porta80 ? "http://" : "https://") + ip + "/sws/index.html";
        }

        boolean isWimModel = modelo.contains("C2003") || modelo.contains("C2004") || modelo.contains("MPC") || modelo.contains("MP C");
        if (isWimModel) {
            String schemaWim = porta80 ? "http://" : "https://";
            String urlWim = schemaWim + ip + "/web/guest/br/websys/status/getUnificationCounter.cgi";
            if (testarRespostaHttp(urlWim)) {
                return urlWim;
            }
            String urlBsa = (porta443 ? "https://" : "http://") + ip + "/counter.asp?Lang=pt";
            if (testarRespostaHttp(urlBsa)) {
                return urlBsa;
            }
            return urlWim;
        }

        // Para os demais modelos Ricoh (SP 3710, M320F, 3510, 377, P 311, MP 501, Ricoh geral, etc.)
        if (porta443 && testarRespostaHttp("https://" + ip + "/counter.asp?Lang=pt")) {
            return "https://" + ip + "/counter.asp?Lang=pt";
        }
        if (porta80 && testarRespostaHttp("http://" + ip + "/counter.asp?Lang=pt")) {
            return "http://" + ip + "/counter.asp?Lang=pt";
        }
        // Fallback caso counter.asp nao responda
        if (testarRespostaHttp("http://" + ip + "/web/guest/br/websys/status/getUnificationCounter.cgi")) {
            return "http://" + ip + "/web/guest/br/websys/status/getUnificationCounter.cgi";
        }

        return (porta443 ? "https://" : "http://") + ip + "/counter.asp?Lang=pt";
    }

    private boolean testarRespostaHttp(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(1500))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();
            HttpResponse<Void> resp = httpClient.send(req, HttpResponse.BodyHandlers.discarding());
            return resp.statusCode() >= 200 && resp.statusCode() < 400;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean tirarScreenshot(String chromePath, String url, File arquivoDestino) {
        Path tempProfile = null;
        try {
            if (arquivoDestino.getParentFile() != null) {
                arquivoDestino.getParentFile().mkdirs();
            }

            tempProfile = Files.createTempDirectory("coleta_browser_");

            List<String> command = List.of(
                    chromePath,
                    "--headless=new",
                    "--disable-gpu",
                    "--no-sandbox",
                    "--test-type",
                    "--ignore-certificate-errors",
                    "--ignore-certificate-errors-spki-list",
                    "--ignore-ssl-errors",
                    "--allow-insecure-localhost",
                    "--disable-web-security",
                    "--allow-running-insecure-content",
                    "--disable-features=IsolateOrigins,site-per-process",
                    "--user-data-dir=" + tempProfile.toAbsolutePath().toString(),
                    "--window-size=1050,720",
                    "--force-device-scale-factor=1.2",
                    "--hide-scrollbars",
                    "--virtual-time-budget=6000",
                    "--screenshot=" + arquivoDestino.getAbsolutePath(),
                    url
            );

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process process = pb.start();

            boolean finished = process.waitFor(22, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
            }

            return arquivoDestino.exists() && arquivoDestino.length() > 1024;
        } catch (Exception e) {
            log.warn("Erro ao tirar screenshot de {}: {}", url, e.getMessage());
            return arquivoDestino.exists() && arquivoDestino.length() > 1024;
        } finally {
            if (tempProfile != null) {
                try {
                    try (var stream = Files.walk(tempProfile)) {
                        stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                        });
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private void extrairDadosContador(ColetaContadorItem item, String url) {
        // Suporte a contadores nativos em JSON para Samsung MultiXpress M4070
        if (item.getModelo() != null && (item.getModelo().toUpperCase().contains("SAMSUNG") || item.getModelo().toUpperCase().contains("M4070"))) {
            extrairDadosSamsung(item);
            return;
        }

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(4))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .GET()
                    .build();

            HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() < 200 || resp.statusCode() >= 400) return;

            String html = new String(resp.body(), StandardCharsets.ISO_8859_1);

            // 1. Formato Ricoh BSA (counter.asp)
            Pattern patTotalBsa = Pattern.compile("Total de p[^<]+</td>\\s*<td[^>]*>.*?</td>\\s*<td[^>]*>\\s*(\\d{1,8})\\s*</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher mTotalBsa = patTotalBsa.matcher(html);
            if (mTotalBsa.find()) {
                item.setContadorTotal(Integer.parseInt(mTotalBsa.group(1).trim()));
            }

            Pattern patPrint = Pattern.compile("Impressora</td>\\s*<td[^>]*>(\\d+)</td>\\s*<td[^>]*>(\\d+)</td>\\s*<td[^>]*>(\\d+)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher mPrint = patPrint.matcher(html);
            if (mPrint.find()) {
                item.setCopiasPrint(Integer.parseInt(mPrint.group(1).trim()));
                item.setContadorMono(Integer.parseInt(mPrint.group(3).trim()));
                item.setContadorColor(Integer.parseInt(mPrint.group(2).trim()));
            }

            Pattern patScanner = Pattern.compile("Scanner</td>\\s*<td[^>]*>(\\d+)</td>\\s*<td[^>]*>(\\d+)</td>\\s*<td[^>]*>(\\d+)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher mScanner = patScanner.matcher(html);
            if (mScanner.find()) {
                item.setCopiasScanner(Integer.parseInt(mScanner.group(1).trim()));
            }

            Pattern patCopiador = Pattern.compile("Copiador(?:a)?</td>\\s*<td[^>]*>(\\d+)</td>\\s*<td[^>]*>(\\d+)</td>\\s*<td[^>]*>(\\d+)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher mCopiador = patCopiador.matcher(html);
            if (mCopiador.find()) {
                item.setCopiasCopiador(Integer.parseInt(mCopiador.group(1).trim()));
            }

            // 2. Formato Ricoh WIM (getUnificationCounter.cgi)
            Pattern patMonoWim = Pattern.compile("Preto e branco\\s*</td>\\s*<td[^>]*>:</td>\\s*<td[^>]*>\\s*(\\d{1,8})\\s*</td>", Pattern.CASE_INSENSITIVE);
            Matcher mMonoWim = patMonoWim.matcher(html);
            if (mMonoWim.find()) {
                item.setContadorMono(Integer.parseInt(mMonoWim.group(1).trim()));
            }

            Pattern patColorWim = Pattern.compile("Cor\\s*</td>\\s*<td[^>]*>:</td>\\s*<td[^>]*>\\s*(\\d{1,8})\\s*</td>", Pattern.CASE_INSENSITIVE);
            Matcher mColorWim = patColorWim.matcher(html);
            if (mColorWim.find()) {
                item.setContadorColor(Integer.parseInt(mColorWim.group(1).trim()));
            }

            if (item.getContadorTotal() == null) {
                if (item.getContadorMono() != null && item.getContadorColor() != null) {
                    item.setContadorTotal(item.getContadorMono() + item.getContadorColor());
                } else if (item.getContadorMono() != null) {
                    item.setContadorTotal(item.getContadorMono());
                }
            }

        } catch (Exception e) {
            log.debug("Nao foi possivel parsear contadores HTML de {}: {}", url, e.getMessage());
        }
    }

    private record SamsungDados(
            String serial,
            int total,
            int print,
            int copy,
            int scanner,
            int simplexTotal,
            int simplexPrint,
            int simplexReport,
            int duplexTotal,
            int duplexPrint,
            int duplexReport,
            int reportTotal
    ) {}

    private SamsungDados extrairDadosSamsung(ColetaContadorItem item) {
        try {
            String jsonUrl = "http://" + item.getIp() + "/sws/app/information/counters/counters.json";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(jsonUrl))
                    .timeout(Duration.ofSeconds(4))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .GET()
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200 && resp.body() != null) {
                String json = resp.body();

                int total = extrairIntRegex(json, "GXI_BILLING_TOTAL_IMP_CNT\\s*:\\s*(\\d+)", 0);
                int print = extrairIntRegex(json, "GXI_BILLING_PRINT_TOTAL_IMP_CNT\\s*:\\s*(\\d+)", 0);
                int copy = extrairIntRegex(json, "GXI_BILLING_COPY_TOTAL_IMP_CNT\\s*:\\s*(\\d+)", 0);
                int scanner = extrairIntRegex(json, "GXI_BILLING_SEND_TO_TOTAL_CNT\\s*:\\s*(\\d+)", 0);
                int simplexTotal = extrairIntRegex(json, "GXI_BILLING_SIMPLEX_BW_TOTAL_CNT\\s*:\\s*(\\d+)", 0);
                int simplexPrint = extrairIntRegex(json, "GXI_BILLING_SIMPLEX_BW_PRINT_CNT\\s*:\\s*(\\d+)", 0);
                int simplexReport = extrairIntRegex(json, "GXI_BILLING_SIMPLEX_BW_REPORT_CNT\\s*:\\s*(\\d+)", 0);
                int duplexTotal = extrairIntRegex(json, "GXI_BILLING_DUPLEX_BW_TOTAL_CNT\\s*:\\s*(\\d+)", 0);
                int duplexPrint = extrairIntRegex(json, "GXI_BILLING_DUPLEX_BW_PRINT_CNT\\s*:\\s*(\\d+)", 0);
                int duplexReport = extrairIntRegex(json, "GXI_BILLING_DUPLEX_BW_REPORT_CNT\\s*:\\s*(\\d+)", 0);
                int reportTotal = extrairIntRegex(json, "GXI_BILLING_REPORT_TOTAL_IMP_CNT\\s*:\\s*(\\d+)", 0);

                String serial = extrairStringRegex(json, "GXI_SYS_SERIAL_NUM\\s*:\\s*\"([^\"]+)\"", "N/D");

                item.setContadorTotal(total);
                item.setContadorMono(total);
                item.setContadorColor(0);
                item.setCopiasPrint(print);
                item.setCopiasCopiador(copy);
                item.setCopiasScanner(scanner);

                return new SamsungDados(serial, total, print, copy, scanner,
                        simplexTotal, simplexPrint, simplexReport,
                        duplexTotal, duplexPrint, duplexReport, reportTotal);
            }
        } catch (Exception e) {
            log.debug("Nao foi possivel extrair JSON da Samsung {}: {}", item.getIp(), e.getMessage());
        }
        return null;
    }

    private int extrairIntRegex(String texto, String regex, int padrao) {
        try {
            Matcher m = Pattern.compile(regex).matcher(texto);
            if (m.find()) {
                return Integer.parseInt(m.group(1).trim());
            }
        } catch (Exception ignored) {}
        return padrao;
    }

    private String extrairStringRegex(String texto, String regex, String padrao) {
        try {
            Matcher m = Pattern.compile(regex).matcher(texto);
            if (m.find()) {
                return m.group(1).trim();
            }
        } catch (Exception ignored) {}
        return padrao;
    }

    private String formatarMilhar(int valor) {
        return String.format(Locale.GERMAN, "%,d", valor);
    }

    private boolean tirarScreenshotSamsung(String chromePath, ColetaContadorItem item, SamsungDados dados, File arquivoDestino) {
        Path tempHtml = null;
        try {
            String html = gerarHtmlComprovanteSamsung(item, dados);
            tempHtml = Files.createTempFile("samsung_sws_", ".html");
            Files.writeString(tempHtml, html, StandardCharsets.UTF_8);

            return tirarScreenshot(chromePath, tempHtml.toUri().toString(), arquivoDestino);
        } catch (Exception e) {
            log.warn("Erro ao gerar comprovante visual Samsung para {}: {}", item.getIp(), e.getMessage());
            return tirarScreenshot(chromePath, "http://" + item.getIp() + "/sws/index.html", arquivoDestino);
        } finally {
            if (tempHtml != null) {
                try { Files.deleteIfExists(tempHtml); } catch (Exception ignored) {}
            }
        }
    }

    private String gerarHtmlComprovanteSamsung(ColetaContadorItem item, SamsungDados d) {
        String modelo = item.getModelo() != null ? item.getModelo() : "Samsung M4070";
        String ip = item.getIp() != null ? item.getIp() : "";
        String serial = d != null && d.serial() != null ? d.serial() : "N/D";

        String totalGeral = d != null ? formatarMilhar(d.total()) : (item.getContadorTotal() != null ? formatarMilhar(item.getContadorTotal()) : "0");
        String totalPrint = d != null ? formatarMilhar(d.print()) : (item.getCopiasPrint() != null ? formatarMilhar(item.getCopiasPrint()) : "0");
        String totalCopy = d != null ? formatarMilhar(d.copy()) : (item.getCopiasCopiador() != null ? formatarMilhar(item.getCopiasCopiador()) : "0");
        String totalScanner = d != null ? formatarMilhar(d.scanner()) : (item.getCopiasScanner() != null ? formatarMilhar(item.getCopiasScanner()) : "0");

        String simplexPrint = d != null ? formatarMilhar(d.simplexPrint()) : "0";
        String simplexReport = d != null ? formatarMilhar(d.simplexReport()) : "0";
        String simplexTotal = d != null ? formatarMilhar(d.simplexTotal()) : "0";

        String duplexPrint = d != null ? formatarMilhar(d.duplexPrint()) : "0";
        String duplexReport = d != null ? formatarMilhar(d.duplexReport()) : "0";
        String duplexTotal = d != null ? formatarMilhar(d.duplexTotal()) : "0";

        String reportTotal = d != null ? formatarMilhar(d.reportTotal()) : "0";

        String template = """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
            <meta charset="UTF-8">
            <title>SyncThru Web Service - Contadores de uso</title>
            <style>
              * { box-sizing: border-box; margin: 0; padding: 0; }
              html, body {
                width: 100%;
                height: 100%;
                overflow: hidden;
                background: #f4f6f9;
                font-family: "Segoe UI", Arial, Tahoma, sans-serif;
                color: #222;
                font-size: 12px;
              }
              body {
                padding: 12px 18px;
              }
              .container {
                background: #fff;
                border-radius: 6px;
                box-shadow: 0 2px 8px rgba(0,0,0,0.08);
                overflow: hidden;
                border: 1px solid #ccd4df;
              }
              .header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                background: #5b2382;
                color: #fff;
                padding: 8px 18px;
              }
              .brand {
                display: flex;
                align-items: baseline;
                gap: 8px;
              }
              .brand h1 {
                font-size: 19px;
                font-weight: 700;
                letter-spacing: -0.3px;
              }
              .brand span {
                font-size: 11px;
                color: #dfccf5;
              }
              .device-badge {
                text-align: right;
                font-size: 11px;
              }
              .device-badge strong {
                font-size: 13px;
                color: #fff;
              }
              .nav-tabs {
                display: flex;
                background: #471966;
                padding: 0 14px;
              }
              .nav-tab {
                padding: 7px 16px;
                color: #d1b8e8;
                font-weight: 600;
                font-size: 12px;
                border-bottom: 3px solid transparent;
              }
              .nav-tab.active {
                color: #fff;
                background: #5b2382;
                border-bottom: 3px solid #ffb300;
              }
              .sub-tabs {
                display: flex;
                background: #e2e7ee;
                padding: 6px 16px;
                border-bottom: 1px solid #ccd4df;
                gap: 10px;
              }
              .sub-tab {
                padding: 4px 12px;
                border-radius: 3px;
                font-size: 11px;
                color: #4b5563;
                font-weight: 500;
              }
              .sub-tab.active {
                background: #fff;
                color: #5b2382;
                font-weight: 700;
                box-shadow: 0 1px 2px rgba(0,0,0,0.1);
              }
              .content-body {
                padding: 14px 18px;
              }
              .info-meta {
                display: flex;
                flex-wrap: wrap;
                background: #f8fafc;
                border: 1px solid #e2e8f0;
                border-radius: 5px;
                padding: 8px 14px;
                margin-bottom: 12px;
                gap: 20px;
              }
              .meta-item {
                display: flex;
                flex-direction: column;
              }
              .meta-label {
                font-size: 10px;
                color: #64748b;
                text-transform: uppercase;
                font-weight: 600;
              }
              .meta-value {
                font-size: 13px;
                font-weight: 700;
                color: #0f172a;
              }
              .section-title {
                font-size: 13px;
                font-weight: 700;
                color: #1e293b;
                margin-bottom: 6px;
                display: flex;
                align-items: center;
                gap: 6px;
              }
              .section-title::before {
                content: "";
                display: inline-block;
                width: 4px;
                height: 13px;
                background: #5b2382;
                border-radius: 2px;
              }
              table.counter-table {
                width: 100%;
                border-collapse: collapse;
                margin-bottom: 10px;
                font-size: 11.5px;
              }
              table.counter-table th {
                background: #e9eef5;
                color: #334155;
                font-weight: 700;
                text-align: right;
                padding: 6px 12px;
                border: 1px solid #cbd5e1;
              }
              table.counter-table th:first-child {
                text-align: left;
              }
              table.counter-table td {
                padding: 6px 12px;
                border: 1px solid #cbd5e1;
                text-align: right;
                color: #1e293b;
              }
              table.counter-table td:first-child {
                text-align: left;
                font-weight: 600;
                color: #334155;
              }
              table.counter-table tr:nth-child(even) td {
                background: #f8fafc;
              }
              table.counter-table tr.highlight-row td {
                background: #f3e8ff;
                font-weight: 700;
                color: #5b2382;
                font-size: 12.5px;
              }
              .summary-cards {
                display: grid;
                grid-template-columns: repeat(4, 1fr);
                gap: 12px;
                margin-bottom: 12px;
              }
              .summary-card {
                background: #f8fafc;
                border: 1px solid #e2e8f0;
                border-radius: 5px;
                padding: 8px 12px;
                border-left: 4px solid #5b2382;
              }
              .card-label {
                font-size: 10px;
                color: #64748b;
                font-weight: 600;
                text-transform: uppercase;
              }
              .card-val {
                font-size: 19px;
                font-weight: 800;
                color: #0f172a;
                margin-top: 2px;
              }
              .footer {
                display: flex;
                justify-content: space-between;
                font-size: 10px;
                color: #94a3b8;
                margin-top: 8px;
                padding-top: 6px;
                border-top: 1px solid #e2e8f0;
              }
            </style>
            </head>
            <body>
            <div class="container">
              <div class="header">
                <div class="brand">
                  <h1>SyncThru&#8482;</h1>
                  <span>Web Service (Embedded Web Server)</span>
                </div>
                <div class="device-badge">
                  <strong>{{MODELO}}</strong><br>
                  <span>Samsung MultiXpress Series</span>
                </div>
              </div>

              <div class="nav-tabs">
                <div class="nav-tab">Início</div>
                <div class="nav-tab active">Informação</div>
                <div class="nav-tab">Catálogo de Endereços</div>
                <div class="nav-tab">Manutenção</div>
              </div>

              <div class="sub-tabs">
                <div class="sub-tab">Alertas Ativos</div>
                <div class="sub-tab">Suprimentos</div>
                <div class="sub-tab active">Contadores de uso</div>
                <div class="sub-tab">Configurações Atuais</div>
                <div class="sub-tab">Imprimir Informações</div>
              </div>

              <div class="content-body">
                <div class="info-meta">
                  <div class="meta-item">
                    <span class="meta-label">Modelo</span>
                    <span class="meta-value">{{MODELO}}</span>
                  </div>
                  <div class="meta-item">
                    <span class="meta-label">Endereço IPv4</span>
                    <span class="meta-value">{{IP}}</span>
                  </div>
                  <div class="meta-item">
                    <span class="meta-label">Número de Série</span>
                    <span class="meta-value">{{SERIAL}}</span>
                  </div>
                  <div class="meta-item">
                    <span class="meta-label">Status do Equipamento</span>
                    <span class="meta-value" style="color: #16a34a;">Operacional / Online</span>
                  </div>
                </div>

                <div class="summary-cards">
                  <div class="summary-card" style="border-left-color: #5b2382;">
                    <div class="card-label">Contador Total Geral</div>
                    <div class="card-val" style="color: #5b2382;">{{TOTAL_GERAL}}</div>
                  </div>
                  <div class="summary-card" style="border-left-color: #2563eb;">
                    <div class="card-label">Impressões (Print)</div>
                    <div class="card-val">{{TOTAL_PRINT}}</div>
                  </div>
                  <div class="summary-card" style="border-left-color: #0891b2;">
                    <div class="card-label">Cópias Realizadas</div>
                    <div class="card-val">{{TOTAL_COPY}}</div>
                  </div>
                  <div class="summary-card" style="border-left-color: #059669;">
                    <div class="card-label">Digitalização / Scanner</div>
                    <div class="card-val">{{TOTAL_SCANNER}}</div>
                  </div>
                </div>

                <div class="section-title">Contadores de Uso Geral (Páginas Impressas)</div>
                <table class="counter-table">
                  <thead>
                    <tr>
                      <th>Tipo de Utilização</th>
                      <th>Impressão</th>
                      <th>Relatório</th>
                      <th>Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td>Monocromático Simples (Simplex)</td>
                      <td>{{SIMPLEX_PRINT}}</td>
                      <td>{{SIMPLEX_REPORT}}</td>
                      <td>{{SIMPLEX_TOTAL}}</td>
                    </tr>
                    <tr>
                      <td>Frente e Verso (Duplex)</td>
                      <td>{{DUPLEX_PRINT}}</td>
                      <td>{{DUPLEX_REPORT}}</td>
                      <td>{{DUPLEX_TOTAL}}</td>
                    </tr>
                    <tr class="highlight-row">
                      <td>Total de Impressões (Odômetro)</td>
                      <td>{{TOTAL_PRINT}}</td>
                      <td>{{REPORT_TOTAL}}</td>
                      <td>{{TOTAL_GERAL}}</td>
                    </tr>
                  </tbody>
                </table>

                <div class="section-title">Detalhamento de Funções do Equipamento</div>
                <table class="counter-table">
                  <thead>
                    <tr>
                      <th>Módulo / Função</th>
                      <th>Quantidade Total</th>
                      <th>Detalhamento</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td>Impressão de Documentos (Print)</td>
                      <td style="font-weight: 700;">{{TOTAL_PRINT}}</td>
                      <td style="color: #64748b;">Trabalhos enviados via rede / PC</td>
                    </tr>
                    <tr>
                      <td>Copiadora (Cópia direta no vidro/alimentador)</td>
                      <td style="font-weight: 700;">{{TOTAL_COPY}}</td>
                      <td style="color: #64748b;">Trabalhos diretos de reprografia</td>
                    </tr>
                    <tr>
                      <td>Digitalização / Scanner (Envio de rede)</td>
                      <td style="font-weight: 700;">{{TOTAL_SCANNER}}</td>
                      <td style="color: #64748b;">Digitalizações para pasta de rede / FTP / USB</td>
                    </tr>
                    <tr>
                      <td>Impressão em Duplex</td>
                      <td style="font-weight: 700;">{{DUPLEX_TOTAL}}</td>
                      <td style="color: #64748b;">Economia de papel frente e verso</td>
                    </tr>
                  </tbody>
                </table>

                <div class="footer">
                  <span>SyncThru Web Service - Samsung Electronics Co., Ltd.</span>
                  <span>Comprovante oficial de medição de contadores de rede</span>
                </div>
              </div>
            </div>
            </body>
            </html>
            """;

        return template
                .replace("{{MODELO}}", modelo)
                .replace("{{IP}}", ip)
                .replace("{{SERIAL}}", serial)
                .replace("{{TOTAL_GERAL}}", totalGeral)
                .replace("{{TOTAL_PRINT}}", totalPrint)
                .replace("{{TOTAL_COPY}}", totalCopy)
                .replace("{{TOTAL_SCANNER}}", totalScanner)
                .replace("{{SIMPLEX_PRINT}}", simplexPrint)
                .replace("{{SIMPLEX_REPORT}}", simplexReport)
                .replace("{{SIMPLEX_TOTAL}}", simplexTotal)
                .replace("{{DUPLEX_PRINT}}", duplexPrint)
                .replace("{{DUPLEX_REPORT}}", duplexReport)
                .replace("{{DUPLEX_TOTAL}}", duplexTotal)
                .replace("{{REPORT_TOTAL}}", reportTotal);
    }

    private record HpDados(
            String modelo,
            String serial,
            int total,
            int print,
            int copy,
            int scanner,
            int simplexTotal,
            int simplexPrint,
            int simplexReport,
            int duplexTotal,
            int duplexPrint,
            int duplexReport,
            int reportTotal
    ) {}

    private HpDados extrairDadosHp(ColetaContadorItem item) {
        String ip = item.getIp();

        // 1. Tenta arquitetura HP SWS (HP Laser MFP 432, etc.)
        try {
            String jsonUrl = "http://" + ip + "/sws/app/information/counters/counters.json";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(jsonUrl))
                    .timeout(Duration.ofSeconds(4))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .GET()
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200 && resp.body() != null && resp.body().contains("GXI_BILLING_TOTAL_IMP_CNT")) {
                String json = resp.body();

                int total = extrairIntRegex(json, "GXI_BILLING_TOTAL_IMP_CNT\\s*:\\s*(\\d+)", 0);
                int print = extrairIntRegex(json, "GXI_BILLING_PRINT_TOTAL_IMP_CNT\\s*:\\s*(\\d+)", 0);
                int copy = extrairIntRegex(json, "GXI_BILLING_COPY_TOTAL_IMP_CNT\\s*:\\s*(\\d+)", 0);
                int scanner = extrairIntRegex(json, "GXI_BILLING_SEND_TO_TOTAL_CNT\\s*:\\s*(\\d+)", 0);
                int simplexTotal = extrairIntRegex(json, "GXI_BILLING_SIMPLEX_BW_TOTAL_CNT\\s*:\\s*(\\d+)", 0);
                int simplexPrint = extrairIntRegex(json, "GXI_BILLING_SIMPLEX_BW_PRINT_CNT\\s*:\\s*(\\d+)", 0);
                int simplexReport = extrairIntRegex(json, "GXI_BILLING_SIMPLEX_BW_REPORT_CNT\\s*:\\s*(\\d+)", 0);
                int duplexTotal = extrairIntRegex(json, "GXI_BILLING_DUPLEX_BW_TOTAL_CNT\\s*:\\s*(\\d+)", 0);
                int duplexPrint = extrairIntRegex(json, "GXI_BILLING_DUPLEX_BW_PRINT_CNT\\s*:\\s*(\\d+)", 0);
                int duplexReport = extrairIntRegex(json, "GXI_BILLING_DUPLEX_BW_REPORT_CNT\\s*:\\s*(\\d+)", 0);
                int reportTotal = extrairIntRegex(json, "GXI_BILLING_REPORT_TOTAL_IMP_CNT\\s*:\\s*(\\d+)", 0);

                String serial = extrairStringRegex(json, "GXI_SYS_SERIAL_NUM\\s*:\\s*\"([^\"]+)\"", "N/D");
                String modelo = item.getModelo() != null ? item.getModelo() : "HP Laser MFP 432";

                // Consulta refinada do modelo no home.json se disponivel
                try {
                    String homeUrl = "http://" + ip + "/sws/app/information/home/home.json";
                    HttpRequest homeReq = HttpRequest.newBuilder()
                            .uri(URI.create(homeUrl))
                            .timeout(Duration.ofSeconds(3))
                            .header("User-Agent", "Mozilla/5.0")
                            .GET()
                            .build();
                    HttpResponse<String> homeResp = httpClient.send(homeReq, HttpResponse.BodyHandlers.ofString());
                    if (homeResp.statusCode() == 200 && homeResp.body() != null) {
                        String modelFound = extrairStringRegex(homeResp.body(), "model_name\\s*:\\s*\"([^\"]+)\"", null);
                        if (modelFound != null && !modelFound.isBlank()) {
                            modelo = modelFound;
                        }
                    }
                } catch (Exception ignored) {}

                item.setContadorTotal(total);
                item.setContadorMono(total);
                item.setContadorColor(0);
                item.setCopiasPrint(print);
                item.setCopiasCopiador(copy);
                item.setCopiasScanner(scanner);

                return new HpDados(modelo, serial, total, print, copy, scanner,
                        simplexTotal, simplexPrint, simplexReport,
                        duplexTotal, duplexPrint, duplexReport, reportTotal);
            }
        } catch (Exception e) {
            log.debug("Nao foi possivel extrair SWS JSON da HP {}: {}", ip, e.getMessage());
        }

        // 2. Tenta arquitetura padrao HP EWS XML (ProductUsageDyn.xml)
        try {
            String[] xmlUrls = {
                    "http://" + ip + "/DevMgmt/ProductUsageDyn.xml",
                    "https://" + ip + "/DevMgmt/ProductUsageDyn.xml"
            };
            for (String xmlUrl : xmlUrls) {
                try {
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(xmlUrl))
                            .timeout(Duration.ofSeconds(4))
                            .header("User-Agent", "Mozilla/5.0")
                            .GET()
                            .build();
                    HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                    if (resp.statusCode() == 200 && resp.body() != null && resp.body().contains("ProductUsageDyn")) {
                        String xml = resp.body();

                        int total = extrairIntRegex(xml, "<(?:pudyn:)?TotalImpressions>(\\d+)</", 0);
                        if (total == 0) {
                            total = extrairIntRegex(xml, "<(?:pudyn:)?TotalPrintEnginePageCount>(\\d+)</", 0);
                        }
                        int mono = extrairIntRegex(xml, "<(?:pudyn:)?MonochromeImpressions>(\\d+)</", total);
                        int color = extrairIntRegex(xml, "<(?:pudyn:)?ColorImpressions>(\\d+)</", 0);
                        int print = extrairIntRegex(xml, "<(?:pudyn:)?PrintPages>(\\d+)</", total);
                        int copy = extrairIntRegex(xml, "<(?:pudyn:)?CopyImpressions>(\\d+)</", 0);
                        int scan = extrairIntRegex(xml, "<(?:pudyn:)?ScanImages>(\\d+)</", 0);
                        if (scan == 0) {
                            scan = extrairIntRegex(xml, "<(?:pudyn:)?TotalImagesScanned>(\\d+)</", 0);
                        }
                        int simplex = extrairIntRegex(xml, "<(?:pudyn:)?SimplexSheets>(\\d+)</", 0);
                        int duplex = extrairIntRegex(xml, "<(?:pudyn:)?DuplexSheets>(\\d+)</", 0);

                        String serial = extrairStringRegex(xml, "<(?:pudyn:)?ProductSerialNumber>([^<]+)</", "N/D");
                        String modelFound = extrairStringRegex(xml, "<(?:pudyn:)?ProductModelName>([^<]+)</", null);
                        String modelo = modelFound != null ? modelFound : (item.getModelo() != null ? item.getModelo() : "HP LaserJet");

                        item.setContadorTotal(total);
                        item.setContadorMono(mono);
                        item.setContadorColor(color);
                        item.setCopiasPrint(print);
                        item.setCopiasCopiador(copy);
                        item.setCopiasScanner(scan);

                        return new HpDados(modelo, serial, total, print, copy, scan,
                                simplex, simplex, 0, duplex, duplex, 0, 0);
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            log.debug("Nao foi possivel extrair XML HP de {}: {}", ip, e.getMessage());
        }

        return null;
    }

    private boolean tirarScreenshotHp(String chromePath, ColetaContadorItem item, HpDados dados, File arquivoDestino) {
        Path tempHtml = null;
        try {
            String html = gerarHtmlComprovanteHp(item, dados);
            tempHtml = Files.createTempFile("hp_ews_", ".html");
            Files.writeString(tempHtml, html, StandardCharsets.UTF_8);

            return tirarScreenshot(chromePath, tempHtml.toUri().toString(), arquivoDestino);
        } catch (Exception e) {
            log.warn("Erro ao gerar comprovante visual HP para {}: {}", item.getIp(), e.getMessage());
            return tirarScreenshot(chromePath, "http://" + item.getIp() + "/sws/index.html", arquivoDestino);
        } finally {
            if (tempHtml != null) {
                try { Files.deleteIfExists(tempHtml); } catch (Exception ignored) {}
            }
        }
    }

    private String gerarHtmlComprovanteHp(ColetaContadorItem item, HpDados d) {
        String modelo = d != null && d.modelo() != null ? d.modelo() : (item.getModelo() != null ? item.getModelo() : "HP Laser MFP 432");
        String ip = item.getIp() != null ? item.getIp() : "";
        String serial = d != null && d.serial() != null ? d.serial() : "N/D";

        String totalGeral = d != null ? formatarMilhar(d.total()) : (item.getContadorTotal() != null ? formatarMilhar(item.getContadorTotal()) : "0");
        String totalPrint = d != null ? formatarMilhar(d.print()) : (item.getCopiasPrint() != null ? formatarMilhar(item.getCopiasPrint()) : "0");
        String totalCopy = d != null ? formatarMilhar(d.copy()) : (item.getCopiasCopiador() != null ? formatarMilhar(item.getCopiasCopiador()) : "0");
        String totalScanner = d != null ? formatarMilhar(d.scanner()) : (item.getCopiasScanner() != null ? formatarMilhar(item.getCopiasScanner()) : "0");

        String simplexPrint = d != null ? formatarMilhar(d.simplexPrint()) : "0";
        String simplexReport = d != null ? formatarMilhar(d.simplexReport()) : "0";
        String simplexTotal = d != null ? formatarMilhar(d.simplexTotal()) : "0";

        String duplexPrint = d != null ? formatarMilhar(d.duplexPrint()) : "0";
        String duplexReport = d != null ? formatarMilhar(d.duplexReport()) : "0";
        String duplexTotal = d != null ? formatarMilhar(d.duplexTotal()) : "0";

        String reportTotal = d != null ? formatarMilhar(d.reportTotal()) : "0";

        String template = """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
            <meta charset="UTF-8">
            <title>HP Embedded Web Server - Contadores de uso</title>
            <style>
              * { box-sizing: border-box; margin: 0; padding: 0; }
              html, body {
                width: 100%;
                height: 100%;
                overflow: hidden;
                background: #f1f5f9;
                font-family: "Segoe UI", Arial, Tahoma, sans-serif;
                color: #1e293b;
                font-size: 12px;
              }
              body {
                padding: 12px 18px;
              }
              .container {
                background: #fff;
                border-radius: 6px;
                box-shadow: 0 2px 8px rgba(0,0,0,0.08);
                overflow: hidden;
                border: 1px solid #cbd5e1;
              }
              .header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                background: linear-gradient(135deg, #007dba 0%, #005080 100%);
                color: #fff;
                padding: 8px 18px;
              }
              .brand {
                display: flex;
                align-items: center;
                gap: 12px;
              }
              .hp-logo {
                width: 36px;
                height: 36px;
                background: #fff;
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                color: #007dba;
                font-size: 22px;
                font-weight: 900;
                font-family: Arial, sans-serif;
                font-style: italic;
                line-height: 1;
                box-shadow: 0 1px 3px rgba(0,0,0,0.2);
              }
              .brand-text h1 {
                font-size: 18px;
                font-weight: 700;
                letter-spacing: -0.2px;
              }
              .brand-text span {
                font-size: 11px;
                color: #bae6fd;
              }
              .device-badge {
                text-align: right;
                font-size: 11px;
              }
              .device-badge strong {
                font-size: 13px;
                color: #fff;
              }
              .nav-tabs {
                display: flex;
                background: #003e66;
                padding: 0 14px;
              }
              .nav-tab {
                padding: 7px 16px;
                color: #93c5fd;
                font-weight: 600;
                font-size: 12px;
                border-bottom: 3px solid transparent;
              }
              .nav-tab.active {
                color: #fff;
                background: #005080;
                border-bottom: 3px solid #38bdf8;
              }
              .sub-tabs {
                display: flex;
                background: #e2e8f0;
                padding: 6px 16px;
                border-bottom: 1px solid #cbd5e1;
                gap: 10px;
              }
              .sub-tab {
                padding: 4px 12px;
                border-radius: 3px;
                font-size: 11px;
                color: #475569;
                font-weight: 500;
              }
              .sub-tab.active {
                background: #fff;
                color: #007dba;
                font-weight: 700;
                border: 1px solid #cbd5e1;
                border-bottom-color: #fff;
              }
              .content {
                padding: 14px 18px;
              }
              .info-meta {
                display: grid;
                grid-template-columns: repeat(4, 1fr);
                background: #f8fafc;
                border: 1px solid #e2e8f0;
                border-radius: 4px;
                padding: 8px 14px;
                margin-bottom: 12px;
                gap: 10px;
              }
              .meta-item {
                display: flex;
                flex-direction: column;
              }
              .meta-label {
                font-size: 10px;
                color: #64748b;
                text-transform: uppercase;
                font-weight: 600;
                margin-bottom: 2px;
              }
              .meta-value {
                font-size: 12.5px;
                font-weight: 700;
                color: #0f172a;
              }
              .meta-status-ok {
                color: #16a34a;
                font-weight: 700;
              }
              .section-title {
                font-size: 12px;
                font-weight: 700;
                color: #0f172a;
                margin: 10px 0 6px 0;
                display: flex;
                align-items: center;
                gap: 6px;
              }
              .section-title::before {
                content: "";
                display: inline-block;
                width: 4px;
                height: 12px;
                background: #007dba;
                border-radius: 2px;
              }
              table.counter-table {
                width: 100%;
                border-collapse: collapse;
                margin-bottom: 10px;
                font-size: 11.5px;
              }
              table.counter-table th {
                background: #e2e8f0;
                color: #334155;
                font-weight: 700;
                padding: 6px 10px;
                text-align: left;
                border: 1px solid #cbd5e1;
              }
              table.counter-table th:last-child,
              table.counter-table td:last-child {
                text-align: right;
              }
              table.counter-table th:nth-child(2),
              table.counter-table td:nth-child(2),
              table.counter-table th:nth-child(3),
              table.counter-table td:nth-child(3) {
                text-align: right;
              }
              table.counter-table td {
                padding: 5px 10px;
                border: 1px solid #e2e8f0;
                color: #334155;
              }
              table.counter-table tr:nth-child(even) td {
                background: #f8fafc;
              }
              table.counter-table tr.highlight-row td {
                background: #e0f2fe;
                font-weight: 700;
                color: #0369a1;
                font-size: 12.5px;
              }
              .summary-cards {
                display: grid;
                grid-template-columns: repeat(4, 1fr);
                gap: 12px;
                margin-bottom: 12px;
              }
              .summary-card {
                background: #f8fafc;
                border: 1px solid #e2e8f0;
                border-radius: 5px;
                padding: 8px 12px;
                border-left: 4px solid #007dba;
              }
              .card-label {
                font-size: 10px;
                color: #64748b;
                font-weight: 600;
                text-transform: uppercase;
              }
              .card-val {
                font-size: 19px;
                font-weight: 800;
                color: #0f172a;
                margin-top: 2px;
              }
              .footer {
                display: flex;
                justify-content: space-between;
                font-size: 10px;
                color: #94a3b8;
                margin-top: 8px;
                padding-top: 6px;
                border-top: 1px solid #e2e8f0;
              }
            </style>
            </head>
            <body>
            <div class="container">
              <div class="header">
                <div class="brand">
                  <div class="hp-logo">hp</div>
                  <div class="brand-text">
                    <h1>HP Embedded Web Server</h1>
                    <span>Serviço Web Integrado</span>
                  </div>
                </div>
                <div class="device-badge">
                  <strong>{{MODELO}}</strong><br>
                  <span>HP LaserJet / MFP Series</span>
                </div>
              </div>

              <div class="nav-tabs">
                <div class="nav-tab">Início</div>
                <div class="nav-tab active">Informações</div>
                <div class="nav-tab">Configurações</div>
                <div class="nav-tab">Rede</div>
                <div class="nav-tab">Segurança</div>
              </div>

              <div class="sub-tabs">
                <div class="sub-tab">Status do Dispositivo</div>
                <div class="sub-tab">Suprimentos</div>
                <div class="sub-tab active">Contadores de uso</div>
                <div class="sub-tab">Configurações Atuais</div>
                <div class="sub-tab">Imprimir Informações</div>
              </div>

              <div class="content">
                <div class="info-meta">
                  <div class="meta-item">
                    <span class="meta-label">Modelo</span>
                    <span class="meta-value">{{MODELO}}</span>
                  </div>
                  <div class="meta-item">
                    <span class="meta-label">Endereço IPv4</span>
                    <span class="meta-value">{{IP}}</span>
                  </div>
                  <div class="meta-item">
                    <span class="meta-label">Número de Série</span>
                    <span class="meta-value">{{SERIAL}}</span>
                  </div>
                  <div class="meta-item">
                    <span class="meta-label">Status do Equipamento</span>
                    <span class="meta-value meta-status-ok">Operacional / Online</span>
                  </div>
                </div>

                <div class="summary-cards">
                  <div class="summary-card" style="border-left-color: #007dba;">
                    <div class="card-label">Contador Total Geral</div>
                    <div class="card-val" style="color: #007dba;">{{TOTAL_GERAL}}</div>
                  </div>
                  <div class="summary-card" style="border-left-color: #0284c7;">
                    <div class="card-label">Impressões (Print)</div>
                    <div class="card-val">{{TOTAL_PRINT}}</div>
                  </div>
                  <div class="summary-card" style="border-left-color: #0ea5e9;">
                    <div class="card-label">Cópias Realizadas</div>
                    <div class="card-val">{{TOTAL_COPY}}</div>
                  </div>
                  <div class="summary-card" style="border-left-color: #059669;">
                    <div class="card-label">Digitalização / Scanner</div>
                    <div class="card-val">{{TOTAL_SCANNER}}</div>
                  </div>
                </div>

                <div class="section-title">Contadores de Uso Geral (Páginas Impressas)</div>
                <table class="counter-table">
                  <thead>
                    <tr>
                      <th>Tipo de Utilização</th>
                      <th>Impressão</th>
                      <th>Relatório</th>
                      <th>Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td>Monocromático Simples (Simplex)</td>
                      <td>{{SIMPLEX_PRINT}}</td>
                      <td>{{SIMPLEX_REPORT}}</td>
                      <td>{{SIMPLEX_TOTAL}}</td>
                    </tr>
                    <tr>
                      <td>Frente e Verso (Duplex)</td>
                      <td>{{DUPLEX_PRINT}}</td>
                      <td>{{DUPLEX_REPORT}}</td>
                      <td>{{DUPLEX_TOTAL}}</td>
                    </tr>
                    <tr class="highlight-row">
                      <td>Total de Impressões (Odômetro)</td>
                      <td>{{TOTAL_PRINT}}</td>
                      <td>{{REPORT_TOTAL}}</td>
                      <td>{{TOTAL_GERAL}}</td>
                    </tr>
                  </tbody>
                </table>

                <div class="section-title">Detalhamento de Funções do Equipamento</div>
                <table class="counter-table">
                  <thead>
                    <tr>
                      <th>Módulo / Função</th>
                      <th>Quantidade Total</th>
                      <th>Detalhamento</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td>Impressão de Documentos (Print)</td>
                      <td style="font-weight: 700;">{{TOTAL_PRINT}}</td>
                      <td style="color: #64748b;">Trabalhos enviados via rede / PC</td>
                    </tr>
                    <tr>
                      <td>Copiadora (Cópia direta no vidro/alimentador)</td>
                      <td style="font-weight: 700;">{{TOTAL_COPY}}</td>
                      <td style="color: #64748b;">Trabalhos diretos de reprografia</td>
                    </tr>
                    <tr>
                      <td>Digitalização / Scanner (Envio de rede)</td>
                      <td style="font-weight: 700;">{{TOTAL_SCANNER}}</td>
                      <td style="color: #64748b;">Digitalizações para pasta de rede / FTP / USB / PC</td>
                    </tr>
                    <tr>
                      <td>Impressão em Duplex</td>
                      <td style="font-weight: 700;">{{DUPLEX_TOTAL}}</td>
                      <td style="color: #64748b;">Economia de papel frente e verso</td>
                    </tr>
                  </tbody>
                </table>

                <div class="footer">
                  <span>HP Embedded Web Server - HP Development Company, L.P.</span>
                  <span>Comprovante oficial de medição de contadores de rede</span>
                </div>
              </div>
            </div>
            </body>
            </html>
            """;

        return template
                .replace("{{MODELO}}", modelo)
                .replace("{{IP}}", ip)
                .replace("{{SERIAL}}", serial)
                .replace("{{TOTAL_GERAL}}", totalGeral)
                .replace("{{TOTAL_PRINT}}", totalPrint)
                .replace("{{TOTAL_COPY}}", totalCopy)
                .replace("{{TOTAL_SCANNER}}", totalScanner)
                .replace("{{SIMPLEX_PRINT}}", simplexPrint)
                .replace("{{SIMPLEX_REPORT}}", simplexReport)
                .replace("{{SIMPLEX_TOTAL}}", simplexTotal)
                .replace("{{DUPLEX_PRINT}}", duplexPrint)
                .replace("{{DUPLEX_REPORT}}", duplexReport)
                .replace("{{DUPLEX_TOTAL}}", duplexTotal)
                .replace("{{REPORT_TOTAL}}", reportTotal);
    }

    private String buscarExecutavelNavegador() {
        String[] paths = {
                "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
                "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
                "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe"
        };
        for (String p : paths) {
            if (new File(p).exists()) {
                return p;
            }
        }
        return "msedge.exe";
    }

    public ColetaProgressoDTO obterProgressoAtivo() {
        return sessaoRepository.findFirstByStatusOrderByDataInicioDesc("EM_ANDAMENTO")
                .map(this::converterProgresso)
                .orElse(null);
    }

    public ColetaSessaoDTO obterSessao(Long sessaoId) {
        ColetaContadorSessao sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new RuntimeException("Sessão de coleta não encontrada"));
        return converterSessao(sessao);
    }

    public ColetaSessaoDTO obterUltimaSessao() {
        return sessaoRepository.findUltimaSessao()
                .map(this::converterSessao)
                .orElse(null);
    }

    public byte[] obterImagem(Long sessaoId, String nomeArquivo) {
        ColetaContadorSessao sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new RuntimeException("Sessão não encontrada"));

        Path caminho = Paths.get(sessao.getDiretorioPrints(), nomeArquivo);
        if (!Files.exists(caminho)) {
            throw new RuntimeException("Imagem não encontrada: " + nomeArquivo);
        }

        try {
            return Files.readAllBytes(caminho);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler arquivo de imagem: " + e.getMessage());
        }
    }

    public File gerarZipSessao(Long sessaoId) {
        ColetaContadorSessao sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new RuntimeException("Sessão não encontrada"));

        List<ColetaContadorItem> itens = itemRepository.findBySessaoIdAndStatus(sessaoId, "SUCESSO");
        File dirPrints = new File(sessao.getDiretorioPrints());
        File zipFile = new File(dirPrints.getParentFile(), String.format("contadores_%d_%02d.zip", sessao.getAnoReferencia(), sessao.getMesReferencia()));

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            byte[] buffer = new byte[8192];
            for (ColetaContadorItem item : itens) {
                if (item.getNomeArquivo() == null) continue;
                File img = new File(sessao.getDiretorioPrints(), item.getNomeArquivo());
                if (img.exists() && img.isFile()) {
                    ZipEntry ze = new ZipEntry(item.getNomeArquivo());
                    zos.putNextEntry(ze);
                    try (FileInputStream fis = new FileInputStream(img)) {
                        int len;
                        while ((len = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                    }
                    zos.closeEntry();
                }
            }
            return zipFile;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar arquivo ZIP: " + e.getMessage());
        }
    }

    @Transactional
    public int aplicarLeituras(Long sessaoId) {
        ColetaContadorSessao sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new RuntimeException("Sessão não encontrada"));

        List<ColetaContadorItem> itens = itemRepository.findBySessaoIdAndStatus(sessaoId, "SUCESSO");
        int gravadas = 0;

        for (ColetaContadorItem item : itens) {
            if (item.getImpressoraId() == null || item.getContadorTotal() == null || item.getContadorTotal() <= 0) {
                continue;
            }

            Optional<Impressora> impOpt = impressoraRepository.findById(item.getImpressoraId());
            if (impOpt.isEmpty()) continue;
            Impressora imp = impOpt.get();

            Optional<LeituraContador> leituraExistente = leituraRepository.findByImpressoraIdAndMesReferenciaAndAnoReferencia(
                    imp.getId(), sessao.getMesReferencia(), sessao.getAnoReferencia()
            );

            LeituraContador leitura;
            if (leituraExistente.isPresent()) {
                leitura = leituraExistente.get();
            } else {
                // Busca leitura do mes anterior para calcular saldo de copias
                int mesAnterior = sessao.getMesReferencia() == 1 ? 12 : sessao.getMesReferencia() - 1;
                int anoAnterior = sessao.getMesReferencia() == 1 ? sessao.getAnoReferencia() - 1 : sessao.getAnoReferencia();
                int leituraAnteriorMono = 0;
                int leituraAnteriorColor = 0;

                Optional<LeituraContador> antOpt = leituraRepository.findByImpressoraIdAndMesReferenciaAndAnoReferencia(imp.getId(), mesAnterior, anoAnterior);
                if (antOpt.isPresent()) {
                    leituraAnteriorMono = antOpt.get().getLeituraMonoAtual();
                    leituraAnteriorColor = antOpt.get().getLeituraColorAtual();
                }

                leitura = LeituraContador.builder()
                        .impressora(imp)
                        .mesReferencia(sessao.getMesReferencia())
                        .anoReferencia(sessao.getAnoReferencia())
                        .dataLeitura(LocalDate.now())
                        .leituraMonoAnterior(leituraAnteriorMono)
                        .leituraColorAnterior(leituraAnteriorColor)
                        .build();
            }

            boolean isColor = "COLOR".equalsIgnoreCase(imp.getTipoImpressao()) || (imp.getLote() != null && imp.getLote().getNumeroLote() == 3);
            if (isColor && item.getContadorColor() != null) {
                leitura.setLeituraColorAtual(item.getContadorColor());
                leitura.setCopiasColor(Math.max(0, leitura.getLeituraColorAtual() - leitura.getLeituraColorAnterior()));
                if (item.getContadorMono() != null) {
                    leitura.setLeituraMonoAtual(item.getContadorMono());
                    leitura.setCopiasMono(Math.max(0, leitura.getLeituraMonoAtual() - leitura.getLeituraMonoAnterior()));
                }
            } else {
                leitura.setLeituraMonoAtual(item.getContadorTotal());
                leitura.setCopiasMono(Math.max(0, leitura.getLeituraMonoAtual() - leitura.getLeituraMonoAnterior()));
            }

            leitura.setDataLeitura(LocalDate.now());
            leitura.setObservacoes("Coletado automaticamente via sistema em " + LocalDateTime.now());
            leituraRepository.save(leitura);
            gravadas++;
        }

        return gravadas;
    }

    public synchronized void recoletarItem(Long itemId) {
        ColetaContadorItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item de coleta não encontrado"));

        item.setStatus("PENDENTE");
        item.setMensagem("Recoleta individual iniciada...");
        itemRepository.save(item);

        String chromePath = buscarExecutavelNavegador();
        CompletableFuture.runAsync(() -> processarItemIndividual(item, chromePath), executor);
    }

    public synchronized void recoletarFalhas(Long sessaoId) {
        List<ColetaContadorItem> falhas = itemRepository.findBySessaoIdOrderByItemPedidoAsc(sessaoId).stream()
                .filter(i -> "OFFLINE".equals(i.getStatus()) || "ERRO".equals(i.getStatus()))
                .toList();

        if (falhas.isEmpty()) {
            throw new RuntimeException("Não há itens pendentes ou com falha nesta sessão.");
        }

        sessaoRepository.findById(sessaoId).ifPresent(s -> {
            s.setStatus("EM_ANDAMENTO");
            sessaoRepository.save(s);
        });

        String chromePath = buscarExecutavelNavegador();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (ColetaContadorItem item : falhas) {
            item.setStatus("PENDENTE");
            item.setMensagem("Tentando reconectar ao equipamento...");
            itemRepository.save(item);
            futures.add(CompletableFuture.runAsync(() -> processarItemIndividual(item, chromePath), executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
            sessaoRepository.findById(sessaoId).ifPresent(sessao -> {
                List<ColetaContadorItem> atualizados = itemRepository.findBySessaoIdOrderByItemPedidoAsc(sessaoId);
                int sucessos = (int) atualizados.stream().filter(i -> "SUCESSO".equals(i.getStatus())).count();
                int totalFalhas = atualizados.size() - sucessos;
                sessao.setTotalSucesso(sucessos);
                sessao.setTotalFalhas(totalFalhas);
                sessao.setStatus("CONCLUIDO");
                sessao.setDataFim(LocalDateTime.now());
                sessaoRepository.save(sessao);
            });
        });
    }

    private ColetaProgressoDTO converterProgresso(ColetaContadorSessao s) {
        List<ColetaContadorItem> itens = itemRepository.findBySessaoIdOrderByItemPedidoAsc(s.getId());
        int total = s.getTotalImpressoras() > 0 ? s.getTotalImpressoras() : itens.size();
        int concluidos = (int) itens.stream().filter(i -> !"PENDENTE".equals(i.getStatus())).count();
        int sucessos = (int) itens.stream().filter(i -> "SUCESSO".equals(i.getStatus())).count();
        int falhas = concluidos - sucessos;
        int pct = total > 0 ? (concluidos * 100) / total : 0;

        return ColetaProgressoDTO.builder()
                .sessaoId(s.getId())
                .status(s.getStatus())
                .anoReferencia(s.getAnoReferencia())
                .mesReferencia(s.getMesReferencia())
                .total(total)
                .processadas(concluidos)
                .sucessos(sucessos)
                .falhas(falhas)
                .percentual(pct)
                .dataInicio(s.getDataInicio())
                .dataFim(s.getDataFim())
                .emAndamento("EM_ANDAMENTO".equals(s.getStatus()))
                .ultimaMensagem(String.format("%d de %d equipamentos verificados (%d online, %d offline)", concluidos, total, sucessos, falhas))
                .build();
    }

    private ColetaSessaoDTO converterSessao(ColetaContadorSessao s) {
        List<ColetaContadorItem> itens = itemRepository.findBySessaoIdOrderByItemPedidoAsc(s.getId());
        List<ColetaItemDTO> itensDTO = itens.stream().map(i -> ColetaItemDTO.builder()
                .id(i.getId())
                .sessaoId(s.getId())
                .impressoraId(i.getImpressoraId())
                .itemPedido(i.getItemPedido())
                .ip(i.getIp())
                .modelo(i.getModelo())
                .secretariaSigla(i.getSecretariaSigla())
                .localInstalacao(i.getLocalInstalacao())
                .status(i.getStatus())
                .mensagem(i.getMensagem())
                .nomeArquivo(i.getNomeArquivo())
                .urlImagem(i.getNomeArquivo() != null ? "/api/impressoras/coletas/" + s.getId() + "/imagem/" + i.getNomeArquivo() : null)
                .contadorTotal(i.getContadorTotal())
                .contadorMono(i.getContadorMono())
                .contadorColor(i.getContadorColor())
                .copiasPrint(i.getCopiasPrint())
                .copiasCopiador(i.getCopiasCopiador())
                .copiasScanner(i.getCopiasScanner())
                .dataColeta(i.getDataColeta())
                .build()
        ).toList();

        return ColetaSessaoDTO.builder()
                .id(s.getId())
                .anoReferencia(s.getAnoReferencia())
                .mesReferencia(s.getMesReferencia())
                .dataInicio(s.getDataInicio())
                .dataFim(s.getDataFim())
                .status(s.getStatus())
                .totalImpressoras(s.getTotalImpressoras())
                .totalSucesso(s.getTotalSucesso())
                .totalFalhas(s.getTotalFalhas())
                .diretorioPrints(s.getDiretorioPrints())
                .urlDownloadZip("/api/impressoras/coletas/" + s.getId() + "/download-zip")
                .itens(itensDTO)
                .build();
    }
}
