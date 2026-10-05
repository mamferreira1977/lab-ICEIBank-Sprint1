package br.pucminas.iceibank.service;

import br.pucminas.iceibank.config.AgenciaConfig;
import br.pucminas.iceibank.model.Evento;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * A funcionalidade adicional de histórico fica separada do RegistroEventos do roteiro,
 * para que RegistroEventos.java permaneça exatamente com a implementação fornecida.
 */
@Service
public class HistoricoTransacoesService {
    private final ObjectMapper objectMapper;
    private final Path arquivoEventos;

    public HistoricoTransacoesService(ObjectMapper objectMapper, AgenciaConfig config) {
        this.objectMapper = objectMapper;
        this.arquivoEventos = Paths.get("data")
                .resolve("eventos-agencia-" + config.getIdAgencia() + ".jsonl")
                .toAbsolutePath()
                .normalize();
    }

    public List<Evento> historicoPorConta(long idConta) {
        List<Evento> resultado = new ArrayList<>();
        if (Files.notExists(arquivoEventos)) {
            return resultado;
        }

        try {
            for (String linha : Files.readAllLines(arquivoEventos, StandardCharsets.UTF_8)) {
                if (linha == null || linha.isBlank()) continue;
                Evento evento = objectMapper.readValue(linha, Evento.class);
                if (evento.detalhes() != null && pertenceAConta(evento.detalhes(), idConta)) {
                    resultado.add(evento);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao consultar o histórico da conta " + idConta, e);
        }

        resultado.sort(Comparator.comparing(Evento::horaParede));
        return resultado;
    }

    private boolean pertenceAConta(Map<String, Object> detalhes, long idConta) {
        return igual(detalhes.get("id"), idConta)
                || igual(detalhes.get("idConta"), idConta)
                || igual(detalhes.get("idOrigem"), idConta)
                || igual(detalhes.get("idDestino"), idConta);
    }

    private boolean igual(Object valor, long esperado) {
        if (valor instanceof Number n) return n.longValue() == esperado;
        if (valor instanceof String s) {
            try { return Long.parseLong(s) == esperado; }
            catch (NumberFormatException ignored) { return false; }
        }
        return false;
    }
}
