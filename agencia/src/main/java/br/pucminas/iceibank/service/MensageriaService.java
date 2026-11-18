package br.pucminas.iceibank.service;

import br.pucminas.iceibank.config.RabbitMqConfig;
import br.pucminas.iceibank.dto.TransferenciaMensagem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Map;
import br.pucminas.iceibank.dto.ResultadoTransferenciaMensagem;
import br.pucminas.iceibank.model.TransferenciaRegistro;
import br.pucminas.iceibank.repository.TransferenciaRepository;
import br.pucminas.iceibank.exception.ApiException;
import org.springframework.http.HttpStatus;
@Service
public class MensageriaService {

 private final RabbitTemplate rabbit;
 private final ObjectMapper mapper;
 private final ContaService contas;
 private final RelogioVetorial relogio;
 private final RegistroEventosAplicacao registro;
 private final TransferenciaRepository transferencias;

 public MensageriaService(
         RabbitTemplate rabbit,
         ObjectMapper mapper,
         ContaService contas,
         RelogioVetorial relogio,
         RegistroEventosAplicacao registro,
         TransferenciaRepository transferencias) {

  this.rabbit = rabbit;
  this.mapper = mapper;
  this.contas = contas;
  this.relogio = relogio;
  this.registro = registro;
  this.transferencias = transferencias;
 }


 public void publicarCredito(
         int agenciaDestino,
         TransferenciaMensagem mensagem) {

  rabbit.convertAndSend(
          RabbitMqConfig.EXCHANGE,
          "agencia." + agenciaDestino + ".creditar",
          mensagem,
          msg -> {
           msg.getMessageProperties()
                   .setDeliveryMode(MessageDeliveryMode.PERSISTENT);
           return msg;
          });
 }

 public void publicarResultado(
         int agenciaOrigem,
         ResultadoTransferenciaMensagem resultado) {

  rabbit.convertAndSend(
          RabbitMqConfig.EXCHANGE,
          "agencia." + agenciaOrigem + ".resultado-transferencia",
          resultado,
          msg -> {
           msg.getMessageProperties()
                   .setDeliveryMode(MessageDeliveryMode.PERSISTENT);
           return msg;
          });
 }

 @RabbitListener(queues = "#{filaResultadoTransferencia.name}")
 public void receberResultado(
         ResultadoTransferenciaMensagem resultado) {

  if (resultado.sucesso()) {


   TransferenciaRegistro atual = transferencias
           .buscar(resultado.transferenciaId())
           .orElseThrow(() -> new IllegalStateException(
                   "Transferência não encontrada: "
                           + resultado.transferenciaId()));

   if (atual.idOrigem() != resultado.idOrigem()
           || atual.idDestino() != resultado.idDestino()) {
    throw new IllegalStateException(
            "Resultado incompatível com a transferência.");
   }

   if ("CONCLUIDA".equals(atual.estado())) {
    return;
   }

   if (!"PENDENTE".equals(atual.estado())) {
    throw new IllegalStateException(
            "Transferência em estado incompatível: "
                    + atual.estado());
   }

   transferencias.salvar(
           new TransferenciaRegistro(
                   atual.transferenciaId(),
                   atual.idOrigem(),
                   atual.idDestino(),
                   atual.valor(),
                   "CONCLUIDA"
           )
   );


   registro.registrar(
           "TRANSFERENCIA_CONFIRMADA",
           relogio.eventoLocal(),
           Map.of(
                   "transferenciaId", resultado.transferenciaId(),
                   "idOrigem", resultado.idOrigem(),
                   "idDestino", resultado.idDestino()
           )
   );

   return;
  }

  TransferenciaRegistro atual = transferencias
          .buscar(resultado.transferenciaId())
          .orElseThrow(() -> new IllegalStateException(
                  "Transferência não encontrada: "
                          + resultado.transferenciaId()));

  if (atual.idOrigem() != resultado.idOrigem()
          || atual.idDestino() != resultado.idDestino()) {
   throw new IllegalStateException(
           "Resultado incompatível com a transferência.");
  }

  if ("ESTORNADA".equals(atual.estado())) {
   return;
  }

  if (!"PENDENTE".equals(atual.estado())) {
   throw new IllegalStateException(
           "Não é possível estornar transferência no estado: "
                   + atual.estado());
  }

  contas.estornarTransferencia(
          atual.idOrigem(),
          atual.valor(),
          atual.transferenciaId()
  );

  transferencias.salvar(
          new TransferenciaRegistro(
                  atual.transferenciaId(),
                  atual.idOrigem(),
                  atual.idDestino(),
                  atual.valor(),
                  "ESTORNADA"
          )
  );

  registro.registrar(
          "TRANSFERENCIA_FALHA_RECEBIDA",
          relogio.eventoLocal(),
          Map.of(
                  "transferenciaId", resultado.transferenciaId(),
                  "idOrigem", resultado.idOrigem(),
                  "motivo", resultado.motivo() == null
                          ? "Não informado"
                          : resultado.motivo()
          )
  );
 }


 @RabbitListener(queues = "#{filaAgencia.name}")
 public void receberCredito(TransferenciaMensagem mensagem) {

  int[] vetor =
          relogio.aoReceber(mensagem.vetorEnvio());

  try {


   boolean creditoAplicado = contas.creditarTransferencia(
           mensagem.idConta(),
           mensagem.valor(),
           mensagem.transferenciaId()
   );

   publicarResultado(
           mensagem.origemAgencia(),
           new ResultadoTransferenciaMensagem(
                   mensagem.transferenciaId(),
                   mensagem.idOrigem(),
                   mensagem.idConta(),
                   mensagem.origemAgencia(),
                   true,
                   creditoAplicado
                           ? "Crédito realizado com sucesso."
                           : "Crédito já processado anteriormente."
           )
   );


   registro.registrar(
           "TRANSFERENCIA_CREDITO_REMOTO",
           vetor,
           Map.of(
                   "idConta", mensagem.idConta(),
                   "idOrigem", mensagem.idOrigem(),
                   "valor", mensagem.valor(),
                   "origemAgencia", mensagem.origemAgencia()));


  } catch (ApiException e) {

   if (e.getStatus() != HttpStatus.NOT_FOUND) {
    throw e;
   }

   registro.registrar(
           "CREDITO_REMOTO_FALHOU",
           vetor,
           Map.of(
                   "idConta", mensagem.idConta(),
                   "valor", mensagem.valor(),
                   "origemAgencia", mensagem.origemAgencia(),
                   "motivo", "conta nao encontrada"
           )
   );

   publicarResultado(
           mensagem.origemAgencia(),
           new ResultadoTransferenciaMensagem(
                   mensagem.transferenciaId(),
                   mensagem.idOrigem(),
                   mensagem.idConta(),
                   mensagem.origemAgencia(),
                   false,
                   "Conta de destino não encontrada."
           )
   );
  }
 }

  @RabbitListener(queues = "fila-auditoria")
  public void auditar (Message mensagem){

   try {

    byte[] corpo = mensagem.getBody();

    Path arquivo =
            Path.of("data", "auditoria-mensageria.jsonl");

    Files.createDirectories(arquivo.getParent());

    String linha =
            mapper.writeValueAsString(
                    Map.of(
                            "horaParede",
                            Instant.now().toString(),
                            "mensagem",
                            mapper.readTree(corpo)))
                    + System.lineSeparator();

    Files.writeString(
            arquivo,
            linha,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND);

   } catch (Exception e) {

    System.err.println(
            "Falha na auditoria: " + e.getMessage());
   }
  }
 }
