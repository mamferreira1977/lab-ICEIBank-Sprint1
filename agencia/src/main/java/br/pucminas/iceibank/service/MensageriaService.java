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

@Service
public class MensageriaService {

 private final RabbitTemplate rabbit;
 private final ObjectMapper mapper;
 private final ContaService contas;
 private final RelogioVetorial relogio;
 private final RegistroEventosAplicacao registro;

 public MensageriaService(
         RabbitTemplate rabbit,
         ObjectMapper mapper,
         ContaService contas,
         RelogioVetorial relogio,
         RegistroEventosAplicacao registro) {

  this.rabbit = rabbit;
  this.mapper = mapper;
  this.contas = contas;
  this.relogio = relogio;
  this.registro = registro;
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

 @RabbitListener(queues = "#{filaAgencia.name}")
 public void receberCredito(TransferenciaMensagem mensagem) {

  int[] vetor =
          relogio.aoReceber(mensagem.vetorEnvio());

  try {

   var conta =
           contas.obterOu404(mensagem.idConta());

   conta.depositar(mensagem.valor());

   // Persiste o saldo após o crédito recebido via RabbitMQ
   contas.atualizarConta(conta);

   registro.registrar(
           "TRANSFERENCIA_CREDITO_REMOTO",
           vetor,
           Map.of(
                   "idConta", mensagem.idConta(),
                   "idOrigem", mensagem.idOrigem(),
                   "valor", mensagem.valor(),
                   "origemAgencia", mensagem.origemAgencia()));

  } catch (Exception e) {

   registro.registrar(
           "CREDITO_REMOTO_FALHOU",
           vetor,
           Map.of(
                   "idConta", mensagem.idConta(),
                   "valor", mensagem.valor(),
                   "origemAgencia", mensagem.origemAgencia(),
                   "motivo", "conta nao encontrada"));
  }
 }

 @RabbitListener(queues = "fila-auditoria")
 public void auditar(Message mensagem) {

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