package br.pucminas.iceibank.service;

import br.pucminas.iceibank.config.AgenciaConfig;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Adaptador Spring para o RegistroEventos fornecido no roteiro.
 * Mantém RegistroEventos.java sem alterações e converte IOException em erro de aplicação.
 */
@Service
public class RegistroEventosAplicacao {
    private final RegistroEventos registroEventos;
    private final Path pastaDados;
    private final Path caminhoArquivo;

    public RegistroEventosAplicacao(RegistroEventos registroEventos, AgenciaConfig config) {
        this.registroEventos = registroEventos;
        this.pastaDados = Paths.get("data").toAbsolutePath().normalize();
        this.caminhoArquivo = pastaDados.resolve("eventos-agencia-" + config.getIdAgencia() + ".jsonl");
    }

    public Map<String, Object> registrar(String tipo, int timestampLamport, Map<String, Object> detalhes) {
        try {
            return registroEventos.registrar(tipo, timestampLamport, detalhes);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao registrar evento em " + caminhoArquivo, e);
        }
    }

    public Path getPastaDados() {
        return pastaDados;
    }

    public Path getCaminhoArquivo() {
        return caminhoArquivo;
    }
}
