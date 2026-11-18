
package br.pucminas.iceibank.controller;

import br.pucminas.iceibank.dto.CriarContaRequest;
import br.pucminas.iceibank.dto.SincronizarRequest;
import br.pucminas.iceibank.dto.ValorRequest;
import br.pucminas.iceibank.model.Conta;
import br.pucminas.iceibank.model.Evento;
import br.pucminas.iceibank.service.ContaService;
import br.pucminas.iceibank.service.HistoricoTransacoesService;
import br.pucminas.iceibank.service.RelogioVetorial;
import br.pucminas.iceibank.service.SincronizacaoOfflineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import br.pucminas.iceibank.repository.ContaRepositoryEmMemoria;

@RestController
@RequestMapping("/contas")
public class ContaController {

    private final ContaService contaService;
    private final RelogioVetorial relogio;
    private final HistoricoTransacoesService historicoTransacoesService;
    private final SincronizacaoOfflineService sincronizacaoOfflineService;
    @Autowired
    private ContaRepositoryEmMemoria contaRepository;
    public ContaController(
            ContaService contaService,
            HistoricoTransacoesService historicoTransacoesService,
            RelogioVetorial relogio,
            SincronizacaoOfflineService sincronizacaoOfflineService) {

        this.relogio = relogio;
        this.contaService = contaService;
        this.historicoTransacoesService = historicoTransacoesService;
        this.sincronizacaoOfflineService = sincronizacaoOfflineService;
    }

    @PostMapping
    public ResponseEntity<Conta> criar(
            @RequestBody CriarContaRequest request) {

        Conta conta = contaService.criar(
                request.id(),
                request.nomeUsuario(),
                request.saldoInicial()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(conta);
    }

        @GetMapping("/{id}")
    public Map<String, Object> consultar(@PathVariable long id) {

        Conta conta = contaService.consultar(id);

        return Map.of(
                "id", conta.getId(),
                "nomeUsuario", conta.getNomeUsuario(),
                "saldo", conta.getSaldo(),
                "vetorAtual", relogio.valorAtual(),
                "versao", contaRepository.consultarVersao(id)
        );
    }


    @PostMapping("/{id}/depositar")
    public Conta depositar(
            @PathVariable long id,
            @RequestBody ValorRequest request) {

        return contaService.depositar(id, request.valor());
    }

    @PostMapping("/{id}/sacar")
    public Conta sacar(
            @PathVariable long id,
            @RequestBody ValorRequest request) {

        return contaService.sacar(id, request.valor());
    }

    @PostMapping("/{id}/sincronizar")
    public Map<String, Object> sincronizar(
            @PathVariable long id,
            @RequestBody SincronizarRequest request) {

        return sincronizacaoOfflineService.sincronizar(id, request);
    }

    @GetMapping("/{id}/historico")
    public List<Evento> historico(@PathVariable long id) {

        contaService.consultar(id);

        return historicoTransacoesService.historicoPorConta(id);
    }
}
