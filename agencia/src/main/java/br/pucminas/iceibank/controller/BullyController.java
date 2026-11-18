package br.pucminas.iceibank.controller;
import br.pucminas.iceibank.config.AgenciaConfig;
import br.pucminas.iceibank.service.BullyService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/bully")
public class BullyController {
 private final BullyService bully;private final AgenciaConfig config;
 public BullyController(BullyService b,AgenciaConfig c){bully=b;config=c;}
 public record Eleicao(int origemId,int[] vetorEnvio){}
 public record Coordenador(int liderId,int[] vetorEnvio){}
 @PostMapping("/eleicao") public void eleicao(@RequestBody Eleicao e){bully.receberEleicao(e.origemId(),e.vetorEnvio());}
 @PostMapping("/coordenador") public void coordenador(@RequestBody Coordenador c){bully.receberCoordenador(c.liderId(),c.vetorEnvio());}
 @GetMapping("/lider") public Map<String,Object> lider(){return Map.of("idAgencia",config.getIdAgencia(),"liderAtual",bully.obterLiderAtual()<0?"":bully.obterLiderAtual());}
}
