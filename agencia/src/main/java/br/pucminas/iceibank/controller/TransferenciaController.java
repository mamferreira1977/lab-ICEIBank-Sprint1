package br.pucminas.iceibank.controller;

import br.pucminas.iceibank.dto.CreditoRemotoRequest;
import br.pucminas.iceibank.dto.TransferenciaRequest;
import br.pucminas.iceibank.service.TransferenciaService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class TransferenciaController {
    private final TransferenciaService transferenciaService;

    public TransferenciaController(TransferenciaService transferenciaService) {
        this.transferenciaService = transferenciaService;
    }

    @PostMapping("/transferencias")
    public Map<String, Object> transferir(@RequestBody TransferenciaRequest request) {
        return transferenciaService.transferir(request.idOrigem(), request.idDestino(), request.valor());
    }

    @PostMapping("/contas/{id}/creditar-remoto")
    public Map<String, Object> creditarRemoto(@PathVariable long id,
                                               @RequestBody CreditoRemotoRequest request) {
        return transferenciaService.creditarRemoto(id, request);
    }
}
