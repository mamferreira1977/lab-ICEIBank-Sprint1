package br.pucminas.iceibank.controller;

import br.pucminas.iceibank.config.AgenciaConfig;
import br.pucminas.iceibank.repository.UsuarioRepository;
import br.pucminas.iceibank.service.ContaService;
import br.pucminas.iceibank.service.RegistroEventosAplicacao;
import br.pucminas.iceibank.service.RelogioLamportMonitorado;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class StatusController {
    private final AgenciaConfig config;
    private final RelogioLamportMonitorado relogio;
    private final ContaService contaService;
    private final RegistroEventosAplicacao registroEventos;
    private final UsuarioRepository usuarioRepository;

    public StatusController(AgenciaConfig config, RelogioLamportMonitorado relogio, ContaService contaService,
                            RegistroEventosAplicacao registroEventos, UsuarioRepository usuarioRepository) {
        this.config = config;
        this.relogio = relogio;
        this.contaService = contaService;
        this.registroEventos = registroEventos;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("agencia", config.getIdAgencia());
        resposta.put("relogioLamport", relogio.valorAtual());
        resposta.put("quantidadeContas", contaService.quantidadeContas());
        resposta.put("pastaDados", registroEventos.getPastaDados().toString());
        resposta.put("arquivoEventos", registroEventos.getCaminhoArquivo().toString());
        resposta.put("arquivoUsuarios", usuarioRepository.getArquivoUsuarios().toString());
        return resposta;
    }
}
