
package br.pucminas.iceibank.repository;
import br.pucminas.iceibank.service.RelogioVetorial;
import br.pucminas.iceibank.config.DataPathConfig;
import br.pucminas.iceibank.exception.ApiException;
import br.pucminas.iceibank.model.Conta;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


@Repository
public class ContaRepositoryEmMemoria implements ContaRepository {

    private final ConcurrentMap<Long, Conta> contas =
            new ConcurrentHashMap<>();

    private final Map<Long, Set<String>> idsProcessados =
            new HashMap<>();

    private final Map<Long, int[]> ultimoVetorOperacao =
            new HashMap<>();
    // Versão persistente de cada conta
    private final Map<Long, Long> versoesContas =
            new HashMap<>();

    private final ObjectMapper objectMapper;
    private final Path arquivoContas;

    public ContaRepositoryEmMemoria(
            ObjectMapper objectMapper,
            DataPathConfig dataPathConfig) {

        this.objectMapper = objectMapper;

        this.arquivoContas = dataPathConfig
                .getPastaDados()
                .resolve("contas.jsonl")
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(arquivoContas.getParent());

            if (Files.notExists(arquivoContas)) {
                Files.createFile(arquivoContas);
            }

            carregarContas();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Não foi possível inicializar o repositório de contas.",
                    e);
        }

        System.out.println(
                "[ICEIBank] Repositório de contas: " + arquivoContas);
    }

    private synchronized void carregarContas() throws IOException {

        for (String linha : Files.readAllLines(
                arquivoContas, StandardCharsets.UTF_8)) {

            if (linha == null || linha.isBlank()) {
                continue;
            }

            ContaPersistida r =
                    objectMapper.readValue(linha, ContaPersistida.class);

            Conta c = new Conta(
                    r.id(),
                    r.nomeUsuario(),
                    r.saldo()
            );

            contas.put(c.getId(), c);
            versoesContas.put(
                    c.getId(),
                    r.versao() == null ? 0L : r.versao()
            );

            idsProcessados.put(
                    c.getId(),
                    new HashSet<>(
                            r.operacoesProcessadas() == null
                                    ? List.of()
                                    : r.operacoesProcessadas()
                    )
            );

            if (r.ultimoVetorOperacao() != null) {
                ultimoVetorOperacao.put(
                        c.getId(),
                        r.ultimoVetorOperacao().clone()
                );
            }
        }
    }

    @Override
    public synchronized boolean salvarSeAusente(Conta conta) {

        if (contas.containsKey(conta.getId())) {
            return false;
        }

        contas.put(conta.getId(), conta);

        try {
            persistirTudo();
            return true;
        } catch (RuntimeException e) {
            contas.remove(conta.getId());
            throw e;
        }
    }

    @Override
    public Optional<Conta> buscarPorId(long id) {
        return Optional.ofNullable(contas.get(id));
    }

    @Override
    public int quantidade() {
        return contas.size();
    }

    @Override
    public synchronized void atualizar(Conta conta) {
        long id = conta.getId();

        contas.put(id, conta);

        versoesContas.put(
                id,
                versoesContas.getOrDefault(id, 0L) + 1
        );

        persistirTudo();
    }

    @Override
    public synchronized boolean creditarTransferencia(
            long idConta,
            BigDecimal valor,
            String transferenciaId) {

        return movimentarTransferencia(
                idConta, valor, transferenciaId, "CREDITO");
    }

    @Override
    public synchronized boolean estornarTransferencia(
            long idConta,
            BigDecimal valor,
            String transferenciaId) {

        return movimentarTransferencia(
                idConta, valor, transferenciaId, "ESTORNO");
    }

    private synchronized boolean movimentarTransferencia(
            long idConta,
            BigDecimal valor,
            String transferenciaId,
            String tipo) {

        if (transferenciaId == null || transferenciaId.isBlank()
                || transferenciaId.length() > 128) {
            throw new IllegalArgumentException(
                    "Identificador de transferência inválido.");
        }

        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Valor de transferência inválido.");
        }

        Conta conta = contas.get(idConta);

        if (conta == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "Conta não encontrada nesta agência.");
        }

        String chave = tipo + ":" + transferenciaId;

        Set<String> processados = idsProcessados.computeIfAbsent(
                idConta, k -> new HashSet<>());

        if (processados.contains(chave)) {
            return false;
        }

        BigDecimal saldoAnterior = conta.getSaldo();
        long versaoAnterior = versoesContas.getOrDefault(idConta, 0L);

        conta.depositar(valor);
        processados.add(chave);
        versoesContas.put(idConta, versaoAnterior + 1);

        try {
            persistirTudo();
        } catch (RuntimeException e) {
            contas.put(idConta, new Conta(
                    idConta, conta.getNomeUsuario(), saldoAnterior));
            processados.remove(chave);
            versoesContas.put(idConta, versaoAnterior);
            throw e;
        }

        return true;
    }

    public synchronized long consultarVersao(long id) {
        if (!contas.containsKey(id)) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "Conta não encontrada nesta agência."
            );
        }

        return versoesContas.getOrDefault(id, 0L);
    }

    /**
     * Retorna true se aplicou a operação.
     * Retorna false se já havia sido aplicada.
     * <p>
     * Saldo e identificador são persistidos
     * no mesmo snapshot JSONL.
     */
    public synchronized boolean aplicarOperacaoOffline(

            long id,
            String operacaoId,
            String tipo,
            BigDecimal valor,
            int[] vetorConhecido,
            int[] vetorNovo,
            Long versaoConhecida) {

        Conta conta = contas.get(id);

        if (conta == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "Conta não encontrada nesta agência.");
        }

        if (operacaoId == null ||
                operacaoId.isBlank() ||
                operacaoId.length() > 128) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "operacaoId inválido.");
        }

        if (valor == null || valor.signum() <= 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "O valor deve ser maior que zero.");
        }

        if (!"DEPOSITO".equals(tipo) &&
                !"SAQUE".equals(tipo)) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Tipo de operação inválido.");
        }

        if (vetorConhecido == null ||
                vetorConhecido.length != 3 ||
                vetorNovo == null ||
                vetorNovo.length != 3) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Vetor inválido.");
        }

        for (int n : vetorConhecido) {
            if (n < 0) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Vetor inválido.");
            }
        }

        Set<String> ids = idsProcessados.computeIfAbsent(
                id,
                k -> new HashSet<>()
        );

        if (ids.contains(operacaoId)) {
            return false;
        }

        int[] anterior = ultimoVetorOperacao.get(id);
        if (anterior != null) {
            int comparacao = RelogioVetorial.comparar(
                    vetorConhecido,
                    anterior
            );

            if (comparacao == 2) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "Conflito 409: operações concorrentes detectadas pelo relógio vetorial."
                );
            }
        }
        long versaoAtual = versoesContas.getOrDefault(id, 0L);

        if (versaoConhecida == null) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Conflito 409: operação sem versão conhecida. "
                            + "É necessária uma reconciliação antes da sincronização.");
        }

        if (versaoConhecida < 0 || versaoConhecida > versaoAtual) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Versão conhecida inválida.");
        }

        if (versaoConhecida != versaoAtual) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Conflito 409: a conta foi alterada após a versão conhecida.");
        }

        if ("SAQUE".equals(tipo) &&
                conta.getSaldo().compareTo(valor) < 0) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Saldo insuficiente.");
        }

        BigDecimal saldoAnterior = conta.getSaldo();

        int[] vetorAnterior =
                anterior == null ? null : anterior.clone();

        if ("DEPOSITO".equals(tipo)) {
            conta.depositar(valor);
        } else {
            conta.sacar(valor);
        }

        ids.add(operacaoId);

        ultimoVetorOperacao.put(
                id,
                vetorNovo.clone()
        );

// Incrementa a versão após uma operação offline
        long versaoAnterior = versoesContas.getOrDefault(id, 0L);
        versoesContas.put(id, versaoAnterior + 1);

        try {
            persistirTudo();

        } catch (RuntimeException e) {
            versoesContas.put(id, versaoAnterior);
            contas.put(
                    id,
                    new Conta(
                            id,
                            conta.getNomeUsuario(),
                            saldoAnterior
                    )
            );

            ids.remove(operacaoId);

            if (vetorAnterior == null) {
                ultimoVetorOperacao.remove(id);
            } else {
                ultimoVetorOperacao.put(id, vetorAnterior);
            }

            throw e;
        }

        return true;
    }

    private void persistirTudo() {

        try {
            StringBuilder conteudo = new StringBuilder();

            for (Conta conta : contas.values()) {

                ContaPersistida r = new ContaPersistida(
                        conta.getId(),
                        conta.getNomeUsuario(),
                        conta.getSaldo(),
                        new ArrayList<>(
                                idsProcessados.getOrDefault(
                                        conta.getId(),
                                        Set.of()
                                )
                        ),
                        ultimoVetorOperacao.get(conta.getId()),
                        versoesContas.getOrDefault(conta.getId(), 0L)
                );

                conteudo
                        .append(objectMapper.writeValueAsString(r))
                        .append(System.lineSeparator());
            }

            Path tmp = Files.createTempFile(
                    arquivoContas.getParent(),
                    "contas-",
                    ".tmp"
            );

            try {
                Files.writeString(
                        tmp,
                        conteudo.toString(),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE
                );

                try {
                    Files.move(
                            tmp,
                            arquivoContas,
                            StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING
                    );

                } catch (AtomicMoveNotSupportedException ex) {
                    throw new IllegalStateException(
                            "Sistema de arquivos não suporta substituição atômica do arquivo de contas.",
                            ex);
                }

            } finally {
                Files.deleteIfExists(tmp);
            }

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Falha ao persistir contas.", e);
        }
    }

    private record ContaPersistida(
            long id,
            String nomeUsuario,
            BigDecimal saldo,
            List<String> operacoesProcessadas,
            int[] ultimoVetorOperacao,
            Long versao
    ) {
        public ContaPersistida {
            if (operacoesProcessadas == null) {
                operacoesProcessadas = List.of();
            }
        }
    }
}
