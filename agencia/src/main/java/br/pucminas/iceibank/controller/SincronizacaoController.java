package br.pucminas.iceibank.controller;
import br.pucminas.iceibank.exception.ApiException;
import br.pucminas.iceibank.model.Conta;
import br.pucminas.iceibank.service.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;
//@RestController @RequestMapping("/contas")
public class SincronizacaoController {
 private final ContaService contas;private final RelogioVetorial relogio;private final RegistroEventosAplicacao registro;
 public SincronizacaoController(ContaService c,RelogioVetorial r,RegistroEventosAplicacao log){contas=c;relogio=r;registro=log;}
 public record Pendente(String tipo,BigDecimal valor,int[] vetorConhecido,String operacaoId){}
 @PostMapping("/{id}/sincronizar") public synchronized ResponseEntity<?> sincronizar(@PathVariable long id,@RequestBody Pendente p){
  Conta conta=contas.consultar(id);int[] atual=relogio.valorAtual();
  if(!"IGUAIS".equals(ComparadorVetorial.comparar(p.vetorConhecido(),atual)))return ResponseEntity.status(409).body(Map.of("erro","Conflito detectado: estado alterado desde a ultima leitura.","vetorConhecido",p.vetorConhecido(),"vetorAtual",atual,"contaAtual",conta));
  if(p.valor()==null||p.valor().signum()<=0)throw new ApiException(HttpStatus.BAD_REQUEST,"Valor deve ser positivo");
  Conta resultado;
  if("DEPOSITO".equals(p.tipo()))resultado=contas.depositar(id,p.valor());
  else if("SAQUE".equals(p.tipo()))resultado=contas.sacar(id,p.valor());
  else throw new ApiException(HttpStatus.BAD_REQUEST,"Tipo de operacao nao suportado");
  registro.registrar("SINCRONIZACAO_OFFLINE",relogio.eventoLocal(),Map.of("id",id,"tipo",p.tipo(),"valor",p.valor(),"operacaoId",p.operacaoId()==null?"nao-informado":p.operacaoId()));
  return ResponseEntity.ok(Map.of("conta",resultado,"vetorAtual",relogio.valorAtual()));
 }
}
