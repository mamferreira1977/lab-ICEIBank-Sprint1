
package br.pucminas.iceibank.service;

import br.pucminas.iceibank.config.AgenciaConfig;
import br.pucminas.iceibank.dto.SincronizarRequest;
import br.pucminas.iceibank.exception.ApiException;
import br.pucminas.iceibank.model.Conta;
import br.pucminas.iceibank.repository.ContaRepositoryEmMemoria;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class SincronizacaoOfflineService {
    private final ContaRepositoryEmMemoria repositorio;
    private final AgenciaConfig config;
    private final RelogioVetorial relogio;
    private final RegistroEventosAplicacao registro;

    public SincronizacaoOfflineService(
            ContaRepositoryEmMemoria repositorio,
            AgenciaConfig config,
            RelogioVetorial relogio,
            RegistroEventosAplicacao registro) {
        this.repositorio = repositorio;
        this.config = config;
        this.relogio = relogio;
        this.registro = registro;
    }

    public synchronized Map<String, Object> sincronizar(
            long id, SincronizarRequest req) {

        if (req == null)
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Requisição obrigatória.");

        if (id < 0 ||
                config.agenciaResponsavel(id) != config.getIdAgencia())
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Conta não pertence a esta agência.");

        int[] novo = relogio.valorAtual();
        novo[config.getIdAgencia()]++;

        boolean aplicada = repositorio.aplicarOperacaoOffline(
                id,
                req.operacaoId(),
                req.tipo(),
                req.valor(),
                req.vetorConhecido(),
                novo,
                req.versaoConhecida()
        );

        Conta conta = repositorio.buscarPorId(id).orElseThrow();

        if (aplicada) {
            int[] ts = relogio.eventoLocal();

            registro.registrar(
                    req.tipo(),
                    ts,
                    Map.of(
                            "id", id,
                            "valor", req.valor(),
                            "novoSaldo", conta.getSaldo(),
                            "operacaoId", req.operacaoId(),
                            "origem", "OFFLINE"
                    )
            );
        }

        return Map.of(
                "operacaoId", req.operacaoId(),
                "aplicada", aplicada,
                "saldo", conta.getSaldo(),
                "vetorAtual", relogio.valorAtual(),
                "versao", repositorio.consultarVersao(id)
        );
    }
}
