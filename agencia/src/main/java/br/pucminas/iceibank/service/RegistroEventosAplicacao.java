package br.pucminas.iceibank.service;
import br.pucminas.iceibank.config.AgenciaConfig; import org.springframework.stereotype.Service;
import java.io.IOException; import java.nio.file.*; import java.util.Map;
@Service public class RegistroEventosAplicacao {
 private final RegistroEventos registroEventos; private final Path pastaDados; private final Path caminhoArquivo;
 public RegistroEventosAplicacao(RegistroEventos r, AgenciaConfig c){registroEventos=r;pastaDados=Paths.get("data").toAbsolutePath().normalize();caminhoArquivo=pastaDados.resolve("eventos-agencia-"+c.getIdAgencia()+".jsonl");}
 public Map<String,Object> registrar(String tipo,int[] ts,Map<String,Object> detalhes){try{return registroEventos.registrar(tipo,ts,detalhes);}catch(IOException e){throw new IllegalStateException("Falha ao registrar evento em "+caminhoArquivo,e);}}
 public Path getPastaDados(){return pastaDados;} public Path getCaminhoArquivo(){return caminhoArquivo;}
}
