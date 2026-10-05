package br.pucminas.iceibank;
import org.springframework.boot.SpringApplication; import org.springframework.boot.autoconfigure.SpringBootApplication; import java.util.*;
@SpringBootApplication public class IceibankApplication {
 public static void main(String[] args){int id=ler("AGENCIA_ID",0),offset=ler("OFFSET",0);if(id<0||id>2)throw new IllegalArgumentException("AGENCIA_ID deve ser 0, 1 ou 2.");List<String>a=new ArrayList<>();if(args!=null)for(String x:args)if(!x.startsWith("--agencia.id=")&&!x.startsWith("--agencia.offset=")&&!x.startsWith("--server.port="))a.add(x);a.add("--agencia.id="+id);a.add("--agencia.offset="+offset);a.add("--server.port="+(4000+offset+id));SpringApplication.run(IceibankApplication.class,a.toArray(String[]::new));System.out.printf("[ICEIBank] Agência %d iniciada em http://localhost:%d%n",id,4000+offset+id);}
 private static int ler(String n,int p){String v=System.getenv(n);return v==null||v.isBlank()?p:Integer.parseInt(v.trim());}
}
