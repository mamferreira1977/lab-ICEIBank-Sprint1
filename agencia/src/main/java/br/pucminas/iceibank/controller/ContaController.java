package br.pucminas.iceibank.controller;

import br.pucminas.iceibank.dto.CriarContaRequest;
import br.pucminas.iceibank.dto.ValorRequest;
import br.pucminas.iceibank.model.Conta;
import br.pucminas.iceibank.model.Evento;
import br.pucminas.iceibank.service.ContaService;
import br.pucminas.iceibank.service.HistoricoTransacoesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/contas")
public class ContaController {
    private final ContaService contaService;
    private final HistoricoTransacoesService historicoTransacoesService;

    public ContaController(ContaService contaService, HistoricoTransacoesService historicoTransacoesService) {
        this.contaService = contaService;
        this.historicoTransacoesService = historicoTransacoesService;
    }

    @PostMapping
    public ResponseEntity<Conta> criar(@RequestBody CriarContaRequest request) {
        Conta conta = contaService.criar(request.id(), request.nomeUsuario(), request.saldoInicial());
        return ResponseEntity.status(HttpStatus.CREATED).body(conta);
    }

    @GetMapping("/{id}")
    public Conta consultar(@PathVariable long id) {
        return contaService.consultar(id);
    }

    @PostMapping("/{id}/depositar")
    public Conta depositar(@PathVariable long id, @RequestBody ValorRequest request) {
        return contaService.depositar(id, request.valor());
    }

    @PostMapping("/{id}/sacar")
    public Conta sacar(@PathVariable long id, @RequestBody ValorRequest request) {
        return contaService.sacar(id, request.valor());
    }

    @GetMapping("/{id}/historico")
    public java.util.List<Evento> historico(@PathVariable long id) {
        contaService.consultar(id);
        return historicoTransacoesService.historicoPorConta(id);
    }
}
