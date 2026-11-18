package br.pucminas.iceibank.service;

import br.pucminas.iceibank.config.AgenciaConfig;
import br.pucminas.iceibank.dto.TransferenciaMensagem;
import br.pucminas.iceibank.exception.ApiException;
import br.pucminas.iceibank.model.Conta;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import br.pucminas.iceibank.model.TransferenciaRegistro;
import br.pucminas.iceibank.repository.TransferenciaRepository;
@Service
public class TransferenciaService {

 private final ContaService contas;
 private final AgenciaConfig config;
 private final RelogioVetorial relogio;
 private final RegistroEventosAplicacao registro;
 private final MensageriaService mensageria;
 private final TransferenciaRepository transferencias;


 public TransferenciaService(
         ContaService contas,
         AgenciaConfig config,
         RelogioVetorial relogio,
         RegistroEventosAplicacao registro,
         MensageriaService mensageria,
         TransferenciaRepository transferencias) {

  this.contas = contas;
  this.config = config;
  this.relogio = relogio;
  this.registro = registro;
  this.mensageria = mensageria;
  this.transferencias = transferencias;
 }


 public Map<String, Object> transferir(
         long idOrigem,
         long idDestino,
         BigDecimal valor) {

  contas.validarValorPositivo(valor);

  Conta origem = contas.obterOu404(idOrigem);

  if (config.agenciaResponsavel(idOrigem)
          != config.getIdAgencia()) {

   throw new ApiException(
           HttpStatus.BAD_REQUEST,
           "Conta de origem não pertence a esta agência.");
  }

  /*
   * Débito da conta de origem.
   */
  synchronized (origem) {

   if (origem.getSaldo().compareTo(valor) < 0) {
    throw new ApiException(
            HttpStatus.BAD_REQUEST,
            "Saldo insuficiente.");
   }

   origem.sacar(valor);

   // Persiste o débito
   contas.atualizarConta(origem);
  }

  int[] vd = relogio.eventoLocal();

  registro.registrar(
          "TRANSFERENCIA_DEBITO",
          vd,
          Map.of(
                  "idOrigem", idOrigem,
                  "idDestino", idDestino,
                  "valor", valor));

  int destino =
          config.agenciaResponsavel(idDestino);

  /*
   * Transferência dentro da mesma agência.
   */
  if (destino == config.getIdAgencia()) {

   try {

    Conta contaDestino =
            contas.obterOu404(idDestino);

    contaDestino.depositar(valor);

    // Persiste o crédito
    contas.atualizarConta(contaDestino);

    int[] vc = relogio.eventoLocal();

    registro.registrar(
            "TRANSFERENCIA_CREDITO",
            vc,
            Map.of(
                    "idOrigem", idOrigem,
                    "idDestino", idDestino,
                    "valor", valor));

    return Map.of(
            "mensagem",
            "Transferência concluída (mesma agência).");

   } catch (ApiException e) {

    /*
     * Se a conta de destino não existir,
     * devolvemos o dinheiro à origem.
     */
    origem.depositar(valor);

    // Persiste o estorno
    contas.atualizarConta(origem);

    throw e;
   }
  }

  /*
   * Transferência entre agências.
   */
  int[] ve = relogio.aoEnviar();
  String transferenciaId = UUID.randomUUID().toString();
  transferencias.salvar(
          new TransferenciaRegistro(
                  transferenciaId,
                  idOrigem,
                  idDestino,
                  valor,
                  "PENDENTE"
          )
  );
  mensageria.publicarCredito(
          destino,
          new TransferenciaMensagem(
                  idDestino,
                  valor,
                  ve,
                  config.getIdAgencia(),
                  idOrigem,
                  transferenciaId)
  );
  return Map.of(
          "mensagem",
          "Transferência publicada para a agência de destino (entrega assíncrona).",
          "timestampVetorial",
          ve);
 }
}