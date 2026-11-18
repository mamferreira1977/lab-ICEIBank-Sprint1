package br.pucminas.iceibank.service;

import br.pucminas.iceibank.config.AgenciaConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class BullyService {
 private final AgenciaConfig config;
 private final RelogioVetorial relogio;
 private final RegistroEventosAplicacao registro;
 private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
 private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {Thread t=new Thread(r,"bully-election");t.setDaemon(true);return t;});
 private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {Thread t=new Thread(r,"bully-heartbeat");t.setDaemon(true);return t;});
 private final AtomicInteger lider = new AtomicInteger(-1);
 private final java.util.concurrent.atomic.AtomicBoolean emEleicao = new java.util.concurrent.atomic.AtomicBoolean(false);
 private final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
 @Value("${bully.heartbeat-ms:3000}") private long intervalo;
 @Value("${bully.timeout-ms:1000}") private long timeout;
 public BullyService(AgenciaConfig c, RelogioVetorial r, RegistroEventosAplicacao log){config=c;relogio=r;registro=log;}
 @PostConstruct public void iniciar(){scheduler.scheduleWithFixedDelay(this::verificarLider, 1500, Math.max(500,intervalo), TimeUnit.MILLISECONDS);}
 @PreDestroy public void parar(){scheduler.shutdownNow();executor.shutdownNow();}
 public int obterLiderAtual(){return lider.get();}
 public void iniciarEleicao(){if(emEleicao.compareAndSet(false,true))executor.submit(this::executarEleicao);}
 private void executarEleicao(){
  try {
   registro.registrar("ELEICAO_INICIADA",relogio.eventoLocal(),Map.of("idAgencia",config.getIdAgencia()));
   boolean maiorRespondeu=false;
   for(int i=config.getIdAgencia()+1;i<AgenciaConfig.NUMERO_AGENCIAS;i++){
    try {int[] v=relogio.aoEnviar();post(i,"/bully/eleicao",Map.of("origemId",config.getIdAgencia(),"vetorEnvio",v));maiorRespondeu=true;}catch(Exception ignored){}
   }
   if(!maiorRespondeu){tornarLider();}
  }finally{emEleicao.set(false);}
 }
 private void tornarLider(){
  lider.set(config.getIdAgencia());
  registro.registrar("TORNOU_LIDER",relogio.eventoLocal(),Map.of("idAgencia",config.getIdAgencia()));
  for(int i=0;i<AgenciaConfig.NUMERO_AGENCIAS;i++){
   if(i==config.getIdAgencia())continue;
   try{post(i,"/bully/coordenador",Map.of("liderId",config.getIdAgencia(),"vetorEnvio",relogio.aoEnviar()));}catch (Exception e) {
    System.err.println(
            "[BULLY] Agência " + config.getIdAgencia()
                    + " não conseguiu anunciar liderança para Agência "
                    + i + ": " + e.getMessage()
    );
   }
  }
 }
 public void receberEleicao(int origem,int[] vetor){
  if(origem>=config.getIdAgencia())throw new IllegalArgumentException("Eleicao exige origem com ID menor");
  registro.registrar("ELEICAO_RECEBIDA",relogio.aoReceber(vetor),Map.of("de",origem,"minhaId",config.getIdAgencia()));
  iniciarEleicao();
 }
 public void receberCoordenador(int id,int[] vetor){
  if(id<0||id>=AgenciaConfig.NUMERO_AGENCIAS)throw new IllegalArgumentException("Lider invalida");
  relogio.aoReceber(vetor);lider.set(id);
  registro.registrar("LIDER_ANUNCIADO",relogio.valorAtual(),Map.of("liderId",id,"agencia",config.getIdAgencia()));
 }
 private void verificarLider(){
  try{
   int atual=lider.get();if(atual==-1){iniciarEleicao();return;}if(atual==config.getIdAgencia())return;
   HttpRequest req=HttpRequest.newBuilder(URI.create(config.urlAgencia(atual)+"/bully/lider")).timeout(Duration.ofMillis(timeout)).GET().build();
   HttpResponse<String> res=http.send(req,HttpResponse.BodyHandlers.ofString());if(res.statusCode()!=200)throw new IllegalStateException("Heartbeat falhou");
   int anunciado=mapper.readTree(res.body()).path("liderAtual").asInt(-1);
   if(anunciado!=atual)throw new IllegalStateException("Lider divergente");
  }catch(Exception e){int antigo=lider.getAndSet(-1);if(antigo!=-1)registro.registrar("LIDER_NAO_RESPONDEU",relogio.eventoLocal(),Map.of("liderAtual",antigo,"agencia",config.getIdAgencia()));iniciarEleicao();}
 }
 private void post(int id,String path,Object body)throws Exception{
  HttpRequest req=HttpRequest.newBuilder(URI.create(config.urlAgencia(id)+path)).timeout(Duration.ofMillis(timeout)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
  HttpResponse<Void> response=http.send(req,HttpResponse.BodyHandlers.discarding());if(response.statusCode()!=200)throw new IllegalStateException("Sem OK");
 }
}
