package br.pucminas.iceibank.service;

import br.pucminas.iceibank.config.AgenciaConfig;
import br.pucminas.iceibank.dto.CreditoRemotoRequest;
import br.pucminas.iceibank.exception.ApiException;
import br.pucminas.iceibank.model.Conta;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Service
public class TransferenciaService {
    private final ContaService contaService;
    private final AgenciaConfig config;
    private final RelogioLamport relogio;
    private final RegistroEventosAplicacao registro;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String internalKey;

    public TransferenciaService(ContaService contaService,
                                AgenciaConfig config,
                                RelogioLamport relogio,
                                RegistroEventosAplicacao registro,
                                ObjectMapper objectMapper,
                                @Value("${iceibank.internal-key}") String internalKey) {
        this.contaService = contaService;
        this.config = config;
        this.relogio = relogio;
        this.registro = registro;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        this.internalKey = internalKey;
    }

    public Map<String, Object> transferir(long idOrigem, long idDestino, BigDecimal valor) {
        contaService.validarValorPositivo(valor);
        Conta contaOrigem = contaService.obterOu404(idOrigem);
        if (config.agenciaResponsavel(idOrigem) != config.getIdAgencia()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Conta de origem não pertence a esta agência.");
        }

        synchronized (contaOrigem) {
            if (contaOrigem.getSaldo().compareTo(valor) < 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Saldo insuficiente.");
            }
            contaOrigem.sacar(valor);
        }

        int tsDebito = relogio.eventoLocal();
        registro.registrar("TRANSFERENCIA_DEBITO", tsDebito, Map.of(
                "idOrigem", idOrigem, "idDestino", idDestino, "valor", valor
        ));

        int agenciaDestino = config.agenciaResponsavel(idDestino);
        if (agenciaDestino == config.getIdAgencia()) {
            try {
                Conta contaDestino = contaService.obterOu404(idDestino);
                contaDestino.depositar(valor);
                int tsCredito = relogio.eventoLocal();
                registro.registrar("TRANSFERENCIA_CREDITO", tsCredito, Map.of(
                        "idOrigem", idOrigem, "idDestino", idDestino, "valor", valor
                ));
                return Map.of("mensagem", "Transferência concluída (mesma agência).");
            } catch (ApiException ex) {
                contaOrigem.depositar(valor);
                throw ex;
            }
        }

        int tsEnvio = relogio.aoEnviar();
        String url = config.urlAgencia(agenciaDestino) + "/contas/" + idDestino + "/creditar-remoto";
        CreditoRemotoRequest corpo = new CreditoRemotoRequest(valor, tsEnvio, config.getIdAgencia());

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Key", internalKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(corpo)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Agência destino respondeu HTTP " + response.statusCode() + ": " + response.body());
            }
            return Map.of("mensagem", "Transferência concluída (entre agências).");
        } catch (Exception ex) {
            int tsFalha = relogio.eventoLocal();
            registro.registrar("TRANSFERENCIA_FALHOU", tsFalha, Map.of(
                    "idOrigem", idOrigem,
                    "idDestino", idDestino,
                    "valor", valor,
                    "erro", ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()
            ));
            // Limitação conhecida do Sprint 1: o débito NÃO é revertido.
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Falha ao contatar agência de destino. Débito já aplicado - inconsistência conhecida (ver Sprint 4).");
        }
    }

    public Map<String, Object> creditarRemoto(long idConta, CreditoRemotoRequest req) {
        int ts = relogio.aoReceber(req.timestampLamport());
        Conta conta = contaService.obterOu404(idConta);
        conta.depositar(req.valor());
        registro.registrar("TRANSFERENCIA_CREDITO_REMOTO", ts, Map.of(
                "idConta", idConta,
                "valor", req.valor(),
                "origemAgencia", req.origemAgencia()
        ));
        return Map.of(
                "mensagem", "Crédito remoto aplicado.",
                "saldoAtual", conta.getSaldo()
        );
    }
}
