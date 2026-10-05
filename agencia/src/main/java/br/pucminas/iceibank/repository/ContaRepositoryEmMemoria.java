package br.pucminas.iceibank.repository;

import br.pucminas.iceibank.config.DataPathConfig;
import br.pucminas.iceibank.model.Conta;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class ContaRepositoryEmMemoria implements ContaRepository {

    private final ConcurrentMap<Long, Conta> contas = new ConcurrentHashMap<>();
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
            if (Files.notExists(this.arquivoContas)) {
                Files.createFile(this.arquivoContas);
            }

            carregarContas();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Não foi possível inicializar o repositório de contas.", e);
        }

        System.out.println(
                "[ICEIBank] Repositório de contas: " + arquivoContas);
    }

    private synchronized void carregarContas() throws IOException {

        List<String> linhas =
                Files.readAllLines(arquivoContas, StandardCharsets.UTF_8);

        for (String linha : linhas) {

            if (linha == null || linha.isBlank()) {
                continue;
            }

            ContaPersistida registro =
                    objectMapper.readValue(linha, ContaPersistida.class);

            Conta conta = new Conta(
                    registro.id(),
                    registro.nomeUsuario(),
                    registro.saldo());

            contas.put(conta.getId(), conta);
        }
    }

    @Override
    public synchronized boolean salvarSeAusente(Conta conta) {

        if (contas.putIfAbsent(conta.getId(), conta) != null) {
            return false;
        }

        persistirTudo();

        return true;
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

        contas.put(conta.getId(), conta);

        persistirTudo();
    }

    private void persistirTudo() {

        try {

            StringBuilder conteudo = new StringBuilder();

            for (Conta conta : contas.values()) {

                ContaPersistida registro = new ContaPersistida(
                        conta.getId(),
                        conta.getNomeUsuario(),
                        conta.getSaldo());

                conteudo
                        .append(objectMapper.writeValueAsString(registro))
                        .append(System.lineSeparator());
            }

            Files.writeString(
                    arquivoContas,
                    conteudo.toString(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Falha ao persistir contas.", e);
        }
    }

    private record ContaPersistida(
            long id,
            String nomeUsuario,
            BigDecimal saldo) {
    }
}