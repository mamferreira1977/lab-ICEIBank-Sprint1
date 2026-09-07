package br.pucminas.iceibank.service;

import br.pucminas.iceibank.config.AgenciaConfig;
import br.pucminas.iceibank.exception.ApiException;
import br.pucminas.iceibank.model.Conta;
import br.pucminas.iceibank.repository.ContaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class ContaService {
    private final ContaRepository contaRepository;
    private final AgenciaConfig config;
    private final RelogioLamport relogio;
    private final RegistroEventosAplicacao registro;

    public ContaService(ContaRepository contaRepository, AgenciaConfig config,
                        RelogioLamport relogio, RegistroEventosAplicacao registro) {
        this.contaRepository = contaRepository;
        this.config = config;
        this.relogio = relogio;
        this.registro = registro;
    }

    public Conta criar(long id, String nomeUsuario, BigDecimal saldoInicial) {
        validarId(id);
        validarPertencimento(id);
        if (nomeUsuario == null || nomeUsuario.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "nomeUsuario é obrigatório.");
        }
        BigDecimal saldo = saldoInicial == null ? BigDecimal.ZERO : saldoInicial;
        if (saldo.signum() < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "saldoInicial não pode ser negativo.");
        }

        Conta nova = new Conta(id, nomeUsuario.trim(), saldo);
        if (!contaRepository.salvarSeAusente(nova)) {
            throw new ApiException(HttpStatus.CONFLICT, "Conta já existe.");
        }

        int ts = relogio.eventoLocal();
        registro.registrar("CRIAR_CONTA", ts, Map.of(
                "id", id, "nomeUsuario", nova.getNomeUsuario(), "saldoInicial", saldo));
        return nova;
    }

    public Conta consultar(long id) {
        validarPertencimento(id);
        return obterOu404(id);
    }

    public Conta depositar(long id, BigDecimal valor) {
        validarPertencimento(id);
        validarValorPositivo(valor);
        Conta conta = obterOu404(id);
        conta.depositar(valor);
        int ts = relogio.eventoLocal();
        registro.registrar("DEPOSITO", ts, Map.of("id", id, "valor", valor, "novoSaldo", conta.getSaldo()));
        return conta;
    }

    public Conta sacar(long id, BigDecimal valor) {
        validarPertencimento(id);
        validarValorPositivo(valor);
        Conta conta = obterOu404(id);
        synchronized (conta) {
            if (conta.getSaldo().compareTo(valor) < 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Saldo insuficiente.");
            }
            conta.sacar(valor);
        }
        int ts = relogio.eventoLocal();
        registro.registrar("SAQUE", ts, Map.of("id", id, "valor", valor, "novoSaldo", conta.getSaldo()));
        return conta;
    }

    public Conta obterOu404(long id) {
        return contaRepository.buscarPorId(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conta não encontrada nesta agência."));
    }

    public int quantidadeContas() {
        return contaRepository.quantidade();
    }

    public void validarValorPositivo(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "O valor deve ser maior que zero.");
        }
    }

    private void validarId(long id) {
        if (id < 0) throw new ApiException(HttpStatus.BAD_REQUEST, "O id da conta não pode ser negativo.");
    }

    private void validarPertencimento(long id) {
        validarId(id);
        if (config.agenciaResponsavel(id) != config.getIdAgencia()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Conta " + id + " não pertence a esta agência.");
        }
    }
}
