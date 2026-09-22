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

    public synchronized ColetaProgressoDTO iniciarColeta(IniciarColetaRequestDTO request) {
        // Verifica se ja ha uma coleta em andamento
        Optional<ColetaContadorSessao> emAndamento = sessaoRepository.findFirstByStatusOrderByDataInicioDesc("EM_ANDAMENTO");
        if (emAndamento.isPresent()) {
            return converterProgresso(emAndamento.get());
        }

        List<InstalacaoImpressora> instalacoes = instalacaoRepository.findAllAtivasWithDetails();

        // Filtra instalacoes que tenham impressora com IP
        List<InstalacaoImpressora> candidatas = instalacoes.stream()
                .filter(inst -> inst.getImpressora() != null &&
                                inst.getImpressora().getIp() != null &&
                                !inst.getImpressora().getIp().isBlank() &&
                                Boolean.TRUE.equals(inst.getImpressora().getAtivo()))
                .filter(inst -> request.getSecretariaId() == null ||
                                (inst.getSecretaria() != null && inst.getSecretaria().getId().equals(request.getSecretariaId())))
                .filter(inst -> request.getEmpenhoId() == null ||
                                (inst.getEmpenho() != null && inst.getEmpenho().getId().equals(request.getEmpenhoId())))
                .sorted(Comparator.comparing(inst -> inst.getImpressora().getItemPedido() != null ? inst.getImpressora().getItemPedido() : 999))
                .toList();

        if (candidatas.isEmpty()) {
            throw new RuntimeException("Nenhuma impressora ativa com IP encontrada para os critérios informados.");
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
        String url = resolverUrlPainel(ip, modelo);
        boolean isHttps = url.startsWith("https://");
        int porta = isHttps ? 443 : 80;

        // 1. Verificacao rapida de socket TCP (Fail-fast em 2.5s)
        if (!isPortaAberta(ip, porta, 2500)) {
            // Tenta porta 80 caso a 443 nao responda
            if (isHttps && isPortaAberta(ip, 80, 1500)) {
                url = "http://" + ip;
                porta = 80;
            } else {
                item.setStatus("OFFLINE");
                item.setMensagem("Dispositivo desligado ou sem resposta na rede (timeout na porta " + porta + ")");
                item.setDataColeta(LocalDateTime.now());
                itemRepository.save(item);
                atualizarProgressoParcial(item.getSessao().getId());
                return;
            }
        }

        // 2. Extrai dados do contador via HTML
        extrairDadosContador(item, url);

        // 3. Captura screenshot amplo via Chromium headless
        File arquivoDestino = new File(item.getCaminhoArquivo());
        boolean printGerado = false;
        if (chromePath != null) {
            printGerado = tirarScreenshot(chromePath, url, arquivoDestino);
        }

        if (printGerado) {
            item.setStatus("SUCESSO");
            item.setMensagem("Contadores e comprovante visual capturados com sucesso.");
        } else if (item.getContadorTotal() != null && item.getContadorTotal() > 0) {
            item.setStatus("SUCESSO");
            item.setMensagem("Contadores lidos com sucesso (sem print visual do navegador).");
        } else {
            item.setStatus("ERRO");
            item.setMensagem("Painel respondeu mas os contadores não puderam ser extraídos.");
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

    private String resolverUrlPainel(String ip, String modelo) {
        if (modelo.contains("M320") || modelo.contains("3710") || modelo.contains("P 311") || modelo.contains("3510") || modelo.contains("377")) {
            return "https://" + ip + "/counter.asp?Lang=pt";
        }
        if (modelo.contains("C2003") || modelo.contains("C2004") || modelo.contains("501") || modelo.contains("RICOH")) {
            return "http://" + ip + "/web/guest/br/websys/status/getUnificationCounter.cgi";
        }
        if (modelo.contains("PANTUM")) {
            return "http://" + ip + "/index.html";
        }
        if (modelo.contains("SAMSUNG") || modelo.contains("M4070")) {
            return "http://" + ip + "/sws/app/information/counters/counters.htm";
        }
        return "http://" + ip;
    }

    private boolean tirarScreenshot(String chromePath, String url, File arquivoDestino) {
        try {
            if (arquivoDestino.getParentFile() != null) {
                arquivoDestino.getParentFile().mkdirs();
            }

            List<String> command = List.of(
                    chromePath,
                    "--headless=new",
                    "--disable-gpu",
                    "--no-sandbox",
                    "--ignore-certificate-errors",
                    "--window-size=1600,1400",
                    "--hide-scrollbars",
                    "--virtual-time-budget=4000",
                    "--screenshot=" + arquivoDestino.getAbsolutePath(),
                    url
            );

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(15, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }

            return arquivoDestino.exists() && arquivoDestino.length() > 1024;
        } catch (Exception e) {
            log.warn("Erro ao tirar screenshot de {}: {}", url, e.getMessage());
            return false;
        }
    }

    private void extrairDadosContador(ColetaContadorItem item, String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(4))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .GET()
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 400) return;

            String html = resp.body();

            // 1. Formato Ricoh BSA (counter.asp)
            // Ex: Total de páginas</td>...:</td>...72342</td>
            Pattern patTotalBsa = Pattern.compile("Total de p[aá]g(?:inas)?.*?<td[^>]*>.*?(\\d{1,8})\\s*</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher mTotalBsa = patTotalBsa.matcher(html);
            if (mTotalBsa.find()) {
                item.setContadorTotal(Integer.parseInt(mTotalBsa.group(1).trim()));
            }

            Pattern patPrint = Pattern.compile("Impressora.*?(\\d{1,8})\\s*</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher mPrint = patPrint.matcher(html);
            if (mPrint.find()) {
                item.setCopiasPrint(Integer.parseInt(mPrint.group(1).trim()));
            }

            Pattern patScanner = Pattern.compile("Scanner.*?(\\d{1,8})\\s*</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher mScanner = patScanner.matcher(html);
            if (mScanner.find()) {
                item.setCopiasScanner(Integer.parseInt(mScanner.group(1).trim()));
            }

            Pattern patCopiador = Pattern.compile("Copiador(?:a)?.*?(\\d{1,8})\\s*</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher mCopiador = patCopiador.matcher(html);
            if (mCopiador.find()) {
                item.setCopiasCopiador(Integer.parseInt(mCopiador.group(1).trim()));
            }

            // 2. Formato Ricoh WIM (getUnificationCounter.cgi)
            // Ex: Preto e branco</td><td nowrap>:</td><td nowrap>22553</td>
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

        String chromePath = buscarExecutavelNavegador();
        for (ColetaContadorItem item : falhas) {
            item.setStatus("PENDENTE");
            item.setMensagem("Tentando reconectar ao equipamento...");
            itemRepository.save(item);
            CompletableFuture.runAsync(() -> processarItemIndividual(item, chromePath), executor);
        }
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
