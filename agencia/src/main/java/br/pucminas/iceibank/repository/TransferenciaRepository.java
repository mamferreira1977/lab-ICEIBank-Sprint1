package br.pucminas.iceibank.repository;

import br.pucminas.iceibank.config.DataPathConfig;
import br.pucminas.iceibank.model.TransferenciaRegistro;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

@Repository
public class TransferenciaRepository {

    private final Path arquivo;
    private final ObjectMapper mapper;

    private final Map<String, TransferenciaRegistro> transferencias =
            new HashMap<>();

    public TransferenciaRepository(
            DataPathConfig dataPathConfig,
            ObjectMapper mapper) {

        this.mapper = mapper;
        this.arquivo = dataPathConfig.getPastaDados()
                .resolve("transferencias.jsonl");

        try {
            Files.createDirectories(arquivo.getParent());

            if (Files.exists(arquivo)) {
                for (String linha : Files.readAllLines(
                        arquivo, StandardCharsets.UTF_8)) {

                    if (!linha.isBlank()) {
                        TransferenciaRegistro registro =
                                mapper.readValue(
                                        linha,
                                        TransferenciaRegistro.class);

                        transferencias.put(
                                registro.transferenciaId(),
                                registro);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Erro ao carregar transferências.", e);
        }
    }

    public synchronized Optional<TransferenciaRegistro> buscar(
            String transferenciaId) {

        return Optional.ofNullable(
                transferencias.get(transferenciaId));
    }

    public synchronized void salvar(TransferenciaRegistro registro) {

        if (registro == null
                || registro.transferenciaId() == null
                || registro.transferenciaId().isBlank()) {
            throw new IllegalArgumentException(
                    "Registro de transferência inválido.");
        }

        TransferenciaRegistro anterior =
                transferencias.put(registro.transferenciaId(), registro);

        try {
            persistirTudo();
        } catch (RuntimeException e) {
            if (anterior == null) {
                transferencias.remove(registro.transferenciaId());
            } else {
                transferencias.put(registro.transferenciaId(), anterior);
            }
            throw e;
        }
    }

    private void persistirTudo() {
        Path temporario = null;

        try {
            temporario = Files.createTempFile(
                    arquivo.getParent(), "transferencias-", ".tmp");

            StringBuilder conteudo = new StringBuilder();

            for (TransferenciaRegistro registro : transferencias.values()) {
                conteudo.append(mapper.writeValueAsString(registro))
                        .append(System.lineSeparator());
            }

            Files.writeString(
                    temporario,
                    conteudo.toString(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);

            Files.move(
                    temporario,
                    arquivo,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Erro ao persistir transferências.", e);
        } finally {
            if (temporario != null) {
                try {
                    Files.deleteIfExists(temporario);
                } catch (IOException ignored) {
                    // Arquivo temporário poderá ser removido posteriormente.
                }
            }
        }
    }

}
